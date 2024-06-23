import models.Peer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URISyntaxException;


public class RunPeer {
    public static void main(String[] args) throws IOException, URISyntaxException {
//        DatagramSocket socket;
//        InetAddress address;
//
//        byte[] buf;
//
//        socket = new DatagramSocket();
//        address = InetAddress.getByName("localhost");
//
//        buf = "thtyhyhythyththt".getBytes();
//        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 12345);
//        socket.send(packet);
////        packet = new DatagramPacket(buf, buf.length);
////        socket.receive(packet);
////        String received = new String(
////                packet.getData(), 0, packet.getLength());
////
////        System.out.println(received);
//
//        socket.close();


        Peer p = new Peer("mammad", "127.0.0.1:6549");
//        p.sendKeepAliveResponse("127.0.0.1:2345");
    }
}
