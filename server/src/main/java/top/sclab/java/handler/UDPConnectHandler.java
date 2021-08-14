package top.sclab.java.handler;

import top.sclab.java.Constant;
import top.sclab.java.ServerConfig;
import top.sclab.java.service.ConnectHandler;
import top.sclab.java.service.MessageHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPConnectHandler implements ConnectHandler, Runnable {

    private ExecutorService threadPoolExecutor;

    private Set<InetSocketAddress> clientConnectSet;

    private MessageHandler messageProcessService;

    private DatagramSocket server;

    private DatagramPacket packet;

    private boolean initialized = false;

    private volatile boolean activated = false;

    @Override
    public void init() {

        if (ServerConfig.getUDPStartup() && !initialized && !activated) {

            int enableCount = ServerConfig.getEnableCount();
            int udpServerPort = ServerConfig.getUDPServerPort();
            try {
                server = new DatagramSocket(udpServerPort);
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }

            float loadFactor = 0.75f;
            int initialCapacity = (int) (enableCount / loadFactor) + 1;
            threadPoolExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

            if (clientConnectSet == null) {
                clientConnectSet = new HashSet<>(initialCapacity, loadFactor);
            }

            ServiceLoader<MessageHandler> consolePrintStreams = ServiceLoader.load(MessageHandler.class);
            Iterator<MessageHandler> iterator = consolePrintStreams.iterator();
            if (iterator.hasNext()) {
                messageProcessService = iterator.next();
            } else {
                messageProcessService = new UDPBaseMessageHandler();
            }
            messageProcessService.setUdpServer(server);
            messageProcessService.setUdpAddresses(clientConnectSet);

            byte[] buf = new byte[1024];
            if (packet == null) {
                packet = new DatagramPacket(buf, buf.length);
            }

            initialized = true;
        }
    }

    public boolean startup() {

        if (ServerConfig.getUDPStartup()) {

            if (activated) {
                throw new RuntimeException("处理器已正在运行!!!");
            }

            if (!initialized) {
                throw new RuntimeException("处理器未初始化!!!");
            }

            new Thread(this).start();

            System.out.printf("Server:%d 工作中...\n", server.getLocalPort());
            return true;
        }

        return false;
    }

    private static final byte[] close = new byte[]{Constant.close};
    private static final byte[] tooManyConnect = new byte[]{Constant.tooManyConnect};

    @Override
    public void run() {

        activated = true;
        ByteBuffer byteBuffer = ByteBuffer.wrap(packet.getData());
        while (activated) {

            try {
                server.receive(packet);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            byte[] data = new byte[packet.getLength()];
            byteBuffer.get(data, 0, data.length);
            System.out.printf("收到数据 %s -> %s\n", packet.getSocketAddress(), Arrays.toString(data));

            InetSocketAddress socketAddress = (InetSocketAddress) packet.getSocketAddress();
            if (!clientConnectSet.contains(socketAddress)) {

                // 超过最大连接数量时通知客户端选择其他连接
                if (clientConnectSet.size() >= ServerConfig.getEnableCount()) {
                    try {
                        server.send(new DatagramPacket(tooManyConnect, tooManyConnect.length, socketAddress));
                    } catch (IOException ignored) {
                    }
                    continue;
                }

                clientConnectSet.add(socketAddress);
            }

            threadPoolExecutor.submit(() -> messageProcessService.udpMessageProcess(socketAddress, data));
        }
    }

    @Override
    public void destroy() {

        messageProcessService.destroy();

        Iterator<InetSocketAddress> iterator = clientConnectSet.iterator();
        while (iterator.hasNext()) {
            InetSocketAddress address = iterator.next();
            try {
                server.send(new DatagramPacket(close, close.length, address));
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                iterator.remove();
            }
        }

        if (server != null) {
            try {
                server.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            server = null;
        }

        activated = initialized = false;
        System.out.println("UDP Server 关闭. 再见!");
    }
}
