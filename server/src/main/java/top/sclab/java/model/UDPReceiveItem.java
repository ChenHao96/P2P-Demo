package top.sclab.java.model;

import java.io.Serializable;
import java.util.concurrent.RunnableScheduledFuture;

public class UDPReceiveItem implements Serializable {

    private RunnableScheduledFuture<?> future;

    private long lastUpdateTime;

    private int token;

    public RunnableScheduledFuture<?> getFuture() {
        return future;
    }

    public void setFuture(RunnableScheduledFuture<?> future) {
        this.future = future;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public int getToken() {
        return token;
    }

    public void setToken(int token) {
        this.token = token;
    }
}
