package top.sclab.java.service;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Set;

public interface MessageHandler extends HandlerInit, HandlerDestroy {

    default void setUdpAddresses(Set<InetSocketAddress> addresses) {
    }

    default void setUdpServer(DatagramSocket server) {
    }

    default void udpMessageProcess(InetSocketAddress current, byte[] data) {
    }
}
