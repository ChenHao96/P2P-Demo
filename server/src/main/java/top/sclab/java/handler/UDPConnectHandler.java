package top.sclab.java.handler;

import top.sclab.java.Constant;
import top.sclab.java.ServerConfig;
import top.sclab.java.service.MessageProcessService;
import top.sclab.java.service.UDPMessageProcessService;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class UDPConnectHandler implements HandlerLife, Runnable {

    private ScheduledThreadPoolExecutor poolExecutor;

    private Map<InetSocketAddress, MessageProcessService> clientConnectMap;

    private DatagramSocket server;

    private DatagramPacket packet;

    private boolean initialized = false;

    private volatile boolean activated = false;

    @Override
    public void init() throws SocketException {

        if (ServerConfig.getUDPStartup() && !initialized && !activated) {

            int enableCount = ServerConfig.getEnableCount();
            int udpServerPort = ServerConfig.getUDPServerPort();

            float loadFactor = 0.75f;
            int initialCapacity = (int) (enableCount / loadFactor) + 1;

            poolExecutor = new ScheduledThreadPoolExecutor(enableCount);
            if (clientConnectMap == null) {
                clientConnectMap = new HashMap<>(initialCapacity, loadFactor);
            }

            server = new DatagramSocket(udpServerPort);

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
            MessageProcessService service = clientConnectMap.get(socketAddress);
            if (null == service) {

                // 超过最大连接数量时通知客户端选择其他连接
                if (clientConnectMap.size() >= ServerConfig.getEnableCount()) {
                    byte[] msg = Constant.tooManyConnectMessage();
                    try {
                        server.send(new DatagramPacket(msg, msg.length, socketAddress));
                    } catch (IOException ignored) {
                    }
                    continue;
                }

                service = new UDPMessageProcessService(server, socketAddress);
                clientConnectMap.put(socketAddress, service);

                // heartbeat 延时10秒执行 每30秒执行
                MessageProcessService finalService = service;
                poolExecutor.scheduleAtFixedRate(() -> finalService.putMessage(Constant.heartbeat)
                        , 10, 30, TimeUnit.SECONDS);
            }

            service.putMessage(data);
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

        clientConnectMap.forEach((address, messageService) -> {
            messageService.putMessage(Constant.clientCloseMessage());
            messageService.destroyProcess();
        });
        clientConnectMap.clear();

        safeClose(server);
        server = null;

        activated = initialized = false;
        System.out.println("UDP Server 关闭. 再见!");
    }
}
