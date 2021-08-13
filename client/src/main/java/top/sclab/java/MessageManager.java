package top.sclab.java;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.concurrent.*;

public final class MessageManager {

    private static final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();

    private static final MessageManager instance = new MessageManager();

    private static final ScheduledThreadPoolExecutor poolExecutor = new ScheduledThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors());

    private HeartbeatRunnable heartbeatRunnable;

    private MessageSendRunnable messageSendRunnable;

    private CountDownLatch countDownLatch;

    public static void setHeartbeatRunnable(HeartbeatRunnable heartbeatRunnable) {
        instance.heartbeatRunnable = heartbeatRunnable;
        poolExecutor.scheduleAtFixedRate(heartbeatRunnable, 5, 15, TimeUnit.SECONDS);
    }

    public static void setMessageSendRunnable(MessageSendRunnable messageSendRunnable) {
        instance.messageSendRunnable = messageSendRunnable;
    }

    public static void setCountDownLatch(CountDownLatch countDownLatch) {
        instance.countDownLatch = countDownLatch;
    }

    private RunnableScheduledFuture<?> runnableFuture;

    public static void addMessage(String message) {
        if (Constant.CONNECT_CLOSE_VALUE.equals(message)) {
            instance.countDownLatch.countDown();
        } else if (message.startsWith(Constant.CLIENT_PREFIX)) {

            URI uri = Constant.parsingAddress(message);
            SocketAddress socketAddress = new InetSocketAddress(uri.getHost(), uri.getPort());
            instance.heartbeatRunnable.setSocketAddress(socketAddress);
            instance.messageSendRunnable.setSocketAddress(socketAddress);

            String param = uri.getQuery();
            // 发送ping/pong消息(被连接端延时)
            int initialDelay = param == null ? 0 : 1000;

            instance.runnableFuture = (RunnableScheduledFuture<?>) poolExecutor.scheduleAtFixedRate(
                    () -> queue.add(Constant.CONNECT_PING_VALUE), initialDelay, 150, TimeUnit.MILLISECONDS);
        } else if (Constant.CONNECT_PING_VALUE.equals(message)) {
            // 响应通讯
            queue.add(Constant.CONNECT_PONG_VALUE);
        } else if (Constant.CONNECT_PONG_VALUE.equals(message)) {
            // 通讯正常后
            if (instance.runnableFuture != null) {
                poolExecutor.remove(instance.runnableFuture);
                instance.runnableFuture = null;
            }
        }
    }

    public static String popMessage() throws InterruptedException {
        return queue.take();
    }
}
