package top.sclab.java.service;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;

public interface MessageHandler extends HandlerInit, HandlerDestroy {

    default void setUDPSocket(DatagramSocket serverSocket) {
    }

    default void setTCPSocket(Socket serverSocket) {
    }

    default void messageProcess(InetSocketAddress current, byte[] data) {
    }
}
