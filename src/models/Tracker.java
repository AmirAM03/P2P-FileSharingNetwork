package models;

import models.file.File;
import models.file.FileChunk;
import models.file.FileName;

import java.io.IOException;
import java.net.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public final class Tracker{
    private String address;
    private final Map<FileName, File> fileNameToFile = new HashMap<>();
    private final Map<FileChunk, List<String>> fileChunkToSeedersName = new HashMap<>();
    private final Map<String, PeerInfo> peerNameToPeerInfo = new HashMap<>();

    private DatagramSocket peerHandlerSocket;

    public Tracker(String address) throws URISyntaxException {
        this.address = address;
    }

    public void start() throws IOException {
        System.out.println(listenOnSocketForCommand());
    }


    public void addNewFile(FileName fileName, Integer size) {
        if(isFileExists(fileName)){
            throw new IllegalArgumentException("file with fileName already exists");
        }
        File newFile = new File(fileName, size);
        addOrUpdateFile(newFile);
        addAllFileChunks(newFile);
    }

    public void addASeederToAChunk(String seederName, FileName fileName, int cid) {
        if(!isPeerInfoExist(seederName)){
            throw new IllegalArgumentException("seeder with seederName does not exist");
        }
        File file = getFileByFileName(fileName);
        FileChunk fileChunk = file.getChunkByCid(cid);
        addSeederToFileChunk(fileChunk, seederName);
    }

    public void addNewSeederName(String seederName, String address) throws URISyntaxException {
        if(isPeerInfoExist(seederName)){
            throw new IllegalArgumentException("seeder with seederName already exists");
        }
        PeerInfo seederInfo = new PeerInfo(seederName, address);
        addOrUpdatePeerInfo(seederInfo);
    }

    private void processCommand(String cmd) throws Exception {
        String[] separatedCmd = cmd.split(" ");

        switch(separatedCmd[0]){
            case "addSeeder" -> {
                // addSeeder <seederName> <address>
                String seederName = separatedCmd[1];
                String address = separatedCmd[2];
                // TODO start keep-alive for socket
            }
            case "share" -> {
                // share <seederName> <fileName> <cid>
                String seederName = separatedCmd[1];
                FileName fileName = new FileName(separatedCmd[2]);
                int cid = Integer.parseInt(separatedCmd[3]);
                addASeederToAChunk(seederName, fileName, cid);
            }
            case "addNewFile" -> {
                // addNewFile <fileName> <size>
                FileName fileName = new FileName(separatedCmd[1]);
                int size = Integer.parseInt(separatedCmd[2]);
                addNewFile(fileName, size);
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
        if(!isFileChunkExist(fileChunk)){
            throw new IllegalArgumentException("fileChunk does not exist");
        }
        getSeedersForFileChunk(fileChunk).add(seederName);
        return this;
    }

    public List<String> getSeedersForFileChunk(FileChunk fileChunk){
        if(!isFileChunkExist(fileChunk)){
            throw new IllegalArgumentException("fileChunk does not exist");
        }
        return this.fileChunkToSeedersName.get(fileChunk);
    }

    public Tracker addNewFileChunk(FileChunk fileChunk){
        if(isFileChunkExist(fileChunk)){
            throw new IllegalArgumentException("fileChunk already exists");
        }
        fileChunkToSeedersName.put(fileChunk, new LinkedList<>());
        return this;
    }

    public Tracker addAllFileChunks(File file){
        for(FileChunk fileChunk : file.getFileChunks()){
            addNewFileChunk(fileChunk);
        }
        return this;
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
}
