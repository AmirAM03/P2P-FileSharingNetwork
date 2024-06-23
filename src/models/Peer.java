package models;

import models.file.FileChunk;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public final class Peer {
    private PeerInfo peerInfo;
    private Map<FileChunk, String> fileChunkToPath = new HashMap<>();
    private DatagramSocket peerHandlerSocket;
    private byte[] socketBuffer;


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

    private void processCommand(String entryCommand){
        // Separator char is %
        if (entryCommand.split("%")[0].equals("alive-checking")) {

        } else {

        }
    }

    public void cli(){
        // CLI

    }


    private void sendKeepAliveResponse(String address) throws SocketException {
        byte[] socketBuffer = new byte[256];
        peerHandlerSocket = new DatagramSocket(Integer.parseInt(address.split(":")[1]));

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
