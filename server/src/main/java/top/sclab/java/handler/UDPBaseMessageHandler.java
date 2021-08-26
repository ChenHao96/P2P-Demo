package top.sclab.java.handler;

import org.springframework.data.redis.core.StringRedisTemplate;
import top.sclab.java.model.UDPReceiveItem;
import top.sclab.java.service.MessageHandler;
import top.sclab.java.util.IPUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class UDPBaseMessageHandler implements MessageHandler {

    public static final byte close = 's';
    public static final byte forward = 'f';
    public static final byte heartbeat = 'h';
    public static final byte broadcast = 'b';

    protected DatagramSocket socket;

    private ScheduledThreadPoolExecutor poolExecutor;

    private Map<InetSocketAddress, UDPReceiveItem> clientMap;

    private final Object clientMapLock = new Object();

    private volatile boolean activated = false;

    private final StringRedisTemplate redisTemplate;

    public UDPBaseMessageHandler(StringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate);
    }

    @Override
    public void setUdpSocket(DatagramSocket socket) {
        this.socket = socket;
    }

    private static final long HeartbeatIntervalTime = TimeUnit.SECONDS.toMillis(15);

    public abstract void processData(InetSocketAddress current, ByteBuffer byteBuffer);

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
            byte cmd = getCmd(byteBuffer);
            register(current, byteBuffer);

            switch (cmd) {
                case heartbeat:
                    heartbeat(current);
                    break;
                case broadcast:
                    broadcast(current, byteBuffer);
                    break;
                case forward:
                    forward(current, byteBuffer);
                    break;
                case close:
                    close(current);
                    break;
                default:
                    processData(current, byteBuffer);
                    break;
            }
        }
    }

    @Override
    public void destroy() {

        this.activated = false;

        if (poolExecutor != null) {
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

        if (clientMap == null || clientMap.size() == 0) {
            return;
        }

        final byte[] data = new byte[]{close};
        Set<Map.Entry<InetSocketAddress, UDPReceiveItem>> entries = clientMap.entrySet();
        Iterator<Map.Entry<InetSocketAddress, UDPReceiveItem>> iterator = entries.iterator();
        DatagramPacket packet = null;
        while (iterator.hasNext()) {

            InetSocketAddress address = iterator.next().getKey();
            if (packet == null) {
                packet = new DatagramPacket(data, data.length, address);
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

    public void forward(InetSocketAddress current, ByteBuffer byteBuffer) {

        InetSocketAddress address = getAddress(byteBuffer);
        synchronized (clientMapLock) {
            if (clientMap.containsKey(address)) {

                setAddress(byteBuffer, current.getHostString(), current.getPort());
                putLocalPort(byteBuffer, address.getPort());
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

    public void broadcast(InetSocketAddress current, ByteBuffer byteBuffer) {

        setAddress(byteBuffer, current.getHostString(), current.getPort());

        byte[] data = byteBuffer.array();
        final DatagramPacket packet = new DatagramPacket(data, data.length, current);
        synchronized (clientMapLock) {
            clientMap.forEach((address, udpReceiveItem) -> {
                if (current.equals(address)) {
                    return;
                }
                try {
                    putLocalPort(byteBuffer, address.getPort());
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

    private boolean checkAndRegisterConnect(InetSocketAddress current, ByteBuffer byteBuffer) {

        int localPort = getLocalPort(byteBuffer);
        String natAddress = current.getHostString();
        int natPort = current.getPort();

        byte[] data = new byte[32];
        getData(byteBuffer, data);
        String token = new String(data);
        // TODO: 校验token

        String cacheKey = String.format("user_connect_list_%s", token);
        redisTemplate.boundListOps(cacheKey);


        return false;
    }

    private static final byte[] heartbeats = new byte[]{heartbeat};

    public void register(InetSocketAddress current, ByteBuffer byteBuffer) {

        if (clientMap.containsKey(current)) {
            return;
        }

        synchronized (clientMapLock) {
            if (clientMap.containsKey(current)) {
                return;
            }

            if (!checkAndRegisterConnect(current, byteBuffer)) {
                return;
            }

            RunnableScheduledFuture<?> future = (RunnableScheduledFuture<?>) poolExecutor.scheduleAtFixedRate(new Runnable() {
                private final DatagramPacket packet = new DatagramPacket(heartbeats, heartbeats.length, current);

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

    public void close(InetSocketAddress current) {
        synchronized (clientMapLock) {
            UDPReceiveItem item = clientMap.remove(current);
            if (item != null) {
                poolExecutor.remove(item.getFuture());
            }
        }
    }

    public void heartbeat(InetSocketAddress current) {
        synchronized (clientMapLock) {
            UDPReceiveItem receiveItem = clientMap.get(current);
            if (receiveItem != null) {
                receiveItem.setLastUpdateTime(System.currentTimeMillis());
            }
        }
    }

    public byte getCmd(ByteBuffer byteBuffer) {
        return byteBuffer.get(0);
    }

    public void getData(ByteBuffer byteBuffer, byte[] data) {
        int index = Byte.BYTES + Integer.BYTES + Short.BYTES + Short.BYTES;
        for (int i = 0; i < data.length; i++) {
            data[i] = byteBuffer.get(i + index);
        }
    }

    public void setAddress(ByteBuffer byteBuffer, String ip, int port) {
        byteBuffer.putInt(Byte.BYTES, IPUtil.IP4ToInt(ip));
        byteBuffer.putShort(Integer.BYTES + Byte.BYTES, (short) port);
    }

    public InetSocketAddress getAddress(ByteBuffer byteBuffer) {
        final String host = IPUtil.int2IP4(byteBuffer.getInt(Byte.BYTES));
        int port = byteBuffer.getShort(Integer.BYTES + Byte.BYTES) & IPUtil.U_SHORT;
        return new InetSocketAddress(host, port);
    }

    public int getLocalPort(ByteBuffer byteBuffer) {
        return byteBuffer.getShort(Integer.BYTES + Byte.BYTES + Short.BYTES) & IPUtil.U_SHORT;
    }

    public void putLocalPort(ByteBuffer byteBuffer, int localPort) {
        byteBuffer.putShort(Integer.BYTES + Byte.BYTES + Short.BYTES, (short) localPort);
    }
}
