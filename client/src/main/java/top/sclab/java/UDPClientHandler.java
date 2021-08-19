package top.sclab.java;

import top.sclab.java.service.HandlerDestroy;
import top.sclab.java.service.HandlerInit;
import top.sclab.java.service.MessageHandler;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPClientHandler implements HandlerInit, HandlerDestroy, Runnable {

    private ExecutorService threadPoolExecutor;

    private MessageHandler messageProcessService;

    private boolean initialized = false;

    private volatile boolean activated = false;

    private final DatagramSocket socket;

    private final InetSocketAddress serverAddress;

    public UDPClientHandler(DatagramSocket socket, InetSocketAddress serverAddress) {
        this.socket = socket;
        this.serverAddress = serverAddress;
    }

    public void setMessageProcessService(MessageHandler messageProcessService) {
        this.messageProcessService = messageProcessService;
    }

    @Override
    public void init() {

        if (!initialized && !activated) {

            if (messageProcessService == null) {
                throw new RuntimeException("");
            }

            threadPoolExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
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

        final byte[] buff = new byte[50];
        DatagramPacket serverPacket = new DatagramPacket(buff, buff.length, serverAddress);
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
        activated = initialized = false;
        System.out.println("UDP Client 关闭.");
    }
}
