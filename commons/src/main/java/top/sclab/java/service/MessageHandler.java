package top.sclab.java.service;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public interface MessageHandler extends HandlerInit, HandlerDestroy {

    byte close = 's';
    byte forward = 'f';
    byte heartbeat = 'h';
    byte broadcast = 'b';

    long HeartbeatIntervalTime = TimeUnit.SECONDS.toMillis(15);

    default void setUDPSocket(DatagramSocket serverSocket) {
    }

    default void setTCPSocket(Socket serverSocket) {
    }

    default void messageProcess(InetSocketAddress current, byte[] data) {
    }
}
