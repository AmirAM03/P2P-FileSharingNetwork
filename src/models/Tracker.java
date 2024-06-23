package models;

import models.file.File;
import models.file.FileChunk;
import models.file.FileName;

import java.io.IOException;
import java.net.*;
import java.util.*;

public final class Tracker{
    private static ArrayList<DatagramPacket> receivingQueue;
    private String address;
    private final List<PeerRequestLog> peerRequestLogs = new ArrayList<>();
    private final Map<FileName, File> fileNameToFile = new HashMap<>();
    private final Map<FileChunk, List<String>> fileChunkToSeedersName = new HashMap<>();
    private final Map<String, PeerInfo> peerNameToPeerInfo = new HashMap<>();
    private DatagramSocket peerHandlerSocket;
    private byte[] socketBuffer;

    public Tracker(String address) throws IOException {
        this.address = address;

        new Thread(this::cli).start();

        peerHandlerSocket = new DatagramSocket(Integer.parseInt(address.split(":")[1]));
        receivingQueue = new ArrayList<>();
        new Thread(this::runUDPListenerStacker).start();

        // Start receiving queue resolver
        new Thread(this::runCommandStackResolver).start();
    }



    private void runUDPListenerStacker() {
        while (true) {
            try {
                DatagramPacket next = listenOnSocketAndStackReceiving();
                System.out.println("New command received and pushed to processing stack ... " + udpPacketReceivedDataCleaner(next));
                addOrRemovePacketFromProcessStack(next, false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void runCommandStackResolver(){
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            try {
                for (DatagramPacket tmp : receivingQueue) {
//                System.out.println("resolving next received command");
                    if (!processCommand(udpPacketReceivedDataCleaner(tmp)).equals("Unknown Command")) {
                        addOrRemovePacketFromProcessStack(tmp, true);
                        break;
                    }
                }
            } catch (ConcurrentModificationException e) {
                System.out.println("Lock of Death Occurred !");
            }

        }
    }


    private static String udpPacketReceivedDataCleaner(DatagramPacket packet){
        return new String(packet.getData(), 0, packet.getLength()).replace( (char) (0) +"", "");
    }

    private DatagramPacket listenOnSocketAndStackReceiving() throws IOException {
        // Wait until first byte receive
        System.out.println("Waiting to receive another command ... " + Thread.currentThread().getName());
        socketBuffer = new byte[256];

        DatagramPacket packet = new DatagramPacket(socketBuffer, socketBuffer.length);
        peerHandlerSocket.receive(packet);

//        System.out.println("receeeivd"+new String(packet.getData(), 0, packet.getLength()));

//        System.out.println((int)received.charAt(5));

//        return received.replace( (char) (0) +"", "");
        return packet;
    }

    public boolean isPeerAlive(String address, int responseDelayThreshold) throws IOException, InterruptedException {
//        DatagramSocket sender = new DatagramSocket();
        socketBuffer = "alive-checking".getBytes();
        DatagramPacket packet = new DatagramPacket(socketBuffer, socketBuffer.length, InetAddress.getLocalHost(), Integer.parseInt(address.split(":")[1]));
        peerHandlerSocket.send(packet);
//        sender.disconnect();
//        sender.close();


        Thread.sleep(responseDelayThreshold);
        for (DatagramPacket tmp:receivingQueue) {
//            System.out.println(udpPacketReceivedDataCleaner(tmp) +" "+ tmp.getAddress() +" "+ tmp.getPort());
            if (udpPacketReceivedDataCleaner(tmp).equals("yes") && tmp.getPort()==packet.getPort() ) {
                addOrRemovePacketFromProcessStack(tmp, true);
                return true;
            }
        }
        return false;
    }



    public FileChunk getFileChunk(FileName fileName, int cid){
        File file = getFileByFileName(fileName);
        return file.getChunkByCid(cid);
    }

    public void addNewChunk(FileChunk fileChunk){
        if(getFileChunkToSeedersName().containsKey(fileChunk)){
            throw new IllegalArgumentException("fileChunk already exists");
        }
        getFileChunkToSeedersName().put(fileChunk, new ArrayList<>());
    }

    public void addAllFileChunks(File file){
        for(FileChunk fileChunk : file.getFileChunks()){
            addNewChunk(fileChunk);
        }
    }

    public String addNewFile(FileName fileName, Integer size) {
        if(isFileExists(fileName)){
            throw new IllegalArgumentException("file with fileName already exists");
        }
        File newFile = new File(fileName, size);
        addOrUpdateFile(newFile);
        addAllFileChunks(newFile);
        return "successfully added newFile["+fileName+"] and its chunks";
    }

    public String addASeederToAChunk(String seederName, FileName fileName, int cid) {
        if(!isPeerExistsWithName(seederName)){
            throw new IllegalArgumentException("seeder with seederName does not exist");
        }
        FileChunk fileChunk = getFileChunk(fileName, cid);
        addSeederToFileChunk(fileChunk, seederName);
        return "successfully added seeder["+seederName+"] to chunk["+fileChunk+"]";
    }

    private void sendPacketToAliveSocket(String address ,String data) throws IOException {
        byte[] socketBuffer = data.getBytes();
        DatagramPacket packet = new DatagramPacket(socketBuffer, socketBuffer.length, InetAddress.getLocalHost(), Integer.parseInt(address.split(":")[1]));
        peerHandlerSocket.send(packet);
    }

    public String addNewSeeder(String seederName, String address) {
        if(isPeerExistsWithName(seederName)){
            throw new IllegalArgumentException("seeder with seederName already exists");
        }
        PeerInfo seederInfo = new PeerInfo(seederName, address);
        addOrUpdatePeerInfo(seederInfo);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        Thread.sleep(10000);
                        if (isPeerAlive(address, 3000)) {
                            System.out.println(seederName + " peer is still live :)");
                        } else {
                            System.out.println(seederName + " peer is not responding :_( I'll remove it from network ...");
                            removePeerFromThisTrackerNetworkByPeerName(seederName);
                            return;
                        }
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
        return "successfully added a new seederName";
    }

    public String sendSeedersForFileChunk(FileName fileName, int cid) {
        FileChunk fileChunk = getFileChunk(fileName, cid);
        return toStringSeedersForFileChunk(fileChunk);
    }

    private String processCommand(String cmd) {
        String[] separatedCmd = cmd.split(" ");

        String response = "";

        switch(separatedCmd[0]){
            case "addSeeder":
                // addSeeder <seederName> <address>
                String seederAddress = separatedCmd[2];
                response = addNewSeeder(separatedCmd[1], seederAddress);
                break;
            case "share":
                // share <file name> <seeder address> <cid>
                FileName fileName = new FileName(separatedCmd[1]);
                int cid = Integer.parseInt(separatedCmd[3]);
                response = addASeederToAChunk(separatedCmd[1], fileName, cid);
                break;
            case "addNewFile":
                // addNewFile <fileName> <size>
                FileName fileName2 = new FileName(separatedCmd[1]);
                int size = Integer.parseInt(separatedCmd[2]);
                response = addNewFile(fileName2, size);
                break;
            case "getChunkSeeders":
                // getChunkSeeders <fileName> <cid>
                FileName fileName3 = new FileName(separatedCmd[1]);
                int cid2 = Integer.parseInt(separatedCmd[2]);
                response = sendSeedersForFileChunk(fileName3, cid2);
                break;
            case "reportLogs":
                for (PeerRequestLog log: getPeerRequestLogs()) {
                    System.out.println(log);
                }
                break;
            case "reportAvailableFileChunks" :
                for(FileChunk fileChunk : fileChunkToSeedersName.keySet()){
                    System.out.println(toStringSeedersForFileChunk(fileChunk));
                }
                break;
            default:
                response = "Unknown Command";
                break;
        }
//        System.out.println(response);
        return response;
        // TODO send response
    }

    public void cli(){
        Scanner cin = new Scanner(System.in);
        while(true){
            String s = cin.next();
            switch(s) {
                case "processStack":
                    for (DatagramPacket tmp : receivingQueue) System.out.println(udpPacketReceivedDataCleaner(tmp));
                    break;
                case "reportLogs":
                    System.out.println("Here the logs :");
                    for (PeerRequestLog log : getPeerRequestLogs()) {
                        System.out.println(log);
                    }
                    break;
                case "reportAvailableFileChunks":
                    for (FileChunk fileChunk : fileChunkToSeedersName.keySet()) {
                        System.out.println(toStringSeedersForFileChunk(fileChunk));
                    }
                    break;
                default:
                    System.out.println("command not found");
            }
        }
    }

    public String toStringSeedersForFileChunk(FileChunk fileChunk){
        if(!isFileChunkExist(fileChunk)){
            throw new IllegalArgumentException("fileChunk does not exist");
        }
        String ret = fileChunk+":";
        ret += getSeedersForFileChunk(fileChunk);
        return ret;
    }

    public Tracker addLog(PeerRequestLog log){
        peerRequestLogs.add(log);
        return this;
    }

    public Tracker addLog(PeerInfo peerInfo, String requestData, boolean isSuccess){
        peerRequestLogs.add(new PeerRequestLog(peerInfo, requestData, isSuccess));
        return this;
    }


    public DatagramSocket getPeerHandlerSocket() {
        return peerHandlerSocket;
    }


    // getters & setters
    public Map<FileName, File> getFileNameToFile() {
        return this.fileNameToFile;
    }

    public File getFileByFileName(FileName fileName){
        if(!isFileExists(fileName)){
            throw new IllegalArgumentException("file with fileName does not exist");
        }
        return this.fileNameToFile.get(fileName);
    }

    public Tracker addOrUpdateFile(File file){
        this.fileNameToFile.put(file.getFileName(), file);
        return this;
    }

    public void addOrUpdateNewFile(FileName fileName, Integer size) {
        if(isFileExists(fileName)){
            return;
        }
        File newFile = new File(fileName, size);
        addOrUpdateFile(newFile);
    }

    public boolean isFileExists(FileName fileName){
        return this.fileNameToFile.containsKey(fileName);
    }


    public Map<FileChunk, List<String>> getFileChunkToSeedersName() {
        return this.fileChunkToSeedersName;
    }

    public Tracker addSeederToFileChunk(FileChunk fileChunk, String seederName) {
        if(!isPeerExistsWithName(seederName)){
            throw new IllegalArgumentException("seeder with seederName does not exist");
        }
        if(isFileChunkExist(fileChunk)){
            this.fileChunkToSeedersName.get(fileChunk).add(seederName);
            return this;
        }
        this.fileChunkToSeedersName.put(fileChunk, Collections.singletonList(seederName));
        return this;
    }

    public List<String> getSeedersForFileChunk(FileChunk fileChunk){
        if(!isFileChunkExist(fileChunk)){
            throw new IllegalArgumentException("fileChunk does not exist");
        }
        return this.fileChunkToSeedersName.get(fileChunk);
    }

    public boolean isFileChunkExist(FileChunk fileChunk){
        return this.fileChunkToSeedersName.containsKey(fileChunk);
    }


    public Map<String, PeerInfo> getPeerNameToPeerInfo() {
        return this.peerNameToPeerInfo;
    }

    public Tracker addOrUpdatePeerInfo(PeerInfo peerInfo){
        this.peerNameToPeerInfo.put(peerInfo.getPeerName(), peerInfo);
        return this;
    }

    public PeerInfo getPeerInfoByPeerName(String peerName){
        if(!isPeerExistsWithName(peerName)){
            throw new IllegalArgumentException("peerInfo with peerName does not exist");
        }
        return peerNameToPeerInfo.get(peerName);
    }

    public boolean isPeerExistsWithName(String peerName){
        return peerNameToPeerInfo.containsKey(peerName);
    }


    public List<PeerRequestLog> getPeerRequestLogs() {
        return peerRequestLogs;
    }


    private void removePeerFromThisTrackerNetworkByPeerName(String peerName){
        if (isPeerExistsWithName(peerName)) {
            PeerInfo inf = peerNameToPeerInfo.get(peerName);
            peerNameToPeerInfo.remove(peerName);
            for (Map.Entry<FileChunk, List<String>> entry : fileChunkToSeedersName.entrySet()) {
                if (entry.getValue().contains(inf.address)) {
                    entry.getValue().remove(inf.address);
                }
            }
        }
    }


    private static synchronized void addOrRemovePacketFromProcessStack(DatagramPacket packet, boolean wantToRemove){
        if (wantToRemove) {
            receivingQueue.remove(packet);
        } else {
            receivingQueue.add(packet);
        }
    }
}