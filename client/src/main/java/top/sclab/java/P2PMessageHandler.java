package top.sclab.java;

import top.sclab.java.handler.UDPBaseMessageHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class P2PMessageHandler extends UDPBaseMessageHandler {

    private final InetSocketAddress serverAddress;

    public P2PMessageHandler(InetSocketAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    @Override
    public void init() {
        super.init();
        register(serverAddress, null);
    }

    @Override
    public void forward(InetSocketAddress current, ByteBuffer byteBuffer) {
        if (!current.equals(serverAddress)) {
            super.forward(current, byteBuffer);
        } else {

            final String host = AddressUtil.int2IP(byteBuffer.getInt());
            int port = byteBuffer.getShort() & AddressUtil.U_SHORT;

            String str = new String(new byte[]{byteBuffer.get(), byteBuffer.get()
                    , byteBuffer.get(), byteBuffer.get(), byteBuffer.get()});
            if (PING_VALUE.equals(str)) {
                if (current.equals(serverAddress)) {
                    try {
                        socket.send(forwardPing(new InetSocketAddress(host, port), current));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        socket.send(new DatagramPacket(PONG_VALUE.getBytes(), PONG_VALUE.length(), current));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static final String PING_VALUE = "ping";
    private static final String PONG_VALUE = "pong";

    private boolean connectPeer(InetSocketAddress current) {

        AtomicBoolean loop = new AtomicBoolean(true);
        new Thread(() -> {
            while (loop.get()) {
                try {
                    socket.send(forwardPing(serverAddress, current));
                    Thread.sleep(250);
                    socket.send(forwardPing(current, current));
                    Thread.sleep(250);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        byte[] bytes = new byte[5];
        final DatagramPacket packet = new DatagramPacket(bytes, bytes.length, current);

        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            while (loop.get()) {
                try {
                    socket.receive(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (PONG_VALUE.equals(new String(packet.getData()))) {
                    latch.countDown();
                    break;
                }
            }
        }).start();

        boolean result = false;
        try {
            result = latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            loop.set(false);
        }
        return result;
    }

    private DatagramPacket forwardPing(InetSocketAddress address, InetSocketAddress current) {
        byte[] data = new byte[Byte.BYTES + Integer.BYTES + Short.BYTES + PING_VALUE.length()];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.put(forward);
        byteBuffer.putInt(AddressUtil.ipToInt(current.getHostString()));
        byteBuffer.putShort((short) current.getPort());
        byteBuffer.put(PING_VALUE.getBytes());
        return new DatagramPacket(data, data.length, address);
    }

    public void broadcastPing() {

        byte[] data = new byte[Byte.BYTES + Integer.BYTES + Short.BYTES + PING_VALUE.length()];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.put(broadcast);
        byteBuffer.putInt(0);
        byteBuffer.putShort((short) 0);
        byteBuffer.put(PING_VALUE.getBytes());

        try {
            socket.send(new DatagramPacket(data, data.length, serverAddress));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void broadcast(InetSocketAddress current, ByteBuffer byteBuffer) {
        if (!current.equals(serverAddress)) {
            super.broadcast(current, byteBuffer);
        } else {
            final String host = AddressUtil.int2IP(byteBuffer.getInt());
            int port = byteBuffer.getShort() & AddressUtil.U_SHORT;
            connectPeer(new InetSocketAddress(host, port));
        }
    }
}
