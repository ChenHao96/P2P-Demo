package top.sclab.java;

import jdk.nashorn.api.scripting.URLReader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public final class ServerConfig {

    private static final String CONFIG_FILE_PATH = "server.config.path";

    private static final String BUFF_SIZE_PROPERTY_KEY = "server.buff.size";

    private static final String UDP_PORT_PROPERTY_KEY = "server.upd.port";
    private static final String UPD_STARTUP_PROPERTY_KEY = "server.upd.startup";

    private static final String TCP_PORT_PROPERTY_KEY = "server.tcp.port";
    private static final String TCP_STARTUP_PROPERTY_KEY = "server.tcp.startup";

    private static final ServerConfig instance = new ServerConfig();

    private Integer udpPort = null;

    /**
     * 获取UDP服务的启动端口
     *
     * @return 服务端口
     */
    public static Integer getUDPServerPort() {

        if (instance.udpPort != null) {
            return instance.udpPort;
        }

        synchronized (instance) {
            if (instance.udpPort != null) {
                return instance.udpPort;
            }

            String port = System.getProperty(UDP_PORT_PROPERTY_KEY);
            if (port == null) {
                port = properties.getProperty(UDP_PORT_PROPERTY_KEY, "8880");
            }

            return instance.udpPort = Integer.parseInt(port);
        }
    }

    private Boolean udpStartup = null;

    /**
     * 获取UDP服务开启状态
     *
     * @return 服务开启状态 true:开启UDP服务 false:不开启
     */
    public static Boolean getUDPStartup() {

        if (null != instance.udpStartup) {
            return instance.udpStartup;
        }

        synchronized (instance) {
            if (null != instance.udpStartup) {
                return instance.udpStartup;
            }

            String startup = System.getProperty(UPD_STARTUP_PROPERTY_KEY);
            if (startup == null) {
                startup = properties.getProperty(UPD_STARTUP_PROPERTY_KEY, "true");
            }

            return instance.udpStartup = Boolean.parseBoolean(startup);
        }
    }

    private Integer tcpPort = null;

    /**
     * 获取TCP服务的启动端口
     *
     * @return 服务端口
     */
    public static Integer getTCPServerPort() {

        if (instance.tcpPort != null) {
            return instance.tcpPort;
        }

        synchronized (instance) {
            if (instance.tcpPort != null) {
                return instance.tcpPort;
            }

            String port = System.getProperty(TCP_PORT_PROPERTY_KEY);
            if (port == null) {
                port = properties.getProperty(TCP_PORT_PROPERTY_KEY, "8881");
            }

            return instance.tcpPort = Integer.parseInt(port);
        }
    }

    private Boolean tcpStartup = null;

    /**
     * 获取TCP服务开启状态
     *
     * @return 服务开启状态 true:开启TCP服务 false:不开启
     */
    public static Boolean getTCPStartup() {

        if (null != instance.tcpStartup) {
            return instance.tcpStartup;
        }

        synchronized (instance) {
            if (null != instance.tcpStartup) {
                return instance.tcpStartup;
            }

            String startup = System.getProperty(TCP_STARTUP_PROPERTY_KEY);
            if (startup == null) {
                startup = properties.getProperty(TCP_STARTUP_PROPERTY_KEY, "true");
            }

            return instance.tcpStartup = Boolean.parseBoolean(startup);
        }
    }

    private Integer buffSize = null;

    /**
     * 获取缓冲区长度
     *
     * @return 缓冲区长度
     */
    public static Integer getBuffSize() {

        if (instance.buffSize != null) {
            return instance.buffSize;
        }

        synchronized (instance) {
            if (instance.buffSize != null) {
                return instance.buffSize;
            }


            String buffSize = System.getProperty(BUFF_SIZE_PROPERTY_KEY);
            if (buffSize == null) {
                buffSize = properties.getProperty(BUFF_SIZE_PROPERTY_KEY, "50");
            }

            return instance.buffSize = Math.max(Integer.parseInt(buffSize), 50);
        }
    }

    private static final Properties properties;

    static {

        URL configUrl;
        String configPath = System.getProperty(CONFIG_FILE_PATH, "classpath:p2p_server_config.properties");
        if (configPath.startsWith("classpath")) {
            String path = configPath.split(":")[1];
            configUrl = ServerConfig.class.getResource("/" + path);
        } else if (configPath.startsWith("http")) {
            try {
                configUrl = new URL(configPath);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                configUrl = new File(configPath).toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        properties = new Properties();
        if (configUrl != null) {
            try {
                properties.load(new URLReader(configUrl));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private ServerConfig() {
    }
}
