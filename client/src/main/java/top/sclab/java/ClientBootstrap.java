package top.sclab.java;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ClientBootstrap {

    private static final ScheduledThreadPoolExecutor poolExecutor = new ScheduledThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 3);

    public static void main(String[] args) throws IOException, InterruptedException {

        String clientIp = "h";
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
            clientIp = addr.getHostAddress();
        }

        // p2p登录服务器
//        SocketAddress socketAddress = new InetSocketAddress("8.136.189.243", 8880);
        SocketAddress socketAddress = new InetSocketAddress("localhost", 8880);

        DatagramSocket client = new DatagramSocket();
        CountDownLatch countDownLatch = new CountDownLatch(1);

        String clientAddress = String.format("client::udp://%s:%d", clientIp, client.getLocalPort());
        client.send(new DatagramPacket(clientAddress.getBytes(), clientAddress.length(), socketAddress));

        HeartbeatRunnable heartbeatRunnable = new HeartbeatRunnable(client);
        MessageManager.setHeartbeatRunnable(heartbeatRunnable);
        heartbeatRunnable.setSocketAddress(socketAddress);

        MessageReceiveRunnable messageProcessRunnable = new MessageReceiveRunnable(client);
        MessageManager.setMessageReceiveRunnable(messageProcessRunnable);
        messageProcessRunnable.setSocketAddress(socketAddress);

        MessageSendRunnable clientHoleRunnable = new MessageSendRunnable(client);
        MessageManager.setMessageSendRunnable(clientHoleRunnable);
        clientHoleRunnable.setSocketAddress(socketAddress);

        poolExecutor.scheduleAtFixedRate(heartbeatRunnable, 10, 15, TimeUnit.SECONDS);
        poolExecutor.schedule(messageProcessRunnable, 0, TimeUnit.SECONDS);
        poolExecutor.schedule(clientHoleRunnable, 0, TimeUnit.SECONDS);

        countDownLatch.await();
        client.close();

        poolExecutor.shutdown();
        try {
            if (!poolExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                poolExecutor.shutdownNow();
                if (!poolExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            poolExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
