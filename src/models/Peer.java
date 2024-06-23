package models;

import models.file.FileChunk;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public final class Peer {
    private PeerInfo peerInfo;
    private Map<FileChunk, String> fileChunkToPath = new HashMap<>();
    private DatagramSocket peerHandlerSocket;
    private byte[] socketBuffer;


    public Peer(String peerName, String address) throws IOException, URISyntaxException {
        setPeerInfo(peerName, address);
        int port = Integer.parseInt(this.peerInfo.address.split(":")[1]);
        peerHandlerSocket = new DatagramSocket(port);

        // Starting cli thread
        new Thread(this::cli).start();


        // Starting command handler
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        String[] receivedCmd = listenOnUDPSocketForCommand(); // Wait until get next cmd
                        processCommand(receivedCmd);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();


        Scanner ent = new Scanner(System.in);
        System.out.println("Gimme the root tracker address ");
        String rootTrackerAddress = ent.nextLine();

        // Introducing it-self to root tracker
        // addSeeder <seederName> <address>
        sendStringUsingUDPPacket(rootTrackerAddress, "addSeeder " + this.peerInfo.getPeerName() + " " + this.peerInfo.getAddress());



        System.out.println(peerName + " peer successfully created on port " + port + " as heart-bit status checking (UDP) and " + (port+1) + " as file sharing (TCP)");
    }

    private String[] listenOnUDPSocketForCommand() throws IOException {
        // Wait until first byte receive
        System.out.println("Listening on UDP port for incoming commands ...");
        socketBuffer = new byte[256];

        DatagramPacket packet = new DatagramPacket(socketBuffer, socketBuffer.length);
        peerHandlerSocket.receive(packet);


        String received = new String(packet.getData(), 0, packet.getLength());

        System.out.println("New msg received: "+received+" from "+packet.getAddress());

        String[] cmd = {received, String.valueOf(packet.getAddress())+":"+String.valueOf(packet.getPort())};
        return cmd;
    }
    private void share(String filePath, String trackerAddress) throws IOException {
        // split file into 2MB chunks
        // index all chunks in tracker

        File relatedFile = new File(filePath);
        for (int offset = 1 ; offset <= Math.floor(relatedFile.length()/(2*10^6)) ; offset++) {
            // share <file name> <seeder address> <cid>
            String cmdPayload = "share " + relatedFile.getName() + " " + this.peerInfo.address + " " + offset;
            sendStringUsingUDPPacket(trackerAddress, cmdPayload);
        }

    }

    private void get(String fileChunk, String trackerAddress, String myAddress){
        // TODO
        // tell the tracker I want the file chunk
        // add a listener to download that file chunk
    }

    private void sendStringUsingUDPPacket(String address, String data) throws IOException {
        peerHandlerSocket = new DatagramSocket();
        byte[] socketBuffer = data.getBytes();
        DatagramPacket payload = new DatagramPacket(socketBuffer, socketBuffer.length, InetAddress.getLocalHost(), Integer.parseInt(address.split(":")[1]));
        peerHandlerSocket.send(payload);
        System.out.println(data + " was sent to "+InetAddress.getLocalHost()+address.split(":")[1]);
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

    public void cli() {
        Scanner cin = new Scanner(System.in);
        while(true){
            String entry = cin.nextLine();
            String[] separatedCmd = entry.split(" ");
            switch(separatedCmd[0]) {
                case "custommsg":
                    // custommsg <target address> <data>
                    try {
                        sendStringUsingUDPPacket(separatedCmd[1], separatedCmd[2]);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "share":
                    // share <file name> <target tracker address> <seeder address>
                    try {
                        share(separatedCmd[1], separatedCmd[2]);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "get":
                    break;
                default:
                    System.out.println("command not found");
            }
        }
    }


    public void sendKeepAliveResponse(String address) throws IOException {
        byte[] socketBuffer = "yes".getBytes();
        DatagramPacket packet = new DatagramPacket(socketBuffer, socketBuffer.length, InetAddress.getLocalHost(), Integer.parseInt(address.split(":")[1]));
        peerHandlerSocket.send(packet);

        System.out.println("Keep-Alive resp was sent !");
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
