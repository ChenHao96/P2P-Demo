package top.sclab.java;

import top.sclab.java.service.BashMessageProcess;

public class MyMessageProcess extends BashMessageProcess {

    @Override
    public byte[] process(byte[] data) {
        return data;
    }
}
