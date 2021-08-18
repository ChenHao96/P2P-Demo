package top.sclab.java;

public class ClientBootstrap {

    public static void main(String[] args) {
        UDPClientHandler udpHandler = new UDPClientHandler();
        udpHandler.init();
        new Thread(udpHandler).start();
        Runtime.getRuntime().addShutdownHook(new Thread(udpHandler::destroy));
    }
}
