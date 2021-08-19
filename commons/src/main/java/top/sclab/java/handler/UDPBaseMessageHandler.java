package top.sclab.java.handler;

import top.sclab.java.AddressUtil;
import top.sclab.java.model.UDPReceiveItem;
import top.sclab.java.service.MessageHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class UDPBaseMessageHandler implements MessageHandler {

    public static final byte close = 's';
    public static final byte forward = 'f';
    public static final byte heartbeat = 'h';
    public static final byte broadcast = 'b';

    protected DatagramSocket socket;

    private ScheduledThreadPoolExecutor poolExecutor;

    private Map<InetSocketAddress, UDPReceiveItem> clientMap;

    private final Object clientMapLock = new Object();

    private volatile boolean activated = false;

    @Override
    public void setUdpSocket(DatagramSocket socket) {
        this.socket = socket;
    }

    private static final long HeartbeatIntervalTime = TimeUnit.SECONDS.toMillis(15);

    @Override
    public void init() {

        final int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        poolExecutor = new ScheduledThreadPoolExecutor(corePoolSize);

        if (clientMap == null) {
            clientMap = new LinkedHashMap<>();
        }

        final long period = HeartbeatIntervalTime * 2;
        poolExecutor.scheduleAtFixedRate(() -> {
            final long currentTime = System.currentTimeMillis();
            synchronized (clientMapLock) {
                System.out.printf("开始检查存活连接  %d\n", clientMap.size());
                Set<Map.Entry<InetSocketAddress, UDPReceiveItem>> entries = clientMap.entrySet();
                Iterator<Map.Entry<InetSocketAddress, UDPReceiveItem>> iterator = entries.iterator();
                while (iterator.hasNext()) {
                    Map.Entry<InetSocketAddress, UDPReceiveItem> entry = iterator.next();
                    UDPReceiveItem item = entry.getValue();
                    long lastTime = item.getLastUpdateTime();
                    if (currentTime - lastTime >= period) {
                        poolExecutor.remove(item.getFuture());
                        iterator.remove();
                    }
                }
                System.out.printf("结束检查存活连接  %d\n", clientMap.size());
            }
        }, period, period, TimeUnit.MILLISECONDS);

        this.activated = true;
    }

    @Override
    public void udpMessageProcess(InetSocketAddress current, byte[] data) {

        if (!activated) {
            return;
        }

        if (data != null && data.length > 0) {

            ByteBuffer byteBuffer = ByteBuffer.wrap(data);

            byte cmd = byteBuffer.get();
            int offset = Byte.BYTES;

            register(current, offset, byteBuffer);

            switch (cmd) {
                case heartbeat:
                    heartbeat(current, offset, byteBuffer);
                    break;
                case broadcast:
                    broadcast(current, offset, byteBuffer);
                    break;
                case forward:
                    forward(current, offset, byteBuffer);
                    break;
                case close:
                    close(current, offset, byteBuffer);
                    break;
            }
        }
    }

    private static final byte[] closeByte = new byte[]{close};

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

        this.activated = false;

        Set<Map.Entry<InetSocketAddress, UDPReceiveItem>> entries = clientMap.entrySet();
        Iterator<Map.Entry<InetSocketAddress, UDPReceiveItem>> iterator = entries.iterator();
        DatagramPacket packet = null;
        while (iterator.hasNext()) {

            InetSocketAddress address = iterator.next().getKey();
            if (packet == null) {
                packet = new DatagramPacket(closeByte, closeByte.length, address);
            } else {
                packet.setSocketAddress(address);
            }

            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                iterator.remove();
            }
        }
    }

    public void forward(InetSocketAddress current, int offset, ByteBuffer byteBuffer) {

        final String host = AddressUtil.int2IP(byteBuffer.getInt());
        int port = byteBuffer.getShort() & AddressUtil.U_SHORT;
        InetSocketAddress address = new InetSocketAddress(host, port);

        synchronized (clientMapLock) {
            if (clientMap.containsKey(address)) {

                byteBuffer.putInt(offset, AddressUtil.ipToInt(current.getHostString()));
                byteBuffer.putShort(offset + Integer.BYTES, (short) port);
                try {
                    byte[] data = byteBuffer.array();
                    socket.send(new DatagramPacket(data, data.length, address));
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.printf("forward %s -> %s\n", current, address);
                }
            }
        }
    }

    public void broadcast(InetSocketAddress current, int offset, ByteBuffer byteBuffer) {

        byteBuffer.putInt(AddressUtil.ipToInt(current.getHostString()));
        byteBuffer.putShort((short) current.getPort());

        byte[] data = byteBuffer.array();
        final DatagramPacket packet = new DatagramPacket(data, data.length, current);
        synchronized (clientMapLock) {
            clientMap.forEach((address, udpReceiveItem) -> {
                if (current.equals(address)) {
                    return;
                }
                try {
                    packet.setSocketAddress(address);
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.printf("broadcast %s -> %s\n", current, address);
                }
            });
        }
    }

    private static final byte[] heartbeatByte = new byte[]{heartbeat};

    public void register(InetSocketAddress current, int offset, ByteBuffer byteBuffer) {

        if (clientMap.containsKey(current)) {
            return;
        }

        synchronized (clientMapLock) {
            if (clientMap.containsKey(current)) {
                return;
            }

            RunnableScheduledFuture<?> future = (RunnableScheduledFuture<?>) poolExecutor.scheduleAtFixedRate(new Runnable() {
                private final DatagramPacket packet = new DatagramPacket(heartbeatByte, heartbeatByte.length, current);

                @Override
                public void run() {
                    try {
                        socket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        System.out.printf("发送心跳 -> %s\n", packet.getSocketAddress());
                    }
                }
            }, 0, HeartbeatIntervalTime, TimeUnit.MILLISECONDS);

            UDPReceiveItem udpReceiveItem = new UDPReceiveItem();
            udpReceiveItem.setLastUpdateTime(System.currentTimeMillis());
            udpReceiveItem.setFuture(future);
            clientMap.put(current, udpReceiveItem);
        }
    }

    public void close(InetSocketAddress current, int offset, ByteBuffer byteBuffer) {
        synchronized (clientMapLock) {
            UDPReceiveItem item = clientMap.remove(current);
            if (item != null) {
                poolExecutor.remove(item.getFuture());
            }
        }
    }

    public void heartbeat(InetSocketAddress current, int offset, ByteBuffer byteBuffer) {
        synchronized (clientMapLock) {
            UDPReceiveItem receiveItem = clientMap.get(current);
            if (receiveItem != null) {
                receiveItem.setLastUpdateTime(System.currentTimeMillis());
            }
        }
    }
}
