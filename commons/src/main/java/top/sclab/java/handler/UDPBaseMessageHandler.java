package top.sclab.java.handler;

import top.sclab.java.AddressUtil;
import top.sclab.java.Constant;
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

    private DatagramSocket socket;

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

            int offset = 0;
            byte cmd = data[offset++];
            register(current, offset, data);

            switch (cmd) {
                case Constant.heartbeat:
                    heartbeat(current, offset, data);
                    break;
                case Constant.broadcast:
                    broadcast(current, offset, data);
                    break;
                case Constant.forward:
                    forward(current, offset, data);
                    break;
                case Constant.close:
                    close(current, offset, data);
                    break;
            }
        }
    }

    private static final byte[] close = new byte[]{Constant.close};

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
                packet = new DatagramPacket(close, close.length, address);
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

    @Override
    public void heartbeat(InetSocketAddress current, final int offset, final byte[] data) {
        synchronized (clientMapLock) {
            UDPReceiveItem receiveItem = clientMap.get(current);
            if (receiveItem != null) {
                receiveItem.setLastUpdateTime(System.currentTimeMillis());
            }
        }
    }

    @Override
    public void forward(InetSocketAddress current, final int offset, final byte[] data) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, data.length);
        final String host = AddressUtil.int2IP(byteBuffer.getInt());
        int port = byteBuffer.getShort() & AddressUtil.U_SHORT;
        InetSocketAddress address = InetSocketAddress.createUnresolved(host, port);

        synchronized (clientMapLock) {
            if (clientMap.containsKey(address)) {

                byteBuffer.putInt(offset, AddressUtil.ipToInt(current.getHostString()));
                byteBuffer.putShort(offset + Integer.BYTES, (short) port);
                try {

                    socket.send(new DatagramPacket(data, data.length, address));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void broadcast(InetSocketAddress current, final int offset, final byte[] data) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, data.length);
        byteBuffer.putInt(offset, AddressUtil.ipToInt(current.getHostString()));
        byteBuffer.putShort(offset + Integer.BYTES, (short) current.getPort());

        final DatagramPacket packet = new DatagramPacket(data, data.length, current);
        synchronized (clientMapLock) {
            clientMap.forEach((address, udpReceiveItem) -> {
                try {

                    packet.setSocketAddress(address);
                    socket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static final byte[] heartbeat = new byte[]{Constant.heartbeat};

    @Override
    public void register(InetSocketAddress current, final int offset, final byte[] data) {

        if (clientMap.containsKey(current)) {
            return;
        }

        synchronized (clientMapLock) {
            if (clientMap.containsKey(current)) {
                return;
            }

            RunnableScheduledFuture<?> future = (RunnableScheduledFuture<?>) poolExecutor.scheduleAtFixedRate(new Runnable() {
                private final DatagramPacket packet = new DatagramPacket(heartbeat, heartbeat.length, current);

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

    @Override
    public void close(InetSocketAddress current, final int offset, final byte[] data) {
        synchronized (clientMapLock) {
            UDPReceiveItem item = clientMap.remove(current);
            if (item != null) {
                poolExecutor.remove(item.getFuture());
            }
        }
    }
}
