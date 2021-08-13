package top.sclab.java;

import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class ServerBootstrap {

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

                if (content.startsWith(Constant.CLIENT_PREFIX)) {

                    InetSocketAddress socketAddress = (InetSocketAddress) packet.getSocketAddress();

                    if (client1 == null) {
                        client1 = socketAddress;
                        URI clientUri = Constant.parsingAddress(content);
                        clientIpMap.put(socketAddress, clientUri);
                    } else {
                        client2 = socketAddress;
                        URI clientUri = Constant.parsingAddress(String.format("%s?client=true", content));
                        clientIpMap.put(socketAddress, clientUri);
                    }

                    if (client2 != null) {

                        URI host = clientIpMap.get(client2);
                        if (!client2.getHostString().equals(client1.getHostString())) {
                            host = Constant.parsingAddress(client2, "");
                        }

                        String p2p_address = Constant.formatAddress(host);
                        server.send(new DatagramPacket(p2p_address.getBytes(), p2p_address.length(), client1));

                        host = clientIpMap.get(client1);
                        if (!client1.getHostString().equals(client2.getHostString())) {
                            host = Constant.parsingAddress(client1, "?client=true");
                        }

                        p2p_address = Constant.formatAddress(host);
                        server.send(new DatagramPacket(p2p_address.getBytes(), p2p_address.length(), client2));
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
