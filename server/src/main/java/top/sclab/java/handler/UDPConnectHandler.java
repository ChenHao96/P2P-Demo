package top.sclab.java.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.sclab.java.model.UDPConnectParam;
import top.sclab.java.service.HandlerDestroy;
import top.sclab.java.service.HandlerInit;
import top.sclab.java.service.MessageHandler;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class UDPConnectHandler implements Runnable, HandlerInit, HandlerDestroy {

    private static final Logger LOGGER = LoggerFactory.getLogger(UDPConnectHandler.class);

    private ExecutorService threadPoolExecutor;

    private DatagramSocket server;

    private boolean initialized = false;

    private volatile boolean activated = false;

    @Autowired
    private UDPConnectParam udpConnectParam;

    @Autowired
    private MessageHandler messageProcessService;

    @Override
    public void init() {

        if (!initialized && !activated) {

            int udpServerPort = udpConnectParam.getServerPort();
            try {
                server = new DatagramSocket(udpServerPort);
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }

            threadPoolExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

            messageProcessService.setUdpSocket(server);
            messageProcessService.init();

            initialized = true;
        }
    }

    public boolean startup() {

        if (activated) {
            LOGGER.warn("处理器已正在运行!!!");
            return false;
        }

        if (!initialized) {
            LOGGER.warn("处理器未初始化!!!");
            return false;
        }

        threadPoolExecutor.submit(this);

        LOGGER.info("Server:{} 工作中...", server.getLocalPort());
        return true;
    }

    @Override
    public void run() {

        activated = true;

        final byte[] buf = new byte[udpConnectParam.getBuffSize()];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        ByteBuffer byteBuffer = ByteBuffer.wrap(buf);

        while (activated) {

            try {
                server.receive(packet);
            } catch (Exception e) {
                LOGGER.error("udp socket loop receive.", e);
                continue;
            }

            byte[] data = new byte[packet.getLength()];
            byteBuffer.get(data, 0, data.length);
            byteBuffer.clear();

            InetSocketAddress socketAddress = (InetSocketAddress) packet.getSocketAddress();
            LOGGER.info("收到数据 {} -> {}", socketAddress, Arrays.toString(data));

            threadPoolExecutor.submit(() -> messageProcessService.udpMessageProcess(socketAddress, data));
        }
    }

    @Override
    public void destroy() {

        activated = initialized = false;

        if (messageProcessService != null) {
            messageProcessService.destroy();
        }

        if (server != null) {
            try {
                server.close();
            } catch (Exception e) {
                LOGGER.error("udp socket close.", e);
            } finally {
                server = null;
            }
        }

        if (threadPoolExecutor != null) {
            threadPoolExecutor.shutdown();
            try {
                if (!threadPoolExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    threadPoolExecutor.shutdownNow();
                    if (!threadPoolExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                        LOGGER.error("线程池无法终止");
                    }
                }
            } catch (InterruptedException ie) {
                threadPoolExecutor.shutdownNow();
            } finally {
                threadPoolExecutor = null;
            }
        }

        LOGGER.info("UDP Server 关闭");
    }
}
