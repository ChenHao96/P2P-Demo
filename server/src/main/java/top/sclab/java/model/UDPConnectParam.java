package top.sclab.java.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "server.udp")
public class UDPConnectParam {

    private Integer serverPort;

    private Integer buffSize;

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public Integer getBuffSize() {
        return buffSize;
    }

    public void setBuffSize(Integer buffSize) {
        this.buffSize = buffSize;
    }
}
