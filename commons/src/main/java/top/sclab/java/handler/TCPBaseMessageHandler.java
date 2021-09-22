package top.sclab.java.handler;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class TCPBaseMessageHandler extends BaseMessageHandler {

    private Socket serverSocket;

    @Override
    public void setTCPSocket(Socket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    protected void close(InetSocketAddress current, ByteBuffer byteBuffer) {

    }

    @Override
    protected void forward(InetSocketAddress current, ByteBuffer byteBuffer) {

    }

    @Override
    protected void broadcast(InetSocketAddress current, ByteBuffer byteBuffer) {

    }

    @Override
    protected void register(InetSocketAddress current, ByteBuffer byteBuffer) {

    }

    @Override
    protected void sendCloseMessageFromServer() {

    }
}
