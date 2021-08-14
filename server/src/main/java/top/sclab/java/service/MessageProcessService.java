package top.sclab.java.service;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Set;

public interface MessageProcessService {

    default void udpMessageProcess(DatagramSocket server, Set<InetSocketAddress> addresses, byte[] data) {

    }
}
