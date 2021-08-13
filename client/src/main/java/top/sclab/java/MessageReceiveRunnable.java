package top.sclab.java;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

public class MessageReceiveRunnable implements Runnable {

    private final byte[] data = new byte[1024];

    private DatagramPacket receive;

    private final DatagramSocket client;

    public MessageReceiveRunnable(DatagramSocket client) {
        this.client = client;
    }

    public void setSocketAddress(SocketAddress socketAddress) {
        this.receive = new DatagramPacket(data, data.length, socketAddress);
    }

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
            System.out.printf("Receive Message :: %s\n", content);

            if (MessageManager.CONNECT_CLOSE_VALUE.equals(content)) {
                break;
            } else {
                MessageManager.addMessage(content);
            }
        }
    }
}
