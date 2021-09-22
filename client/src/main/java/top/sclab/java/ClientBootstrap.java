package top.sclab.java;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;

public class ClientBootstrap {

    private static final class ObjectStore<T> {

        private T value;

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = value;
        }
    }

    public static void main(String[] args) {

        String port = System.getProperty("port", "8880");
        String host = System.getProperty("host", "localhost");
        InetSocketAddress serverAddress = new InetSocketAddress(host, Integer.parseInt(port));

        ObjectStore<SocketAddress> objectStore = new ObjectStore<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);

        new Thread(() -> {
            try {
                Socket socket = new Socket();
                socket.setReuseAddress(true);
                socket.connect(serverAddress);
                objectStore.set(socket.getLocalSocketAddress());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                countDownLatch.countDown();
            }
        }).start();

        new Thread(() -> {
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            SocketAddress socketAddress = objectStore.get();
            if (socketAddress == null) {
                System.out.println("SocketAddress is null.");
                return;
            } else {
                System.out.println(socketAddress);
            }

            try {
                ServerSocket serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(socketAddress, 2);
                serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main2(String[] args) throws SocketException {

        Enumeration<NetworkInterface> faces = NetworkInterface.getNetworkInterfaces();
        while (faces.hasMoreElements()) {
            NetworkInterface face = faces.nextElement();
            if (face.getDisplayName().contains("Adapter")) {
                continue;
            }
            if (face.isLoopback() || face.isVirtual() || !face.isUp()) {
                continue;
            }

            System.out.printf("网络设备: %s\n", face.getDisplayName());

            System.out.print("物理地址: ");
            byte[] mac = face.getHardwareAddress();
            for (int i = 0; i < mac.length; i++) {
                System.out.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : "");
            }
            System.out.println();

            InetAddress addr = face.getInetAddresses().nextElement();
            System.out.printf("主机名称: %s\n", addr.getHostName());
            System.out.printf("网络地址: %s\n", addr.getHostAddress());
        }

        DatagramSocket socket = new DatagramSocket();
        System.out.printf("local socket port:%d\n", socket.getLocalPort());

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

        messageHandler.broadcastPing();

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
