package top.sclab.java;

import java.net.*;

public class ClientBootstrap {

    public static void main(String[] args) throws SocketException {

        DatagramSocket socket = new DatagramSocket();
        String host = System.getProperty("host", "localhost");
        String port = System.getProperty("port", "8880");
        SocketAddress socketAddress = new InetSocketAddress(host, Integer.parseInt(port));

//        byte[] data = "h1234567891234567890123456789012345678901234567111".getBytes();
        byte[] data = "h12345678912345678901234567890123456789012345671122".getBytes();

        try {
            socket.send(new DatagramPacket(data, data.length, socketAddress));
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
