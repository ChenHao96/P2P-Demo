package top.sclab.java;

public final class AddressUtil {

    public static final int UBYTE = 255;
    public static final int USHORT = 65535;

    public static int ipToInt(String ipStr) {

        String[] ip = ipStr.split("\\.");
        int result = 0;

        result += Integer.parseInt(ip[0]);
        result = result << 8;

        result += Integer.parseInt(ip[1]);
        result = result << 8;

        result += Integer.parseInt(ip[2]);
        result = result << 8;

        result += Integer.parseInt(ip[3]);
        return result;
    }

    public static String int2IP(int ip) {

        StringBuilder result = new StringBuilder(15);

        result.insert(0, ip & UBYTE);
        result.insert(0, ".");
        ip = ip >>> 8;

        result.insert(0, ip & UBYTE);
        result.insert(0, ".");
        ip = ip >>> 8;

        result.insert(0, ip & UBYTE);
        result.insert(0, ".");
        ip = ip >>> 8;

        result.insert(0, ip & UBYTE);
        return result.toString();
    }

    private AddressUtil() {
    }
}
