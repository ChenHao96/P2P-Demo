package top.sclab.java;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;

public class ClientBootstrap {

    public static void main(String[] args) throws IOException, InterruptedException {

        String clientIp = "";
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
            clientIp = addr.getHostAddress();
        }

        DatagramSocket client = new DatagramSocket();
        SocketAddress socketAddress = new InetSocketAddress("localhost", 8880);

        if (!"".equals(clientIp)) {

            System.out.printf("网络地址: %s:%d\n", clientIp, client.getLocalPort());
            String clientAddress = String.format("client::udp://%s:%d", clientIp, client.getLocalPort());
            client.send(new DatagramPacket(clientAddress.getBytes(), clientAddress.length(), socketAddress));
        }

        HeartbeatRunnable heartbeatRunnable = new HeartbeatRunnable(client);
        MessageManager.setHeartbeatRunnable(heartbeatRunnable);
        heartbeatRunnable.setSocketAddress(socketAddress);

        MessageReceiveRunnable messageReceiveRunnable = new MessageReceiveRunnable(client);
        messageReceiveRunnable.setSocketAddress(socketAddress);

        MessageSendRunnable messageSendRunnable = new MessageSendRunnable(client);
        MessageManager.setMessageSendRunnable(messageSendRunnable);
        messageSendRunnable.setSocketAddress(socketAddress);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        MessageManager.setCountDownLatch(countDownLatch);

        Thread read = new Thread(messageReceiveRunnable);
        read.setDaemon(true);
        read.start();

        Thread write = new Thread(messageSendRunnable);
        write.setDaemon(true);
        write.start();

        countDownLatch.await();
        client.close();
    }
}
