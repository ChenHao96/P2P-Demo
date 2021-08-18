package top.sclab.java.service;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public interface MessageHandler extends HandlerInit, HandlerDestroy {

    default void setUdpSocket(DatagramSocket socket) {
    }

    default void udpMessageProcess(InetSocketAddress current, byte[] data) {
    }

    void forward(InetSocketAddress current, final int offset, final byte[] data);

    void broadcast(InetSocketAddress current, final int offset, final byte[] data);

    void register(InetSocketAddress current, final int offset, final byte[] data);

    void heartbeat(InetSocketAddress current, final int offset, final byte[] data);

    void close(InetSocketAddress current, final int offset, final byte[] data);
}
