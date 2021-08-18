package top.sclab.java.handler;

import top.sclab.java.AddressUtil;
import top.sclab.java.Constant;
import top.sclab.java.ServerConfig;
import top.sclab.java.model.UDPReceiveItem;
import top.sclab.java.service.MessageHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class UDPBaseMessageHandler implements MessageHandler {

    private ScheduledThreadPoolExecutor poolExecutor;

    private Map<InetSocketAddress, UDPReceiveItem> clientMap;

    private DatagramSocket server;

    private List<Integer> clientTokens;

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

        if (clientTokens == null) {
            clientTokens = new ArrayList<>(enableCount);
        } else {
            clientTokens.clear();
        }
        for (int i = 0; i < enableCount; i++) {
            clientTokens.add(i + 1);
        }

        poolExecutor.scheduleAtFixedRate(() -> {

            final long currentTime = System.currentTimeMillis();
            Set<Map.Entry<InetSocketAddress, UDPReceiveItem>> entries = clientMap.entrySet();
            Iterator<Map.Entry<InetSocketAddress, UDPReceiveItem>> iterator = entries.iterator();
            while (iterator.hasNext()) {

                Map.Entry<InetSocketAddress, UDPReceiveItem> entry = iterator.next();
                UDPReceiveItem item = entry.getValue();
                long lastTime = item.getLastUpdateTime();
                if (Math.abs(currentTime - lastTime) >= TimeUnit.MINUTES.toMillis(1)) {

                    poolExecutor.remove(item.getFuture());
                    iterator.remove();
                    clientTokens.add(item.getToken());
                }
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void udpMessageProcess(InetSocketAddress current, byte[] data) {

        if (data != null && data.length > 0) {

            int offset = 0;
            int token = data[offset++] & AddressUtil.UBYTE;

            UDPReceiveItem udpReceiveItem = clientMap.get(current);
            if (udpReceiveItem.getToken() != token) {
                return;
            }

            switch (data[offset++]) {
                case Constant.heartbeat:
                    heartbeat(current, offset, data);
                    break;
                case Constant.register:
                    register(current, offset, data);
                    break;
                case Constant.broadcast:
                    broadcast(current, offset, data);
                    break;
                case Constant.forward:
                    forward(current, offset, data);
                    break;
                case Constant.other:
                    other(current, offset, data);
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

        Set<Map.Entry<InetSocketAddress, UDPReceiveItem>> entries = clientMap.entrySet();
        Iterator<Map.Entry<InetSocketAddress, UDPReceiveItem>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            InetSocketAddress address = iterator.next().getKey();
            try {
                server.send(new DatagramPacket(close, close.length, address));
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                iterator.remove();
            }
        }
    }

    public void heartbeat(InetSocketAddress current, final int offset, final byte[] data) {

        UDPReceiveItem receiveItem = clientMap.get(current);
        if (receiveItem != null) {

            receiveItem.setLastUpdateTime(System.currentTimeMillis());
        }
    }

    public void forward(InetSocketAddress current, final int offset, final byte[] data) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, data.length);
        final String host = AddressUtil.int2IP(byteBuffer.getInt());
        int port = byteBuffer.getShort() & AddressUtil.USHORT;
        InetSocketAddress address = InetSocketAddress.createUnresolved(host, port);

        byteBuffer.putInt(offset, AddressUtil.ipToInt(current.getHostString()));
        byteBuffer.putShort(offset + Integer.BYTES, (short) port);

        DatagramPacket packet = new DatagramPacket(data, data.length, address);
        try {

            server.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcast(InetSocketAddress current, final int offset, final byte[] data) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, data.length);
        byteBuffer.putInt(offset, AddressUtil.ipToInt(current.getHostString()));
        byteBuffer.putShort(offset + Integer.BYTES, (short) current.getPort());

        DatagramPacket packet = new DatagramPacket(data, data.length, current);
        clientMap.forEach((address, udpReceiveItem) -> {
            try {

                packet.setSocketAddress(address);
                server.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static final byte[] tooManyConnect = new byte[]{Constant.tooManyConnect};

    public synchronized void register(InetSocketAddress current, final int offset, final byte[] data) {

        if (clientTokens.size() == 0) {
            try {
                server.send(new DatagramPacket(tooManyConnect, tooManyConnect.length, current));
            } catch (IOException ignored) {}
            return;
        }

        final int token = clientTokens.remove(clientTokens.size() - 1);
        // heartbeat 延时10秒执行 每30秒执行
        RunnableScheduledFuture<?> future = (RunnableScheduledFuture<?>) poolExecutor.scheduleAtFixedRate(new Runnable() {

            private final byte[] heartbeat = new byte[]{(byte) (token & AddressUtil.UBYTE), Constant.heartbeat};
            private final DatagramPacket packet = new DatagramPacket(heartbeat, heartbeat.length, current);

            @Override
            public void run() {
                try {

                    server.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 30, TimeUnit.SECONDS);

        UDPReceiveItem udpReceiveItem = new UDPReceiveItem();
        udpReceiveItem.setLastUpdateTime(System.currentTimeMillis());
        udpReceiveItem.setFuture(future);
        udpReceiveItem.setToken(token);
        clientMap.put(current, udpReceiveItem);
    }

    public void other(InetSocketAddress current, final int offset, final byte[] data) {

    }
}
