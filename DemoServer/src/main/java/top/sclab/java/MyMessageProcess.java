package top.sclab.java;

import top.sclab.java.service.UDPMessageProcess;

public class MyMessageProcess extends UDPMessageProcess {

    @Override
    public byte[] process(byte[] data) {
        return data;
    }
}
