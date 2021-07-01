package cn.hj.rediszsetq.consumer.strategy;

import cn.hj.rediszsetq.consumer.DequeueThread;
import cn.hj.rediszsetq.consumer.MessageListener;
import cn.hj.rediszsetq.model.Message;
import cn.hj.rediszsetq.persistence.RedisZSetQOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

public class SingleThreadStrategy implements ThreadStrategy {

    private static final Logger log = LoggerFactory.getLogger(SingleThreadStrategy.class);

    private final int concurrency;

    @Autowired
    private RedisZSetQOps redisZSetQOps;

    public SingleThreadStrategy(int concurrency) {
        this.concurrency = concurrency;
    }

    @Override
    public void start(String queueName, MessageListener messageListener) {
        for (int i = 0; i < concurrency; i++) {
            DequeueThread dequeueThread = new DequeueThread(() -> {
                Message messageResult = redisZSetQOps.dequeue(queueName, Message.class);
                try {
                    if (messageResult == null) {
                        TimeUnit.SECONDS.sleep(1);
                    } else {
                        messageListener.onMessage(messageResult.getPayload());
                    }
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            });
            dequeueThread.setName(String.format("rediszsetq-consumer-single[%s]-%d", queueName, i));
            dequeueThread.start();
        }
    }
}
