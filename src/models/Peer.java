package models;

import models.file.FileChunk;
import models.file.FileName;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public final class Peer {
    private PeerInfo peerInfo;
    private Map<FileChunk, String> fileChunkToPath = new HashMap<>();
    private DatagramSocket peerHandlerSocket;
    private byte[] socketBuffer;
    private PrintWriter outPipeline;
    private BufferedReader inPipeline;



    public Peer(String peerName, String address) throws IOException, URISyntaxException {
        setPeerInfo(peerName, address);
        String[] parts = address.split(":");
        String peerAddress = parts[0];
        int port = Integer.parseInt(this.peerInfo.address.split(":")[1]);
        peerHandlerSocket = new DatagramSocket(port);


        Scanner ent = new Scanner(System.in);
        System.out.println("Gimme the root tracker address ");
        String rootTrackerAddress = ent.nextLine();

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



        // Introducing it-self to root tracker
        // addSeeder <seederName> <address>
        sendStringUsingUDPPacket(rootTrackerAddress, "addSeeder " + this.peerInfo.getPeerName() + " " + this.peerInfo.getAddress());



        System.out.println(peerName + " peer successfully created on port " + port + " as heart-bit status checking (UDP) and " + (port+1) + " as file sharing (TCP)");




        // TCP Setup for file transfer between peers
        ServerSocket serverTCPSocket = new ServerSocket(port+1);
        Socket clientSocket = serverTCPSocket.accept();
        outPipeline = new PrintWriter(clientSocket.getOutputStream(), true);
        inPipeline = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        // TCP command handler
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (inPipeline.ready()) {
                            String message = inPipeline.readLine();
                            if (message.startsWith("get"))
                                sendChuck(message, outPipeline);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }
    private void sendChuck(String message, PrintWriter outPipeline) throws IOException {
        String[] parts = message.split(" ");
        String fileName = parts[1];
        int chunkId = Integer.parseInt(parts[3]);
        String[] addressParts = parts[2].split(":");
        String peerAddress = addressParts[0];
        int peerPort = Integer.parseInt(addressParts[1]);
        byte[] buffer = new byte[2 * 1024 * 1024]; // 2MB
        RandomAccessFile raf = new RandomAccessFile(fileName, "r");
        raf.seek(chunkId * buffer.length);
        int bytesRead = raf.read(buffer, 0, buffer.length);

        // sending
        outPipeline.write(bytesRead);
    }
//    public void sendChunk(FileName filePath, int chunkId, String peerAddress, int peerPort) {
//        DataOutputStream out = null;
//        DataInputStream in = null;
//        RandomAccessFile raf = null;
//
//        try {
//            out = new DataOutputStream(socket.getOutputStream());
//            in = new DataInputStream(socket.getInputStream());
//
//            models.file.File file = new models.file.File(filePath, 10);
//            FileChunk chunk = file.getChunkByCid(chunkId);
//
//
//            // reading
//            raf = new RandomAccessFile(filePath.getFileName(), "r");
//            byte[] buffer = new byte[2 * 1024 * 1024]; // 2MB
//            raf.seek(chunkId * buffer.length);
//            int bytesRead = raf.read(buffer, 0, buffer.length);
//
//            // sending
//            out.writeInt(bytesRead);
//            out.write(buffer, 0, bytesRead);
//
//            // acknowledgment
//            int ack = in.readInt();
//            if (ack == 1) {
//                System.out.println("Chunk " + chunkId + " sent successfully.");
//            } else {
//                System.out.println("Failed to send chunk " + chunkId);
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            // closing
//            try {
//                if (raf != null) raf.close();
//                if (out != null) out.close();
//                if (in != null) in.close();
//                if (socket != null) socket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    public void receiveChunk(int tcpPort) {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(tcpPort);
            System.out.println("Server started, waiting for connections...");

            while (true) {
                try (Socket socket = serverSocket.accept();
                     DataInputStream in = new DataInputStream(socket.getInputStream());
                     DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

                    // receive
                    int chunkSize = in.readInt();
                    byte[] buffer = new byte[chunkSize];

                    // receive
                    in.readFully(buffer, 0, chunkSize);

                    // write
                    try (FileOutputStream fos = new FileOutputStream("received_chunk_" + System.currentTimeMillis())) {
                        fos.write(buffer);
                    }

                    // acknowledgment
                    out.writeInt(1);

                    System.out.println("Chunk received and saved.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // close
            try {
                if (serverSocket != null) serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        for (int offset = 0 ; offset*(2*(1e6)) <= Math.ceil(relatedFile.length()) ; offset++) {
            // share <file name> <seeder name> <cid>
            String cmdPayload = "share " + relatedFile.getName() + " " + this.peerInfo.getPeerName() + " " + offset;
            sendStringUsingUDPPacket(trackerAddress, cmdPayload);
        }

    }

    private void get(String fileChunk, String trackerAddress, String myAddress){
        // TODO
        // tell the tracker I want the file chunk
        // add a listener to download that file chunk
    }

    private void sendStringUsingUDPPacket(String address, String data) throws IOException {
//        peerHandlerSocket = new DatagramSocket();
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
                    // share <file name> <target tracker address>
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
