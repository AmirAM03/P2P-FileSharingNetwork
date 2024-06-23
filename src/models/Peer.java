package models;

import models.file.FileChunk;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public final class Peer {
    private PeerInfo peerInfo;
    private Map<FileChunk, String> fileChunkToPath = new HashMap<>();
    private DatagramSocket peerHandlerSocket;
    private byte[] socketBuffer;


    public Peer(String peerName, String address) throws IOException, URISyntaxException {
        setPeerInfo(peerName, address);
        peerHandlerSocket = new DatagramSocket(Integer.parseInt(this.peerInfo.address.split(":")[1]));

        while (true) {
            String[] receivedCmd = listenOnSocketForCommand(); // Wait until get next cmd
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        processCommand(receivedCmd);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }
    }

    private String[] listenOnSocketForCommand() throws IOException {
        // Wait until first byte receive
        socketBuffer = new byte[256];

        DatagramPacket packet = new DatagramPacket(socketBuffer, socketBuffer.length);
        peerHandlerSocket.receive(packet);

        String received = new String(packet.getData(), 0, packet.getLength());

        String[] cmd = {received, String.valueOf(packet.getAddress())+":"+String.valueOf(packet.getPort())};
        return cmd;
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

    private void processCommand(String[] entryCommand) throws IOException {
        // Separator char is %
        if (entryCommand[0].equals("alive-checking")) {
            // Second segment will be the address of checker tracker
            sendKeepAliveResponse(entryCommand[1]);
        } else {
            sendKeepAliveResponse(entryCommand[1]);
        }
    }

    public void cli(){
        // CLI

    }


    public void sendKeepAliveResponse(String address) throws IOException {
        byte[] socketBuffer = "yes".getBytes();
        DatagramPacket packet = new DatagramPacket(socketBuffer, socketBuffer.length, InetAddress.getLocalHost(), Integer.parseInt(address.split(":")[1]));
        peerHandlerSocket.send(packet);

        System.out.println("Sent !");
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
