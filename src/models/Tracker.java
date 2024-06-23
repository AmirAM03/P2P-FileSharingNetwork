package models;

import models.file.File;
import models.file.FileChunk;
import models.file.FileName;

import java.net.DatagramSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public final class Tracker{
    private URI uri;
    private final Map<FileName, File> fileNameToFile = new HashMap<>();
    private final Map<FileChunk, List<String>> fileChunkToSeedersName = new HashMap<>();
    private final Map<String, PeerInfo> peerNameToPeerInfo = new HashMap<>();
    private final Map<String, DatagramSocket> peerNameToSocket = new HashMap<>();


    public Tracker(String address) throws URISyntaxException {
        setUri(address);
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


    // getters & setters

    public URI getUri() {
        return this.uri;
    }

    private Tracker setUri(String address) throws URISyntaxException {
        this.uri = new URI(address);
        return this;
    }


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
