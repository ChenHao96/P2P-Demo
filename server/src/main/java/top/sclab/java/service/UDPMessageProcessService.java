package top.sclab.java.service;

import top.sclab.java.ServerConfig;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.LinkedBlockingQueue;

public class UDPMessageProcessService implements MessageProcessService, Runnable {

    private final Thread[] sendThreads;

    private final DatagramSocket socket;

    private final InetSocketAddress socketAddress;

    private final LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();

    private volatile boolean activated;

    private final Process messageProcess;

    public UDPMessageProcessService(DatagramSocket socket, InetSocketAddress socketAddress) {

        this.activated = true;

        this.socket = Objects.requireNonNull(socket);
        this.socketAddress = Objects.requireNonNull(socketAddress);

        ServiceLoader<Process> processes = ServiceLoader.load(Process.class);
        Iterator<Process> iterator = processes.iterator();
        if (iterator.hasNext()) {
            this.messageProcess = iterator.next();
        } else {
            this.messageProcess = new BashMessageProcess();
        }

        int processors = ServerConfig.getMessageProcessors();
        this.sendThreads = new Thread[processors];
        for (int i = 0; i < processors; i++) {
            Thread thread = this.sendThreads[i] = new Thread(this);
            thread.start();
        }
    }

    @Override
    public void putMessage(byte[] data) {
        try {
            queue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (activated) {
            try {
                byte[] data = messageProcess.process(queue.take());
                if (data != null) {
                    socket.send(new DatagramPacket(data, data.length, socketAddress));
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }

        System.out.printf("%s Message Process stop.\n", socketAddress);
    }

    @Override
    public void destroyProcess() {
        activated = false;
        for (Thread sendThread : sendThreads) {
            sendThread.interrupt();
        }
    }
}
