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
import java.util.concurrent.atomic.AtomicInteger;

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

        register(serverAddress, 0, null);
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
    public void forward(InetSocketAddress current, int offset, ByteBuffer byteBuffer) {

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
                    private final AtomicInteger atomicInteger = new AtomicInteger(150);

                    @Override
                    public void run() {
                        try {
                            socket.send(packet);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            System.out.printf("forward ping -> %s\n", address);
                            if (atomicInteger.decrementAndGet() < 0) {
                                RunnableScheduledFuture<?> future = futureMap.remove(address);
                                if (future != null) {
                                    future.cancel(true);
                                }
                            }
                        }
                    }
                }, 0, 200, TimeUnit.MILLISECONDS);
                futureMap.put(address, future);
            } else {

                RunnableScheduledFuture<?> future = futureMap.remove(current);
                if (future != null) {
                    poolExecutor.remove(future);
                }

                byteBuffer.put(offset + Integer.BYTES + Short.BYTES + Byte.BYTES, (byte) 'o');
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

    @Override
    public void broadcast(InetSocketAddress current, int offset, ByteBuffer byteBuffer) {

        final String host = AddressUtil.int2IP(byteBuffer.getInt());
        int port = byteBuffer.getShort() & AddressUtil.U_SHORT;

        byteBuffer.put(offset - Byte.BYTES, forward);
        final byte[] data = byteBuffer.array();

        final InetSocketAddress address = new InetSocketAddress(host, port);

        Runnable broadcast = new Runnable() {
            private final DatagramPacket packet = new DatagramPacket(data, data.length, address);
            private final AtomicInteger atomicInteger = new AtomicInteger(150);

            @Override
            public void run() {
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.printf("broadcast ping -> %s\n", address);
                    int value = atomicInteger.decrementAndGet();
                    if (value > 0) {
                        RunnableScheduledFuture<?> future;
                        if (value < 100) {
                            future = (RunnableScheduledFuture<?>) poolExecutor.schedule(
                                    this, 500, TimeUnit.MILLISECONDS);
                        } else {
                            future = (RunnableScheduledFuture<?>) poolExecutor.schedule(
                                    this, 200, TimeUnit.MILLISECONDS);
                        }
                        futureMap.put(address, future);
                    } else {
                        RunnableScheduledFuture<?> future = futureMap.remove(address);
                        if (future != null) {
                            future.cancel(true);
                        }
                    }
                }
            }
        };

        RunnableScheduledFuture<?> future = (RunnableScheduledFuture<?>) poolExecutor.schedule(
                broadcast, 0, TimeUnit.MILLISECONDS);
        futureMap.put(address, future);

        try {
            socket.send(new DatagramPacket(data, data.length, serverAddress));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
}
