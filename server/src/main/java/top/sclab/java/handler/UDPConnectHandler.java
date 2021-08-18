package top.sclab.java.handler;

import top.sclab.java.ServerConfig;
import top.sclab.java.service.ConnectHandler;
import top.sclab.java.service.MessageHandler;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPConnectHandler implements ConnectHandler, Runnable {

    private ExecutorService threadPoolExecutor;

    private MessageHandler messageProcessService;

    private DatagramSocket server;

    private DatagramPacket packet;

    private boolean initialized = false;

    private volatile boolean activated = false;

    @Override
    public void init() {

        if (ServerConfig.getUDPStartup() && !initialized && !activated) {

            int udpServerPort = ServerConfig.getUDPServerPort();
            try {
                server = new DatagramSocket(udpServerPort);
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }

            threadPoolExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

            ServiceLoader<MessageHandler> consolePrintStreams = ServiceLoader.load(MessageHandler.class);
            Iterator<MessageHandler> iterator = consolePrintStreams.iterator();
            if (iterator.hasNext()) {
                messageProcessService = iterator.next();
            } else {
                messageProcessService = new UDPBaseMessageHandler();
            }
            messageProcessService.setUdpSocket(server);
            messageProcessService.init();

            final byte[] buf = new byte[ServerConfig.getBuffSize()];
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
            byteBuffer.clear();
            System.out.printf("收到数据 %s -> %s\n", packet.getSocketAddress(), Arrays.toString(data));

            InetSocketAddress socketAddress = (InetSocketAddress) packet.getSocketAddress();
            threadPoolExecutor.submit(() -> messageProcessService.udpMessageProcess(socketAddress, data));
        }
    }

    @Override
    public void destroy() {

        messageProcessService.destroy();

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
