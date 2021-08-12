package top.sclab.java;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class ServerBootstrap {

    public static void main(String[] args) {

        int bufLength = 1024;
        DatagramPacket packet = new DatagramPacket(new byte[1024], bufLength);

        DatagramSocket server = null;
        int udpServerPort = ServerConfig.getUDPServerPort();
        try {
            server = new DatagramSocket(udpServerPort);
            System.out.printf("Server:%d is working...\n", udpServerPort);

            InetSocketAddress client1 = null, client2 = null;

            while (true) {
                server.receive(packet);

                String content = new String(packet.getData(), 0, packet.getLength());
                if ("shutdown".equalsIgnoreCase(content)) {
                    if (packet.getAddress().isLoopbackAddress()) {
                        break;
                    }
                } else if ("h".equalsIgnoreCase(content)) {

                    if (client1 == null) {
                        client1 = (InetSocketAddress) packet.getSocketAddress();
                    } else if (!packet.getSocketAddress().equals(client1)) {
                        client2 = (InetSocketAddress) packet.getSocketAddress();
                    }

                    if (client2 == null) {
                        server.send(new DatagramPacket(new byte[]{'b'}, 1, packet.getSocketAddress()));
                    } else {
                        String a = String.format("connect::%s,%d", client2.getHostString(), client2.getPort());
                        server.send(new DatagramPacket(a.getBytes(), a.length(), client1));

                        a = String.format("connect::%s,%d", client1.getHostString(), client1.getPort());
                        server.send(new DatagramPacket(a.getBytes(), a.length(), client2));
                    }
                }

                System.out.printf("Client address:%s msg:%s\n", packet.getSocketAddress(), content);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            safeClose(server);
        }

        System.out.println("Server closed. Byb!");
    }

    public static void safeClose(AutoCloseable... closeables) {
        if (closeables != null && closeables.length > 0) {
            for (AutoCloseable closeable : closeables) {
                if (closeable != null) {
                    try {
                        closeable.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
