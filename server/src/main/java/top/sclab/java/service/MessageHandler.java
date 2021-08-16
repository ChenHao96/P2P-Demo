package top.sclab.java.service;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public interface MessageHandler extends HandlerInit, HandlerDestroy {

    default void setUdpServer(DatagramSocket server) {
    }

    default void udpMessageProcess(InetSocketAddress current, byte[] data) {
    }
}
