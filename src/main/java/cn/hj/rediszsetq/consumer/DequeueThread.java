package cn.hj.rediszsetq.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DequeueThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(DequeueThread.class);

    private boolean stopRequested = false;
    private final Runnable callback;

    public DequeueThread(Runnable callback) {
        this.callback = callback;
    }

    @Override
    public void run() {
        while (!stopRequested && !isInterrupted()) {
            try {
                callback.run();
            } catch (Throwable t) {
                log.error("Exception while handling next queue item.", t);
            }
        }
    }
}
