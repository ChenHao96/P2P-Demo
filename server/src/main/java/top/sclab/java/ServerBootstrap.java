package top.sclab.java;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class ServerBootstrap {

    public static final String CLIENT_PREFIX = "client::";

    public static void main(String[] args) {

        int bufLength = 1024;
        DatagramPacket packet = new DatagramPacket(new byte[1024], bufLength);

        DatagramSocket server = null;
        int udpServerPort = ServerConfig.getUDPServerPort();
        try {
            server = new DatagramSocket(udpServerPort);
            System.out.printf("Server:%d is working...\n", udpServerPort);

            InetSocketAddress client1 = null, client2 = null;
            Map<InetSocketAddress, String> clientIpMap = new HashMap<>();

            while (true) {
                server.receive(packet);

                String content = new String(packet.getData(), 0, packet.getLength());
                System.out.printf("Client address:%s msg:%s\n", packet.getSocketAddress(), content);

                if ("shutdown".equalsIgnoreCase(content)) {
                    if (packet.getAddress().isLoopbackAddress()) {
                        break;
                    }
                } else if ("h".equalsIgnoreCase(content)) {
                    server.send(new DatagramPacket(new byte[]{'b'}, 1, packet.getSocketAddress()));
                } else if (content.startsWith(CLIENT_PREFIX)) {
                    String clientIp = content.substring(CLIENT_PREFIX.length());
                    clientIpMap.put((InetSocketAddress) packet.getSocketAddress(), clientIp);

                    if (client1 == null) {
                        client1 = (InetSocketAddress) packet.getSocketAddress();
                    } else if (!packet.getSocketAddress().equals(client1)) {
                        client2 = (InetSocketAddress) packet.getSocketAddress();
                    }

                    if (client2 != null) {
                        String host = client2.getHostString();
                        if (host.equals(client1.getHostString())) {
                            host = clientIpMap.get(client1);
                        }

                        String a = String.format("connect::%s,%d", host, client2.getPort());
                        server.send(new DatagramPacket(a.getBytes(), a.length(), client1));

                        host = client1.getHostString();
                        if (host.equals(client2.getHostString())) {
                            host = clientIpMap.get(client2);
                        }

                        a = String.format("connect::%s,%d", host, client1.getPort());
                        server.send(new DatagramPacket(a.getBytes(), a.length(), client2));
                        break;
                    }
                }
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
