package top.sclab.java;

import top.sclab.java.handler.UDPConnectHandler;
import top.sclab.java.service.ConnectHandler;

public class ServerBootstrap {

    public static void main(String[] args) {
        ConnectHandler udpHandler = new UDPConnectHandler();
        udpHandler.init();
        if (udpHandler.startup()) {
            Runtime.getRuntime().addShutdownHook(new Thread(udpHandler::destroy));
        }
    }
}
