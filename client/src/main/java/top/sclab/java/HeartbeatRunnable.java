package top.sclab.java;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

public class HeartbeatRunnable implements Runnable {

    private final byte[] heartbeat = "h".getBytes();

    private final DatagramSocket client;

    private SocketAddress socketAddress;

    public HeartbeatRunnable(DatagramSocket client) {
        this.client = client;
    }

    public void setSocketAddress(SocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    @Override
    public void run() {
        try {
            client.send(new DatagramPacket(heartbeat, heartbeat.length, socketAddress));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
