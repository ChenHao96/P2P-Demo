package top.sclab.java;

import top.sclab.java.handler.UDPBaseMessageHandler;
import top.sclab.java.service.HandlerDestroy;
import top.sclab.java.service.HandlerInit;
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

public class UDPClientHandler implements HandlerInit, HandlerDestroy, Runnable {

    private ExecutorService threadPoolExecutor;

    private MessageHandler messageProcessService;

    private boolean initialized = false;

    private volatile boolean activated = false;

    private DatagramSocket socket;

    private DatagramPacket serverPacket;

    @Override
    public void init() {

        if (!initialized && !activated) {

            try {
                socket = new DatagramSocket();
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

            messageProcessService.setUdpSocket(socket);
            messageProcessService.init();

            String port = System.getProperty("port", "8880");
            String host = System.getProperty("host", "localhost");
            InetSocketAddress serverAddress = new InetSocketAddress(host, Integer.parseInt(port));
            messageProcessService.register(serverAddress, 1, new byte[]{Constant.heartbeat});

            final byte[] buff = new byte[50];
            serverPacket = new DatagramPacket(buff, buff.length, serverAddress);

            initialized = true;
        }
    }

    @Override
    public void run() {

        if (activated) {
            throw new RuntimeException("处理器已正在运行!!!");
        }

        if (!initialized) {
            throw new RuntimeException("处理器未初始化!!!");
        }

        activated = true;

        ByteBuffer byteBuffer = ByteBuffer.wrap(serverPacket.getData());
        while (activated) {

            try {
                socket.receive(serverPacket);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            byte[] data = new byte[serverPacket.getLength()];
            byteBuffer.get(data, 0, data.length);
            byteBuffer.clear();

            InetSocketAddress socketAddress = (InetSocketAddress) serverPacket.getSocketAddress();
            System.out.printf("收到数据 %s -> %s\n", socketAddress, Arrays.toString(data));
            threadPoolExecutor.submit(() -> messageProcessService.udpMessageProcess(socketAddress, data));
        }
    }

    @Override
    public void destroy() {

        messageProcessService.destroy();

        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            socket = null;
        }

        activated = initialized = false;
        System.out.println("UDP Server 关闭. 再见!");
    }
}
