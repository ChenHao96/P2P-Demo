package top.sclab.java.handler;

import top.sclab.java.Constant;
import top.sclab.java.ServerConfig;
import top.sclab.java.service.MessageProcessService;
import top.sclab.java.service.UDPMessageProcess;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class UDPConnectHandler implements HandlerLife, Runnable {

    private ScheduledThreadPoolExecutor poolExecutor;

    private ExecutorService threadPoolExecutor;

    private Set<InetSocketAddress> clientConnectSet;

    private MessageProcessService messageProcessService;

    private DatagramSocket server;

    private DatagramPacket packet;

    private boolean initialized = false;

    private volatile boolean activated = false;

    @Override
    public void init() throws SocketException {

        if (ServerConfig.getUDPStartup() && !initialized && !activated) {

            int enableCount = ServerConfig.getEnableCount();
            int udpServerPort = ServerConfig.getUDPServerPort();
            server = new DatagramSocket(udpServerPort);

            float loadFactor = 0.75f;
            int initialCapacity = (int) (enableCount / loadFactor) + 1;

            poolExecutor = new ScheduledThreadPoolExecutor(enableCount);
            threadPoolExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

            if (clientConnectSet == null) {
                clientConnectSet = new HashSet<>(initialCapacity, loadFactor);
            }

            ServiceLoader<MessageProcessService> consolePrintStreams = ServiceLoader.load(MessageProcessService.class);
            Iterator<MessageProcessService> iterator = consolePrintStreams.iterator();
            if (iterator.hasNext()) {
                messageProcessService = iterator.next();
            } else {
                messageProcessService = new UDPMessageProcess();
            }

            byte[] buf = new byte[1024];
            if (packet == null) {
                packet = new DatagramPacket(buf, buf.length);
            }

            initialized = true;
        }
    }

    @Override
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
    private static final byte[] heartbeat = new byte[]{Constant.heartbeat};
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

                // heartbeat 延时10秒执行 每30秒执行
                poolExecutor.scheduleAtFixedRate(() -> {
                    try {
                        server.send(new DatagramPacket(heartbeat, heartbeat.length, socketAddress));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }, 10, 30, TimeUnit.SECONDS);
            }

            threadPoolExecutor.submit(() -> messageProcessService.udpMessageProcess(server, clientConnectSet, data));
        }
    }

    @Override
    public void shutdown() {

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

        clientConnectSet.forEach(address -> {
            try {
                server.send(new DatagramPacket(close, close.length, address));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        clientConnectSet.clear();

        safeClose(server);
        server = null;

        activated = initialized = false;
        System.out.println("UDP Server 关闭. 再见!");
    }
}
