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
            }
        }, 0, period, TimeUnit.MILLISECONDS);

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
            register(current, byteBuffer);

            switch (cmd) {
                case heartbeat:
                    heartbeat(current, byteBuffer);
                    break;
                case broadcast:
                    broadcast(current, byteBuffer);
                    break;
                case forward:
                    forward(current, byteBuffer);
                    break;
                case close:
                    close(current, byteBuffer);
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

    public void heartbeat(InetSocketAddress current, ByteBuffer byteBuffer) {
        synchronized (clientMapLock) {
            UDPReceiveItem receiveItem = clientMap.get(current);
            if (receiveItem != null) {
                receiveItem.setLastUpdateTime(System.currentTimeMillis());
            }
        }
    }

    public void forward(InetSocketAddress current, ByteBuffer byteBuffer) {

        final String host = AddressUtil.int2IP(byteBuffer.getInt());
        int port = byteBuffer.getShort() & AddressUtil.U_SHORT;
        InetSocketAddress address = InetSocketAddress.createUnresolved(host, port);

        synchronized (clientMapLock) {
            if (clientMap.containsKey(address)) {

                byteBuffer.putInt(AddressUtil.ipToInt(current.getHostString()));
                byteBuffer.putShort((short) port);
                try {
                    byte[] data = byteBuffer.array();
                    socket.send(new DatagramPacket(data, data.length, address));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void broadcast(InetSocketAddress current, ByteBuffer byteBuffer) {

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
                }
            });
        }
    }

    private static final byte[] heartbeatByte = new byte[]{heartbeat};

    public void register(InetSocketAddress current, ByteBuffer byteBuffer) {

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
                    }
                }
            }, 0, HeartbeatIntervalTime, TimeUnit.MILLISECONDS);

            UDPReceiveItem udpReceiveItem = new UDPReceiveItem();
            udpReceiveItem.setLastUpdateTime(System.currentTimeMillis());
            udpReceiveItem.setFuture(future);
            clientMap.put(current, udpReceiveItem);
        }
    }

    public void close(InetSocketAddress current, ByteBuffer byteBuffer) {
        synchronized (clientMapLock) {
            UDPReceiveItem item = clientMap.remove(current);
            if (item != null) {
                poolExecutor.remove(item.getFuture());
            }
        }
    }
}
