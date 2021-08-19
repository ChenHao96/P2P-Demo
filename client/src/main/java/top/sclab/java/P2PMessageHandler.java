package top.sclab.java;

import top.sclab.java.handler.UDPBaseMessageHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class P2PMessageHandler extends UDPBaseMessageHandler {

    private final InetSocketAddress serverAddress;

    private final Map<InetSocketAddress, RunnableScheduledFuture<?>> futureMap = new LinkedHashMap<>();

    private ScheduledThreadPoolExecutor poolExecutor;

    public P2PMessageHandler(InetSocketAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    @Override
    public void init() {
        super.init();

        poolExecutor = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors());

        register(serverAddress, null);
    }

    @Override
    public void destroy() {

        super.destroy();

        poolExecutor.shutdown();
        try {
            if (!poolExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                poolExecutor.shutdownNow();
                if (!poolExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("线程池无法终止");
                }
            }
        } catch (InterruptedException ie) {
            poolExecutor.shutdownNow();
        } finally {
            poolExecutor = null;
        }
    }

    @Override
    public void forward(InetSocketAddress current, ByteBuffer byteBuffer) {

        final String host = AddressUtil.int2IP(byteBuffer.getInt());
        int port = byteBuffer.getShort() & AddressUtil.U_SHORT;

        byte[] strByte = new byte[PING_VALUE.length()];
        byteBuffer.get(strByte);
        String str = new String(strByte);

        if (PING_VALUE.equals(str)) {
            if (current.equals(serverAddress)) {

                final byte[] data = byteBuffer.array();
                InetSocketAddress address = new InetSocketAddress(host, port);
                RunnableScheduledFuture<?> future = (RunnableScheduledFuture<?>) poolExecutor.scheduleAtFixedRate(new Runnable() {
                    private final DatagramPacket packet = new DatagramPacket(data, data.length, address);

                    @Override
                    public void run() {
                        try {
                            socket.send(packet);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, 1500, 150, TimeUnit.MILLISECONDS);
                futureMap.put(address, future);
            } else {

                RunnableScheduledFuture<?> future = futureMap.remove(current);
                if (future != null) {
                    poolExecutor.remove(future);
                }

                byteBuffer.put(Byte.BYTES + Integer.BYTES + Short.BYTES + Byte.BYTES, (byte) 'o');
                final byte[] data = byteBuffer.array();
                try {
                    socket.send(new DatagramPacket(data, data.length, current));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (PONG_VALUE.equals(str)) {
            RunnableScheduledFuture<?> future = futureMap.remove(current);
            if (future != null) {
                poolExecutor.remove(future);
            }
        }
    }

    private static final String PING_VALUE = "ping";
    private static final String PONG_VALUE = "pong";

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

        final String host = AddressUtil.int2IP(byteBuffer.getInt());
        int port = byteBuffer.getShort() & AddressUtil.U_SHORT;

        byteBuffer.put(0, forward);
        final byte[] data = byteBuffer.array();

        final InetSocketAddress address = new InetSocketAddress(host, port);
        RunnableScheduledFuture<?> future = (RunnableScheduledFuture<?>) poolExecutor.scheduleAtFixedRate(new Runnable() {
            private final DatagramPacket packet = new DatagramPacket(data, data.length, address);

            @Override
            public void run() {
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 150, TimeUnit.MILLISECONDS);
        futureMap.put(address, future);

        try {
            socket.send(new DatagramPacket(data, data.length, serverAddress));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
