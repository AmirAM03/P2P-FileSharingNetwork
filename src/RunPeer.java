import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class RunPeer {
    public static void main(String[] args) throws IOException {
        DatagramSocket socket;
        InetAddress address;

        byte[] buf;

        socket = new DatagramSocket();
        address = InetAddress.getByName("localhost");

        buf = "thtyhyhythyththt".getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 12345);
        socket.send(packet);
//        packet = new DatagramPacket(buf, buf.length);
//        socket.receive(packet);
//        String received = new String(
//                packet.getData(), 0, packet.getLength());
//
//        System.out.println(received);

        socket.close();
    }
}
