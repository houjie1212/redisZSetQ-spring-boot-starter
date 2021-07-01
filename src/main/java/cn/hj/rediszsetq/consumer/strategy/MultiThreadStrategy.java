package cn.hj.rediszsetq.consumer.strategy;

import cn.hj.rediszsetq.consumer.DequeueThread;
import cn.hj.rediszsetq.consumer.MessageListener;
import cn.hj.rediszsetq.model.Message;
import cn.hj.rediszsetq.persistence.RedisZSetQOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MultiThreadStrategy implements ThreadStrategy {

    private static final Logger log = LoggerFactory.getLogger(MultiThreadStrategy.class);

    private final int concurrency;
    private final int fetchCount;

    @Autowired
    private RedisZSetQOps redisZSetQOps;

    public MultiThreadStrategy(int concurrency, int fetchCount) {
        this.concurrency = concurrency;
        this.fetchCount = fetchCount;
    }

    @Override
    public void start(String queueName, MessageListener messageListener) {
        for (int i = 0; i < concurrency; i++) {
            DequeueThread dequeueThread = new DequeueThread(() -> {
                List<Message> messageList = redisZSetQOps.dequeue(queueName, Message.class, fetchCount);
                try {
                    if (CollectionUtils.isEmpty(messageList)) {
                        TimeUnit.SECONDS.sleep(1);
                    } else {
                        List<Object> payloads =
                                messageList.stream().map(Message::getPayload).collect(Collectors.toList());
                        messageListener.onMessage(payloads);
                    }
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            });
            dequeueThread.setName(String.format("rediszsetq-consumer-multi[%s]-%d", queueName, i));
            dequeueThread.start();
        }
    }
}
