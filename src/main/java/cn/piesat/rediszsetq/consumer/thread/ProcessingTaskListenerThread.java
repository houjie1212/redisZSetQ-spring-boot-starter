package cn.piesat.rediszsetq.consumer.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ProcessingTaskListenerThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(ProcessingTaskListenerThread.class);

    private final Runnable callback;

    public ProcessingTaskListenerThread(Runnable callback) {
        this.callback = callback;
    }

    @Override
    public void run() {
        while (true) {
            callback.run();
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                log.error("InterruptedException while listenering ProcessingTaskListenerThread", e);
            }
        }
    }
}
