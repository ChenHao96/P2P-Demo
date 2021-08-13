package top.sclab.java;

import java.net.URI;

public final class Constant {

    private static final String PREFIX = "connect::";

    public static final String CLIENT_PREFIX = PREFIX + "client::";

    public static final String CONNECT_CLOSE_VALUE = PREFIX + "close";

    public static final String CONNECT_PING_VALUE = PREFIX + "ping";

    public static final String CONNECT_PONG_VALUE = PREFIX + "pong";

    public static String formatAddress(URI address) {
        return String.format("%s%s:%d", Constant.CLIENT_PREFIX, address.getHost(), address.getPort());
    }

    public static String[] ParsingAddress(String address) {

    }

    private Constant() {
    }
}
