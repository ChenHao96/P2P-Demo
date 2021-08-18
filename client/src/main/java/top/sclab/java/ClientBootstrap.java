package top.sclab.java;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class ClientBootstrap {

    public static void main(String[] args) throws SocketException {

        DatagramSocket socket = new DatagramSocket();
        String port = System.getProperty("port", "8880");
        String host = System.getProperty("host", "localhost");
        InetSocketAddress serverAddress = new InetSocketAddress(host, Integer.parseInt(port));

        P2PMessageHandler messageHandler = new P2PMessageHandler(serverAddress);
        messageHandler.setUdpSocket(socket);
        messageHandler.init();

        UDPClientHandler udpHandler = new UDPClientHandler(socket, serverAddress);
        udpHandler.setMessageProcessService(messageHandler);
        udpHandler.init();

        new Thread(udpHandler).start();

        messageHandler.connectPeer(new InetSocketAddress("127.0.0.1",63681));

        final Runnable destroy = () -> {
            udpHandler.destroy();
            messageHandler.destroy();
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        Runtime.getRuntime().addShutdownHook(new Thread(destroy));
    }
}
