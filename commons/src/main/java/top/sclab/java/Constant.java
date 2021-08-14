package top.sclab.java;

import java.net.InetSocketAddress;
import java.net.URI;

public final class Constant {

    public static final byte[] heartbeat = "hb".getBytes();

    private static final String PREFIX = "connect::";

    public static final String CLIENT_PREFIX = PREFIX + "client::";

    public static final String CONNECT_CLOSE_VALUE = PREFIX + "close";

    public static final String CONNECT_PING_VALUE = PREFIX + "ping";

    public static final String CONNECT_PONG_VALUE = PREFIX + "pong";

    public static String formatAddress(URI address) {
        return String.format("%s%s", Constant.CLIENT_PREFIX, address.toString());
    }

    public static URI parsingAddress(String address) {
        address = address.substring(Constant.CLIENT_PREFIX.length());
        return URI.create(address);
    }

    public static URI parsingAddress(InetSocketAddress addresses, String param) {
        return URI.create(url(addresses.getHostString(), addresses.getPort(), param));
    }

    private static String url(String host, int port, String param) {
        return String.format("udp://%s:%d%s", host, port, param);
    }

    public static byte[] clientCloseMessage() {
        return "close".getBytes();
    }

    public static byte[] tooManyConnectMessage() {
        return "tooManyConnect".getBytes();
    }

    private Constant() {
    }
}
