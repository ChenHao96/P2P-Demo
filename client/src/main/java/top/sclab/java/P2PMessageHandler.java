package top.sclab.java;

import top.sclab.java.handler.UDPBaseMessageHandler;

import java.net.InetSocketAddress;

public class P2PMessageHandler extends UDPBaseMessageHandler {

    private InetSocketAddress serverAddress;

    @Override
    public void forward(InetSocketAddress current, int offset, byte[] data) {
        if (!current.equals(serverAddress)) {
            super.forward(current, offset, data);
        } else {
            // TODO:
        }
    }

    @Override
    public void broadcast(InetSocketAddress current, int offset, byte[] data) {
        if (!current.equals(serverAddress)) {
            super.broadcast(current, offset, data);
        } else {
            // TODO:
        }
    }

    @Override
    public void register(InetSocketAddress current, int offset, byte[] data) {
        if (serverAddress == null) {
            serverAddress = current;
        }
        super.register(current, offset, data);
    }
}
