package top.sclab.java;

import java.net.InetSocketAddress;
import java.net.URI;

public final class Constant {

    private static final String PREFIX = "connect::";

    public static final String CLIENT_PREFIX = PREFIX + "client::";

    public static final String CONNECT_CLOSE_VALUE = PREFIX + "close";

    public static final String CONNECT_PING_VALUE = PREFIX + "ping";

    public static final String CONNECT_PONG_VALUE = PREFIX + "pong";

    public static String formatAddress(URI address) {
        return String.format("%s%s", Constant.CLIENT_PREFIX, url(address.getHost(), address.getPort()));
    }

    public static URI parsingAddress(String address) {
        address = address.substring(Constant.CLIENT_PREFIX.length());
        return URI.create(address);
    }

    public static URI parsingAddress(InetSocketAddress addresses) {
        return URI.create(url(addresses.getHostString(), addresses.getPort()));
    }

    private static String url(String host, int port) {
        return String.format("udp://%s:%d", host, port);
    }

    private Constant() {
    }
}
