package top.sclab.java;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;

public class MessageProcessRunnable implements Runnable {

    private final byte[] data = new byte[1024];

    private DatagramPacket receive;

    private HeartbeatRunnable heartbeatRunnable;

    private final DatagramSocket client;

    private final CountDownLatch countDownLatch;

    public MessageProcessRunnable(DatagramSocket client, CountDownLatch countDownLatch) {
        this.client = client;
        this.countDownLatch = countDownLatch;
    }

    public void setHeartbeatRunnable(HeartbeatRunnable heartbeatRunnable) {
        this.heartbeatRunnable = heartbeatRunnable;
        this.receive = new DatagramPacket(data, data.length, heartbeatRunnable.getSocketAddress());
    }

    private static final String PREFIX = "connect::";

    @Override
    public void run() {
        while (true) {

            try {
                client.receive(receive);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            String content = new String(receive.getData(), 0, receive.getLength());
            System.out.printf("Server msg:%s\n", content);

            if ("close".equalsIgnoreCase(content)) {

                countDownLatch.countDown();
                break;

            } else if (content.startsWith(PREFIX)) {

                String connect = content.substring(PREFIX.length());
                String[] addresses = connect.split(",");

                SocketAddress socketAddress = new InetSocketAddress(addresses[0], Integer.parseInt(addresses[1]));
                heartbeatRunnable.setSocketAddress(socketAddress);
            }
        }
    }
}
