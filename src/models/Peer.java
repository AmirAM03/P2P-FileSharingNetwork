package models;

import models.file.FileChunk;

import java.net.SocketException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public final class Peer {
    private PeerInfo peerInfo;
    private Map<FileChunk, String> fileChunkToPath = new HashMap<>();


    public Peer(String peerName, String address) throws URISyntaxException {
        setPeerInfo(peerName, address);
    }


    private void share(String fileChunk, String trackerAddress, String myAddress){
        // TODO
        // tell the tracker I have the file chunk
        // add a listener for uploading that file chunk
    }

    private void get(String fileChunk, String trackerAddress, String myAddress){
        // TODO
        // tell the tracker I want the file chunk
        // add a listener to download that file chunk
    }

    public void cli(){
        // CLI
    }


    // getters & setters

    public PeerInfo getPeerInfo() {
        return this.peerInfo;
    }

    public Peer setPeerInfo(String peerName, String address) throws URISyntaxException {
        this.peerInfo = new PeerInfo(peerName, address);
        return this;
    }


    public Map<FileChunk, String> getFileChunkToPath() {
        return this.fileChunkToPath;
    }

    public Peer setFileChunkToPath(Map<FileChunk, String> fileChunkToPath) {
        this.fileChunkToPath = fileChunkToPath;
        return this;
    }
}
