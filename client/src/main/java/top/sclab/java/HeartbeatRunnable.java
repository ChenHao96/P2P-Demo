package top.sclab.java;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

public class HeartbeatRunnable implements Runnable {

    private final byte[] heartbeat = "h".getBytes();

    private final DatagramSocket client;

    private DatagramPacket datagramPacket;

    public HeartbeatRunnable(DatagramSocket client) {
        this.client = client;
    }

    public void setSocketAddress(SocketAddress socketAddress) {
        this.datagramPacket = new DatagramPacket(heartbeat, heartbeat.length, socketAddress);
    }

    @Override
    public void run() {
        try {
            client.send(this.datagramPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
