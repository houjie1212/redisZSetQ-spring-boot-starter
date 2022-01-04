package pers.lurker.rediszsetq.consumer.strategy;

import pers.lurker.rediszsetq.consumer.Consumer;
import pers.lurker.rediszsetq.consumer.MessageListener;
import pers.lurker.rediszsetq.consumer.thread.DequeueThread;
import pers.lurker.rediszsetq.model.Message;
import pers.lurker.rediszsetq.model.MessageStatusRecord;
import pers.lurker.rediszsetq.persistence.RedisZSetQOps;
import pers.lurker.rediszsetq.util.ClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SingleThreadStrategy implements ThreadStrategy {

    private static final Logger log = LoggerFactory.getLogger(SingleThreadStrategy.class);

    private final int concurrency;
    private final int restTimeIfConsumeNull;
    private final List<DequeueThread> dequeueThreads;

    @Autowired
    private RedisZSetQOps redisZSetQOps;
    @Autowired
    @Qualifier("zsetQRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private Consumer consumer;

    public SingleThreadStrategy(int concurrency, int restTimeIfConsumeNull) {
        this.concurrency = concurrency;
        this.restTimeIfConsumeNull = restTimeIfConsumeNull;
        dequeueThreads = new ArrayList<>(concurrency);
    }

    @Override
    public void start(String queueName, MessageListener messageListener) {
        for (int i = 0; i < concurrency; i++) {
            DequeueThread dequeueThread = new DequeueThread(() -> {
                Message messageResult = redisZSetQOps.dequeue(queueName);
                try {
                    if (messageResult == null) {
                        TimeUnit.SECONDS.sleep(restTimeIfConsumeNull);
                    } else {
                        // 放入记录队列，标记任务执行中
                        MessageStatusRecord messageStatusRecord = saveProcessingTask(messageResult);
                        // 记录日志
                        saveLogTask(messageStatusRecord);

                        messageListener.onMessage(messageStatusRecord, consumer);
                    }
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            });
            dequeueThread.setName(String.format("rediszsetq-consumer-single[%s]-%d", queueName, i));
            dequeueThread.start();

            dequeueThreads.add(dequeueThread);
        }
    }

    @Override
    public void stop() {
        dequeueThreads.forEach(dequeueThread -> dequeueThread.setStopRequested(true));
        dequeueThreads.clear();
    }

    private MessageStatusRecord saveProcessingTask(Message messageResult) {
        MessageStatusRecord messageStatusRecord = new MessageStatusRecord(messageResult);
        redisTemplate.opsForList().rightPush(PROCESSING_TASKS_QNAME + ClientUtil.getClientName(), messageStatusRecord);
        redisTemplate.expire(PROCESSING_TASKS_QNAME + ClientUtil.getClientName(), 1, TimeUnit.DAYS);
        return messageStatusRecord;
    }

    private void saveLogTask(MessageStatusRecord messageStatusRecord) {
        redisTemplate.opsForList().rightPush(LOG_TASKS_QNAME + ClientUtil.getClientName(), messageStatusRecord);
        redisTemplate.expire(LOG_TASKS_QNAME + ClientUtil.getClientName(), 7, TimeUnit.DAYS);
    }
}
