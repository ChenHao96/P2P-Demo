package top.sclab.java;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServerBootstrap {

    public static final String CLIENT_PREFIX = "client::";

    private static final ScheduledThreadPoolExecutor poolExecutor = new ScheduledThreadPoolExecutor(2);

    public static void main(String[] args) throws SocketException {

        int bufLength = 1024;
        DatagramPacket packet = new DatagramPacket(new byte[1024], bufLength);

        int udpServerPort = ServerConfig.getUDPServerPort();
        DatagramSocket server = new DatagramSocket(udpServerPort);
        System.out.printf("Server:%d is working...\n", udpServerPort);

        try {
            InetSocketAddress client1 = null, client2 = null;
            Map<InetSocketAddress, URI> clientIpMap = new HashMap<>();

            while (true) {
                server.receive(packet);

                String content = new String(packet.getData(), 0, packet.getLength());
                System.out.printf("Client address:%s msg:%s\n", packet.getSocketAddress(), content);

                if (content.startsWith(CLIENT_PREFIX)) {

                    InetSocketAddress socketAddress = (InetSocketAddress) packet.getSocketAddress();
                    String clientAddress = content.substring(CLIENT_PREFIX.length());
                    clientIpMap.put(socketAddress, new URI(clientAddress));

                    if (client1 == null) {
                        client1 = socketAddress;
                    } else if (!packet.getSocketAddress().equals(client1)) {
                        client2 = socketAddress;
                    }

                    if (client2 != null) {
                        URI host = clientIpMap.get(client2);
                        if (!client2.getHostString().equals(client1.getHostString())) {
                            host = new URI("udp", null, client2.getHostString(), client2.getPort(), null, null, null);
                        }

                        String a = String.format("connect::%s%s,%d", CLIENT_PREFIX, host.getHost(), host.getPort());
                        server.send(new DatagramPacket(a.getBytes(), a.length(), client1));

                        host = clientIpMap.get(client1);
                        if (!client1.getHostString().equals(client2.getHostString())) {
                            host = new URI("udp", null, client1.getHostString(), client1.getPort(), null, null, null);
                        }

                        a = String.format("connect::%s%s,%d", CLIENT_PREFIX, host.getHost(), host.getPort());
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
