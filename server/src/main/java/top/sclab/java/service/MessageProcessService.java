package top.sclab.java.service;

public interface MessageProcessService {

    void putMessage(byte[] data);

    void destroyProcess();

    interface Process {
        byte[] process(byte[] data);
    }
}
