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

    private static final String PING_VALUE = "ping";
    private static final String PONG_VALUE = "pong";
    private static final long RETRY_TIME = TimeUnit.SECONDS.toMillis(30);

    private static final long PERIOD_MS = 250;
    private static final int RETRY_COUNT = (int) (RETRY_TIME / PERIOD_MS);

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

    public void broadcastPing() {

        ByteBuffer byteBuffer = createByteBuffer(broadcast, "0.0.0.0", 0, PING_VALUE.getBytes());
        final byte[] data = byteBuffer.array();

        try {
            socket.send(new DatagramPacket(data, data.length, serverAddress));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void forward(InetSocketAddress current, ByteBuffer byteBuffer) {

        InetSocketAddress address = getAddress(byteBuffer);

        byte[] strByte = new byte[PING_VALUE.length()];
        getValue(byteBuffer, strByte);
        String str = new String(strByte);

        if (PING_VALUE.equals(str)) {
            if (current.equals(serverAddress)) {

                putLocalPort(byteBuffer, socket.getLocalPort());
                final byte[] data = byteBuffer.array();
                RunnableScheduledFuture<?> future = (RunnableScheduledFuture<?>) poolExecutor.scheduleAtFixedRate(new Runnable() {
                    private final DatagramPacket packet = new DatagramPacket(data, data.length, address);
                    private final AtomicInteger atomicInteger = new AtomicInteger(RETRY_COUNT);

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
                }, 0, PERIOD_MS, TimeUnit.MILLISECONDS);
                futureMap.put(address, future);
            } else {

                RunnableScheduledFuture<?> future = futureMap.remove(current);
                if (future != null) {
                    poolExecutor.remove(future);
                }

                putValue(byteBuffer, PONG_VALUE.getBytes());
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
    public void broadcast(InetSocketAddress current, ByteBuffer byteBuffer) {

        final InetSocketAddress address = getAddress(byteBuffer);
        int localPort = getLocalPort(byteBuffer);

        setCmd(byteBuffer, forward);
        long initialDelay = socket.getLocalPort() == localPort ? PERIOD_MS * 2 : 0;
        putLocalPort(byteBuffer, socket.getLocalPort());

        final byte[] data = byteBuffer.array();
        RunnableScheduledFuture<?> future = (RunnableScheduledFuture<?>) poolExecutor.scheduleAtFixedRate(new Runnable() {
            private final DatagramPacket packet = new DatagramPacket(data, data.length, address);
            private final AtomicInteger atomicInteger = new AtomicInteger(RETRY_COUNT);

            @Override
            public void run() {
                try {
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.printf("broadcast ping -> %s\n", address);
                    if (atomicInteger.decrementAndGet() < 0) {
                        RunnableScheduledFuture<?> future = futureMap.remove(address);
                        if (future != null) {
                            future.cancel(true);
                        }
                    }
                }
            }
        }, initialDelay, PERIOD_MS, TimeUnit.MILLISECONDS);
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
