package top.sclab.java.handler;

import top.sclab.java.service.MessageHandler;

import java.net.InetSocketAddress;
import java.net.Socket;

public class TCPBaseMessageHandler implements MessageHandler {

    // TODO:

    private Socket serverSocket;

    @Override
    public void init() {

    }

    @Override
    public void setTCPSocket(Socket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    public void messageProcess(InetSocketAddress current, byte[] data) {

    }

    @Override
    public void destroy() {

    }
}
