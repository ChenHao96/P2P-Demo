package top.sclab.java;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

public class MessageSendRunnable implements Runnable {

    private final DatagramSocket client;

    private SocketAddress socketAddress;

    public MessageSendRunnable(DatagramSocket client) {
        this.client = client;
    }

    public void setSocketAddress(SocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }

    @Override
    public void run() {
        while (true) {

            String message = "";
            try {
                message = MessageManager.popMessage();
                System.out.printf("Send Message: {%s} -> %s\n", message, socketAddress);
                client.send(new DatagramPacket(message.getBytes(), message.length(), socketAddress));
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

            if (Constant.CONNECT_CLOSE_VALUE.equals(message)) {
                break;
            }
        }
    }
}
