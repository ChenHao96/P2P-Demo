package top.sclab.java.handler;

import top.sclab.java.AddressUtil;
import top.sclab.java.model.SocketReceiveItem;
import top.sclab.java.service.MessageHandler;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class BaseMessageHandler implements MessageHandler {

    protected ScheduledThreadPoolExecutor poolExecutor;

    protected Map<InetSocketAddress, SocketReceiveItem> clientMap;

    protected final Object clientMapLock = new Object();

    protected volatile boolean activated = false;

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
                Set<Map.Entry<InetSocketAddress, SocketReceiveItem>> entries = clientMap.entrySet();
                Iterator<Map.Entry<InetSocketAddress, SocketReceiveItem>> iterator = entries.iterator();
                while (iterator.hasNext()) {
                    Map.Entry<InetSocketAddress, SocketReceiveItem> entry = iterator.next();
                    SocketReceiveItem item = entry.getValue();
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
    public void messageProcess(InetSocketAddress current, byte[] data) {

        if (!activated) {
            return;
        }

        if (data != null && data.length > 0) {

            ByteBuffer byteBuffer = ByteBuffer.wrap(data);
            byte cmd = getCmd(byteBuffer);
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

    protected abstract void close(InetSocketAddress current, ByteBuffer byteBuffer);

    protected abstract void forward(InetSocketAddress current, ByteBuffer byteBuffer);

    protected abstract void broadcast(InetSocketAddress current, ByteBuffer byteBuffer);

    protected abstract void register(InetSocketAddress current, ByteBuffer byteBuffer);

    protected abstract void sendCloseMessageFromServer();

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

        sendCloseMessageFromServer();
    }

    protected static final int BLANK_PORT = 0;
    protected static final String BLANK_IP = "0.0.0.0";

    protected ByteBuffer createByteBuffer(byte cmd, String ip, int port, byte[] data) {

        // cmd
        int buffLength = Byte.BYTES;
        if (!BLANK_IP.equals(ip)) {
            // from ip
            buffLength += Integer.BYTES;
        }
        if (BLANK_PORT != port) {
            // from port
            buffLength += Short.BYTES;
        }
        if (data != null) {
            // data
            buffLength += data.length;
        }
        if (buffLength > Byte.BYTES) {
            // localPort
            buffLength += Short.BYTES;
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(buffLength);
        setCmd(byteBuffer, cmd);
        setAddress(byteBuffer, ip, port);
        putValue(byteBuffer, data);
        return byteBuffer;
    }

    protected void setCmd(ByteBuffer byteBuffer, byte cmd) {
        byteBuffer.put(0, cmd);
    }

    protected byte getCmd(ByteBuffer byteBuffer) {
        return byteBuffer.get(0);
    }

    protected void setAddress(ByteBuffer byteBuffer, String ip, int port) {
        if (!BLANK_IP.equals(ip)) {
            byteBuffer.putInt(Byte.BYTES, AddressUtil.ipToInt(ip));
        }
        if (port != BLANK_PORT) {
            byteBuffer.putShort(Integer.BYTES + Byte.BYTES, (short) port);
        }
    }

    protected InetSocketAddress getAddress(ByteBuffer byteBuffer) {
        final String host = AddressUtil.int2IP(byteBuffer.getInt(Byte.BYTES));
        int port = byteBuffer.getShort(Integer.BYTES + Byte.BYTES) & AddressUtil.U_SHORT;
        return new InetSocketAddress(host, port);
    }

    protected int getLocalPort(ByteBuffer byteBuffer) {
        return byteBuffer.getShort(Integer.BYTES + Byte.BYTES + Short.BYTES) & AddressUtil.U_SHORT;
    }

    protected void putLocalPort(ByteBuffer byteBuffer, int localPort) {
        byteBuffer.putShort(Integer.BYTES + Byte.BYTES + Short.BYTES, (short) localPort);
    }

    protected void getValue(ByteBuffer byteBuffer, byte[] value) {
        if (value != null) {
            int index = Byte.BYTES + Integer.BYTES + Short.BYTES + Short.BYTES;
            for (int i = 0; i < value.length; i++) {
                value[i] = byteBuffer.get(index + i);
            }
        }
    }

    protected void putValue(ByteBuffer byteBuffer, byte[] value) {
        if (value != null && value.length > 0) {
            int index = Byte.BYTES + Integer.BYTES + Short.BYTES + Short.BYTES;
            for (int i = 0; i < value.length; i++) {
                byteBuffer.put(index + i, value[i]);
            }
        }
    }

    protected void heartbeat(InetSocketAddress current, ByteBuffer byteBuffer) {
        synchronized (clientMapLock) {
            SocketReceiveItem receiveItem = clientMap.get(current);
            if (receiveItem != null) {
                receiveItem.setLastUpdateTime(System.currentTimeMillis());
            }
        }
    }
}
