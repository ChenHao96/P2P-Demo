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
    private static final String UPD_STARTUP_PROPERTY_KEY = "server.upd.startup";

    private static final String TCP_PORT_PROPERTY_KEY = "server.tcp.port";
    private static final String TCP_VERSION_PROPERTY_KEY = "server.tcp.version";
    private static final String TCP_STARTUP_PROPERTY_KEY = "server.tcp.startup";

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

    private Integer udpPort;

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
                version = properties.getProperty(UDP_VERSION_PROPERTY_KEY, "ipv4");
            }

            return instance.udpVersion = version;
        }
    }

    private Boolean udpStartup;

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

            return instance.udpStartup = Boolean.getBoolean(startup);
        }
    }

    private Integer tcpPort;

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

    private String tcpVersion;

    public static String getTCPServerVersion() {

        if (null != instance.tcpVersion) {
            return instance.tcpVersion;
        }

        synchronized (instance) {
            if (null != instance.tcpVersion) {
                return instance.tcpVersion;
            }

            String version = System.getProperty(TCP_VERSION_PROPERTY_KEY);
            if (version == null) {
                version = properties.getProperty(TCP_VERSION_PROPERTY_KEY, "ipv4");
            }

            return instance.tcpVersion = version;
        }
    }

    private Boolean tcpStartup;

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

            return instance.tcpStartup = Boolean.getBoolean(startup);
        }
    }

    private ServerConfig() {
    }
}
