package top.sclab.java.handler;

import top.sclab.java.Constant;
import top.sclab.java.ServerConfig;
import top.sclab.java.service.MessageHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class UDPBaseMessageHandler implements MessageHandler {

    private ScheduledThreadPoolExecutor poolExecutor;

    private Set<InetSocketAddress> addresses;

    private DatagramSocket server;

    @Override
    public void setUdpAddresses(Set<InetSocketAddress> addresses) {
        this.addresses = addresses;
    }

    @Override
    public void setUdpServer(DatagramSocket server) {
        this.server = server;
    }

    @Override
    public void init() {
        int enableCount = ServerConfig.getEnableCount();
        poolExecutor = new ScheduledThreadPoolExecutor(enableCount + 1);
        poolExecutor.scheduleAtFixedRate(() -> {
            //
        }, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void udpMessageProcess(InetSocketAddress current, byte[] data) {
        if (data != null && data.length > 0) {
            switch (data[0]) {
                case Constant.heartbeat:
                    heartbeat(current, data);
                    break;
                case Constant.connect:
                    connect(current, data);
                    break;
                case Constant.broadcast:
                    broadcast(current, data);
                    break;
                case Constant.forward:
                    forward(current, data);
                    break;
            }
        }
    }

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
    }

    public void heartbeat(InetSocketAddress current, byte[] data) {
        // TODO: 刷连接时间
    }

    public void forward(InetSocketAddress current, byte[] data) {

    }

    public void broadcast(InetSocketAddress current, byte[] data) {
        addresses.forEach(address -> {

        });
    }

    public void connect(InetSocketAddress current, byte[] data) {
        // TODO: 发起连接

        // heartbeat 延时10秒执行 每30秒执行
        RunnableScheduledFuture<?> future = (RunnableScheduledFuture<?>) poolExecutor.scheduleAtFixedRate(new Runnable() {
            private final byte[] heartbeat = new byte[]{Constant.heartbeat};
            private final DatagramPacket packet = new DatagramPacket(heartbeat, heartbeat.length, current);

            @Override
            public void run() {
                try {
                    server.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 10, 30, TimeUnit.SECONDS);
    }
}
