package top.sclab.java;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MessageManager {

    private static final String PREFIX = "connect::";

    public static final String CONNECT_CLOSE_VALUE = PREFIX + "close";

    public static final String CONNECT_CLIENT_VALUE = PREFIX + "client::";

    public static final String CONNECT_PING_VALUE = PREFIX + "ping";

    public static final String CONNECT_PONG_VALUE = PREFIX + "pong";

    private static final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();

    private static final MessageManager instance = new MessageManager();

    private HeartbeatRunnable heartbeatRunnable;

    private MessageReceiveRunnable messageReceiveRunnable;

    private MessageSendRunnable messageSendRunnable;

    public static void setHeartbeatRunnable(HeartbeatRunnable heartbeatRunnable) {
        instance.heartbeatRunnable = heartbeatRunnable;
    }

    public static void setMessageReceiveRunnable(MessageReceiveRunnable messageReceiveRunnable) {
        instance.messageReceiveRunnable = messageReceiveRunnable;
    }

    public static void setMessageSendRunnable(MessageSendRunnable messageSendRunnable) {
        instance.messageSendRunnable = messageSendRunnable;
    }

    private static final AtomicBoolean doLoop = new AtomicBoolean(true);

    public static void addMessage(String message) {
        if ("h".equals(message)) {
            queue.add("b");
        } else if (message.startsWith(CONNECT_CLIENT_VALUE)) {
            String[] addresses = message.substring(CONNECT_CLIENT_VALUE.length()).split(",");

            SocketAddress socketAddress = new InetSocketAddress(addresses[0], Integer.parseInt(addresses[1]));
            instance.heartbeatRunnable.setSocketAddress(socketAddress);
            instance.messageSendRunnable.setSocketAddress(socketAddress);

            new Thread(() -> {
                while (doLoop.get()) {
                    queue.add(CONNECT_PING_VALUE);
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else if (CONNECT_PING_VALUE.equals(message)) {
            // TODO: 读取收到包的地址
            // TODO: 心跳，发送地址修改
            queue.add(CONNECT_PONG_VALUE);
        } else if (CONNECT_PONG_VALUE.equals(message)) {
            doLoop.set(false);
        }
    }

    public static String popMessage() throws InterruptedException {
        return queue.take();
    }
}
