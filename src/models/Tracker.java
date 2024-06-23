package models;

import models.file.File;
import models.file.FileChunk;
import models.file.FileName;

import java.io.IOException;
import java.net.*;
import java.util.*;

public final class Tracker{
    private String address;
    private final List<PeerRequestLog> peerRequestLogs = new ArrayList<>();
    private final Map<FileName, File> fileNameToFile = new HashMap<>();
    private final Map<FileChunk, List<String>> fileChunkToSeedersName = new HashMap<>();
    private final Map<String, PeerInfo> peerNameToPeerInfo = new HashMap<>();
    private DatagramSocket peerHandlerSocket;
    private byte[] socketBuffer;

    public Tracker(String address) {
        this.address = address;

        new Thread(this::cli).start();
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
        if(!isPeerInfoExist(seederName)){
            throw new IllegalArgumentException("seeder with seederName does not exist");
        }
        FileChunk fileChunk = getFileChunk(fileName, cid);
        addSeederToFileChunk(fileChunk, seederName);
        return "successfully added seeder["+seederName+"] to chunk["+fileChunk+"]";
    }

    public String addNewSeederName(String seederName, String address) {
        if(isPeerInfoExist(seederName)){
            throw new IllegalArgumentException("seeder with seederName already exists");
        }
        PeerInfo seederInfo = new PeerInfo(seederName, address);
        addOrUpdatePeerInfo(seederInfo);
        return "successfully added a new seederName";
    }

    public String sendSeedersForFileChunk(FileName fileName, int cid) {
        FileChunk fileChunk = getFileChunk(fileName, cid);
        return toStringSeedersForFileChunk(fileChunk);
    }

    private void processCommand(String cmd) {
        String[] separatedCmd = cmd.split(" ");

        String response = "";

        switch(separatedCmd[0]){
            case "addSeeder":
                // addSeeder <seederName> <address>
                String seederName = separatedCmd[1];
                String address = separatedCmd[2];
                response = addNewSeederName(seederName, address);
                // TODO start keep-alive for socket
                break;
            case "share":
                // share <seederName> <fileName> <cid>
                String seederName2 = separatedCmd[1];
                FileName fileName = new FileName(separatedCmd[2]);
                int cid = Integer.parseInt(separatedCmd[3]);
                response = addASeederToAChunk(seederName2, fileName, cid);
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
                throw new IllegalArgumentException("command not found");
        }
        sendResponse(response);
        // TODO send response
    }

    public void cli(){
        Scanner cin = new Scanner(System.in);
        while(true){
            String s = cin.next();
            switch(s) {
                case "reportLogs":
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

    public void sendResponse(String response){
        System.out.println(response);
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

    private void sendPacketToAliveSocket(InetAddress ip, int port) throws IOException {
        byte[] socketBuffer = new byte[256];
        peerHandlerSocket = new DatagramSocket(port);

        while (true) {
            DatagramPacket packet = new DatagramPacket(socketBuffer, socketBuffer.length);
            peerHandlerSocket.receive(packet);

            packet = new DatagramPacket(socketBuffer, socketBuffer.length, ip, port);
            String received = new String(packet.getData(), 0, packet.getLength());

            if (received.equals("Terminate@the@Socket")) {
                peerHandlerSocket.close();
                return;
            }

            peerHandlerSocket.send(packet);
        }
    }

    public String listenOnSocketForCommand() throws IOException {
        // Wait until first byte receive
        socketBuffer = new byte[256];
        peerHandlerSocket = new DatagramSocket(Integer.parseInt(this.address.split(":")[1]));

        DatagramPacket packet = new DatagramPacket(socketBuffer, socketBuffer.length);
        peerHandlerSocket.receive(packet);

        InetAddress address = packet.getAddress();
        int port = packet.getPort();
        packet = new DatagramPacket(socketBuffer, socketBuffer.length, address, port);
        String received = new String(packet.getData(), 0, packet.getLength());

        peerHandlerSocket.close();

//        System.out.println((int)received.charAt(5));

        return received.replace( (char) (0) +"", "");
    }

    public boolean isPeerAlive(String address) throws IOException {
        peerHandlerSocket = new DatagramSocket();
        socketBuffer = "alive-checking".getBytes();
        DatagramPacket packet = new DatagramPacket(socketBuffer, socketBuffer.length, InetAddress.getLocalHost(), Integer.parseInt(address.split(":")[1]));
        peerHandlerSocket.send(packet);

        String response = listenOnSocketForCommand();

        peerHandlerSocket.disconnect();

        if (response.equals("yes")) return true;

        return false;
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
        if(!isPeerInfoExist(seederName)){
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
        if(!isPeerInfoExist(peerName)){
            throw new IllegalArgumentException("peerInfo with peerName does not exist");
        }
        return peerNameToPeerInfo.get(peerName);
    }

    public boolean isPeerInfoExist(String peerName){
        return peerNameToPeerInfo.containsKey(peerName);
    }


    public List<PeerRequestLog> getPeerRequestLogs() {
        return peerRequestLogs;
    }
}
