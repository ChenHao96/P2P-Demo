package top.sclab.java;

import top.sclab.java.handler.UDPConnectHandler;

public class ServerBootstrap {

    public static void main(String[] args) throws Exception {

        Console.load();

        UDPConnectHandler udpHandler = new UDPConnectHandler();
        udpHandler.init();
        if (udpHandler.startup()) {
            Runtime.getRuntime().addShutdownHook(new Thread(udpHandler::shutdown));
        }
    }
}
