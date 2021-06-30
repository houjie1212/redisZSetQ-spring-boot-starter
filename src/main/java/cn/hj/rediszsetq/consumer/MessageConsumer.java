package cn.hj.rediszsetq.consumer;

import cn.hj.rediszsetq.model.Message;
import cn.hj.rediszsetq.persistence.RedisQOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

public class MessageConsumer<T> {

    private static final Logger log = LoggerFactory.getLogger(MessageConsumer.class);

    @Autowired
    private RedisQOps redisQOps;

    private String queueName;
    private MessageListener<T> messageListener;

    public void init() {
        startConsumer();
    }

    public void startConsumer() {

        start(queueName, () -> {
            Message<T> messageResult = redisQOps.dequeue(queueName, Message.class);
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

    }

    public void start(String queueName, Runnable callback) {
        DequeueThread dequeueThread = new DequeueThread(callback);
        dequeueThread.setName(String.format("redisq-consumer[%s]", queueName));
        dequeueThread.start();
    }

    public MessageConsumer setQueueName(String queueName) {
        this.queueName = queueName;
        return this;
    }

    public MessageConsumer setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
        return this;
    }
}
