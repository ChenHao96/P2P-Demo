package top.sclab.java;

import jdk.nashorn.api.scripting.URLReader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public final class ServerConfig {

    private static final String CONFIG_FILE_PATH = "server.config.path";
    private static final String UDP_PORT_PROPERTY_KEY = "server.upd.port";
    private static final String UDP_VERSION_PROPERTY_KEY = "server.upd.version";
    private static final String TCP_PORT_PROPERTY_KEY = "server.tcp.port";
    private static final String TCP_VERSION_PROPERTY_KEY = "server.tcp.version";

    private static final Properties properties;

    static {

        URL configUrl;
        String configPath = System.getProperty(CONFIG_FILE_PATH, "classpath:p2p_server_config.properties");
        if (configPath.startsWith("classpath")) {
            String path = configPath.split(":")[1];
            configUrl = ServerBootstrap.class.getResource("/" + path);
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

    private static final ServerConfig instance = new ServerConfig();

    private Long udpPort;

    public static Integer getUDPServerPort() {

        if (instance.udpPort != null) {
            return instance.udpPort.intValue();
        }

        synchronized (instance) {
            if (instance.udpPort != null) {
                return instance.udpPort.intValue();
            }

            String port = System.getProperty(UDP_PORT_PROPERTY_KEY);
            if (port == null) {

                String portPro = properties.getProperty(UDP_PORT_PROPERTY_KEY, "8880");
                instance.udpPort = Long.parseLong(portPro);
            } else {

                instance.udpPort = Long.parseLong(port);
            }

            return instance.udpPort.intValue();
        }
    }

    private String udpVersion;

    public static String getUDPServerVersion() {

        if (null != instance.udpVersion) {
            return instance.udpVersion;
        }

        synchronized (instance) {
            if (null != instance.udpVersion) {
                return instance.udpVersion;
            }

            String version = System.getProperty(UDP_VERSION_PROPERTY_KEY);
            if (version == null) {

                instance.udpVersion = properties.getProperty(UDP_VERSION_PROPERTY_KEY, "ipv4");
            } else {

                instance.udpVersion = version;
            }

            return instance.udpVersion;
        }
    }

    private Long tcpPort;

    public static Integer getTCPServerPort() {

        if (instance.tcpPort != null) {
            return instance.tcpPort.intValue();
        }

        synchronized (instance) {
            if (instance.tcpPort != null) {
                return instance.tcpPort.intValue();
            }

            String port = System.getProperty(TCP_PORT_PROPERTY_KEY);
            if (port == null) {

                String portPro = properties.getProperty(TCP_PORT_PROPERTY_KEY, "8881");
                instance.tcpPort = Long.parseLong(portPro);
            } else {

                instance.tcpPort = Long.parseLong(port);
            }

            return instance.tcpPort.intValue();
        }
    }

    private String tcpVersion;

    public static String getTCPServerVersion() {

        if (null != instance.tcpVersion) {
            return instance.udpVersion;
        }

        synchronized (instance) {
            if (null != instance.tcpVersion) {
                return instance.udpVersion;
            }

            String version = System.getProperty(TCP_VERSION_PROPERTY_KEY);
            if (version == null) {

                instance.tcpVersion = properties.getProperty(TCP_VERSION_PROPERTY_KEY, "ipv4");
            } else {

                instance.tcpVersion = version;
            }

            return instance.tcpVersion;
        }
    }

    private ServerConfig() {
    }
}
