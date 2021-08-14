package top.sclab.java.handler;

public interface HandlerLife {

    boolean startup() throws Exception;

    void init() throws Exception;

    void shutdown() throws Exception;

    default void safeClose(AutoCloseable... closeables) {
        if (closeables != null && closeables.length > 0) {
            for (AutoCloseable closeable : closeables) {
                if (closeable != null) {
                    try {
                        closeable.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
