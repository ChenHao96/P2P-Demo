package top.sclab.java.handler;

import top.sclab.java.Constant;
import top.sclab.java.ServerConfig;
import top.sclab.java.model.UDPReceiveItem;
import top.sclab.java.service.MessageHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class UDPBaseMessageHandler implements MessageHandler {

    private ScheduledThreadPoolExecutor poolExecutor;

    private Set<InetSocketAddress> addresses;

    private Map<InetSocketAddress, UDPReceiveItem> clientMap;

    private DatagramSocket server;

    @Override
    public void setUdpAddresses(Set<InetSocketAddress> addresses) {
        this.addresses = addresses;
    }

    @Override
    public void setUdpServer(DatagramSocket server) {
        this.server = server;
    }

    @Override
    public void init() {

        int enableCount = ServerConfig.getEnableCount();
        poolExecutor = new ScheduledThreadPoolExecutor(enableCount + 1);

        float loadFactor = 0.75f;
        int initialCapacity = (int) (enableCount / loadFactor) + 1;
        if (clientMap == null) {
            clientMap = new HashMap<>(initialCapacity, loadFactor);
        }

        poolExecutor.scheduleAtFixedRate(() -> {

            final long currentTime = System.currentTimeMillis();
            Set<Map.Entry<InetSocketAddress, UDPReceiveItem>> entries = clientMap.entrySet();
            Iterator<Map.Entry<InetSocketAddress, UDPReceiveItem>> iterator = entries.iterator();
            while (iterator.hasNext()) {

                Map.Entry<InetSocketAddress, UDPReceiveItem> entry = iterator.next();
                long lastTime = entry.getValue().getLastUpdateTime();
                if (Math.abs(currentTime - lastTime) > TimeUnit.SECONDS.toMillis(30)) {
                    poolExecutor.remove(entry.getValue().getFuture());
                    addresses.remove(entry.getKey());
                    iterator.remove();
                }
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void udpMessageProcess(InetSocketAddress current, byte[] data) {

        if (data != null && data.length > 0) {

            ByteBuffer byteBuffer = ByteBuffer.wrap(data);
            byte cmd = byteBuffer.get();
            switch (cmd) {

                case Constant.heartbeat:
                    heartbeat(current, byteBuffer);
                    break;
                case Constant.connect:
                    connect(current, byteBuffer);
                    break;
                case Constant.broadcast:
                    broadcast(current, byteBuffer);
                    break;
                case Constant.forward:
                    forward(current, byteBuffer);
                    break;
            }
        }
    }

    @Override
    public void destroy() {
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

    public void heartbeat(InetSocketAddress current, ByteBuffer byteBuffer) {
        // TODO:
    }

    public void forward(InetSocketAddress current, ByteBuffer byteBuffer) {
        // TODO:
    }

    public void broadcast(InetSocketAddress current, ByteBuffer byteBuffer) {

        addresses.forEach(address -> {

        });
    }

    public void connect(InetSocketAddress current, ByteBuffer byteBuffer) {

        // heartbeat 延时10秒执行 每30秒执行
        RunnableScheduledFuture<?> future = (RunnableScheduledFuture<?>) poolExecutor.scheduleAtFixedRate(new Runnable() {
            private final byte[] heartbeat = new byte[]{Constant.heartbeat};
            private final DatagramPacket packet = new DatagramPacket(heartbeat, heartbeat.length, current);

            @Override
            public void run() {
                try {
                    server.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 10, 30, TimeUnit.SECONDS);

        UDPReceiveItem udpReceiveItem = new UDPReceiveItem();
        udpReceiveItem.setLastUpdateTime(System.currentTimeMillis());
        udpReceiveItem.setFuture(future);
        clientMap.put(current, udpReceiveItem);
    }
}
