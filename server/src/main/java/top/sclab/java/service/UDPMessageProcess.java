package top.sclab.java.service;

import top.sclab.java.Constant;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Set;

public class UDPMessageProcess implements MessageProcessService {

    @Override
    public void udpMessageProcess(DatagramSocket server, Set<InetSocketAddress> addresses, byte[] data) {
        if (data != null && data.length > 0) {

            ByteBuffer byteBuffer = ByteBuffer.wrap(data);
            byte tmp = byteBuffer.get();

            switch (tmp) {
                case Constant.heartbeat: {
                    // 刷连接时间
                    break;
                }

                case Constant.connect: {
                    // TODO: 发起连接
                    break;
                }

                case Constant.broadcast: {
                    break;
                }

                case Constant.forward: {
                    break;
                }
            }
        }
    }
}
