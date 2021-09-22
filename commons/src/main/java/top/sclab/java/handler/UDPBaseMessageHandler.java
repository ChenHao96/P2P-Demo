package top.sclab.java.handler;

import top.sclab.java.model.SocketReceiveItem;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;

public class UDPBaseMessageHandler extends BaseMessageHandler {

    protected DatagramSocket serverSocket;

    @Override
    public void setUDPSocket(DatagramSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    protected void sendCloseMessageFromServer() {

        ByteBuffer byteBuffer = createByteBuffer(close, BLANK_IP, BLANK_PORT, null);
        byte[] data = byteBuffer.array();

        Set<Map.Entry<InetSocketAddress, SocketReceiveItem>> entries = clientMap.entrySet();
        Iterator<Map.Entry<InetSocketAddress, SocketReceiveItem>> iterator = entries.iterator();

        DatagramPacket packet = null;
        while (iterator.hasNext()) {

            InetSocketAddress address = iterator.next().getKey();
            if (packet == null) {
                packet = new DatagramPacket(data, data.length, address);
            } else {
                packet.setSocketAddress(address);
            }

            try {
                serverSocket.send(packet);
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
                    serverSocket.send(new DatagramPacket(data, data.length, address));
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
                    serverSocket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.printf("broadcast %s -> %s\n", current, address);
                }
            });
        }
    }

    public void register(InetSocketAddress current, ByteBuffer byteBuffer) {

        if (clientMap.containsKey(current)) {
            return;
        }

        synchronized (clientMapLock) {
            if (clientMap.containsKey(current)) {
                return;
            }

            byteBuffer = createByteBuffer(heartbeat, BLANK_IP, BLANK_PORT, null);
            byte[] data = byteBuffer.array();
            RunnableScheduledFuture<?> future = (RunnableScheduledFuture<?>) poolExecutor.scheduleAtFixedRate(new Runnable() {
                private final DatagramPacket packet = new DatagramPacket(data, data.length, current);

                @Override
                public void run() {
                    try {
                        serverSocket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        System.out.printf("发送心跳 -> %s\n", packet.getSocketAddress());
                    }
                }
            }, 0, HeartbeatIntervalTime, TimeUnit.MILLISECONDS);

            SocketReceiveItem udpReceiveItem = new SocketReceiveItem();
            udpReceiveItem.setLastUpdateTime(System.currentTimeMillis());
            udpReceiveItem.setFuture(future);
            clientMap.put(current, udpReceiveItem);
        }
    }

    public void close(InetSocketAddress current, ByteBuffer byteBuffer) {
        synchronized (clientMapLock) {
            SocketReceiveItem item = clientMap.remove(current);
            if (item != null) {
                poolExecutor.remove(item.getFuture());
            }
        }
    }
}
