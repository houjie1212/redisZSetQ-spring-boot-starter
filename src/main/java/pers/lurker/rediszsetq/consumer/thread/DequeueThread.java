package pers.lurker.rediszsetq.consumer.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DequeueThread extends Thread {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Runnable callback;

    public DequeueThread(Runnable callback) {
        this.callback = callback;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                callback.run();
            } catch (Throwable t) {
                //log.error("Exception while handling next queue item.", t);
            }
        }
        log.info("thread[{}] stopped", this.getName());
    }

}
