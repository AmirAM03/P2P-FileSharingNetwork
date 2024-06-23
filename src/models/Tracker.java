package models;

import models.file.File;
import models.file.FileChunk;
import models.file.FileName;

import java.io.IOException;
import java.net.*;
import java.util.*;

public final class Tracker{
    private String address;
    private final Map<FileName, File> fileNameToFile = new HashMap<>();
    private final Map<FileChunk, List<String>> fileChunkToSeedersName = new HashMap<>();
    private final Map<String, PeerInfo> peerNameToPeerInfo = new HashMap<>();
    private final Map<String, DatagramSocket> peerNameToSocket = new HashMap<>();


    private DatagramSocket peerHandlerSocket;

    public Tracker(String address) throws URISyntaxException {
        this.address = address;
    }

    public void start() throws IOException {
        System.out.println(listenOnSocketForCommand());
    }

    private void processCommand(String cmd) throws Exception {
        String[] separatedCmd = cmd.split(" ");

        switch(separatedCmd[0]){
            case "addSeeder" -> {
                // addSeeder <seederName> <address>
                String seederName = separatedCmd[1];
                String address = separatedCmd[2];
                if(isPeerInfoExist(seederName)){
                    throw new IllegalArgumentException("seeder with seederName already exists");
                }
                Peer seeder = new Peer(seederName, address);
                DatagramSocket datagramSocket = new DatagramSocket(Integer.parseInt(address.split(":")[1]));
                addOrUpdatePeerInfo(seeder.getPeerInfo());
                addOrUpdatePeerSocket(seederName, datagramSocket);
                // TODO start keep-alive for socket, ask ahmadi?
            }
            case "share" -> {
                // share <fileName> <cid> <seederName>
                FileName fileName = new FileName(separatedCmd[1]);
                int cid = Integer.parseInt(separatedCmd[2]);
                String seederName = separatedCmd[3];

                File file = getFileByFileName(fileName);
                FileChunk fileChunk = file.getChunkByCid(cid);
                if(!isPeerInfoExist(seederName)){
                    throw new IllegalArgumentException("seeder with seederName does not exist");
                }
            }
            case "reportLogs" -> {

            }
            case "reportAvailableFileChunks" -> {

            }
        }
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

    private String listenOnSocketForCommand() throws IOException {
        // Wait until first byte receive
        byte[] buf = new byte[256];
        peerHandlerSocket = new DatagramSocket(Integer.parseInt(this.address.split(":")[1]));

        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        peerHandlerSocket.receive(packet);

        InetAddress address = packet.getAddress();
        int port = packet.getPort();
        packet = new DatagramPacket(buf, buf.length, address, port);
        String received = new String(packet.getData(), 0, packet.getLength());
        return received;
    }

    public void isPeerAlive(String peerName){

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

    public Tracker addSeederForFileChunk(FileChunk fileChunk, String seederName) {
        if(!isPeerInfoExist(seederName) || !isPeerSocketExist(seederName)){
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

    public PeerInfo getPeerInfo(String peerName){
        if(!isPeerInfoExist(peerName)){
            throw new IllegalArgumentException("peerInfo with peerName does not exist");
        }
        return peerNameToPeerInfo.get(peerName);
    }

    public boolean isPeerInfoExist(String peerName){
        return peerNameToPeerInfo.containsKey(peerName);
    }


    public Map<String, DatagramSocket> getPeerNameToSocket() {
        return peerNameToSocket;
    }

    public Tracker addOrUpdatePeerSocket(String peerName, DatagramSocket socket){
        this.peerNameToSocket.put(peerName, socket);
        return this;
    }

    public DatagramSocket getPeerSocket(String peerName){
        if(!isPeerSocketExist(peerName)){
            throw new IllegalArgumentException("socket with peerName does not exist");
        }
        return peerNameToSocket.get(peerName);
    }

    public boolean isPeerSocketExist(String peerName){
        return peerNameToPeerInfo.containsKey(peerName);
    }

}
