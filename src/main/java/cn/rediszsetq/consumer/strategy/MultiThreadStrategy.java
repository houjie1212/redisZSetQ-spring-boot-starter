package cn.rediszsetq.consumer.strategy;

import cn.rediszsetq.consumer.Consumer;
import cn.rediszsetq.consumer.MessageListener;
import cn.rediszsetq.consumer.thread.DequeueThread;
import cn.rediszsetq.model.Message;
import cn.rediszsetq.persistence.RedisZSetQOps;
import cn.rediszsetq.model.MessageStatusRecord;
import cn.rediszsetq.util.ClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MultiThreadStrategy implements ThreadStrategy {

    private static final Logger log = LoggerFactory.getLogger(MultiThreadStrategy.class);

    private final int concurrency;
    private final int restTimeIfConsumeNull;
    private final int fetchCount;
    private final List<DequeueThread> dequeueThreads;

    @Autowired
    private RedisZSetQOps redisZSetQOps;
    @Autowired
    @Qualifier("zsetQRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private Consumer consumer;

    public MultiThreadStrategy(int concurrency, int restTimeIfConsumeNull, int fetchCount) {
        this.concurrency = concurrency;
        this.restTimeIfConsumeNull = restTimeIfConsumeNull;
        this.fetchCount = fetchCount;
        dequeueThreads = new ArrayList<>(concurrency);
    }

    @Override
    public void start(String queueName, MessageListener messageListener) {
        for (int i = 0; i < concurrency; i++) {
            DequeueThread dequeueThread = new DequeueThread(() -> {
                List<Message> messageResults = redisZSetQOps.dequeue(queueName, fetchCount);
                try {
                    if (CollectionUtils.isEmpty(messageResults)) {
                        TimeUnit.SECONDS.sleep(restTimeIfConsumeNull);
                    } else {
                        List<MessageStatusRecord> messageStatusRecords = saveProcessingTask(messageResults);
                        saveLogTask(messageStatusRecords);

                        messageListener.onMessage(messageStatusRecords, consumer);
                    }
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            });
            dequeueThread.setName(String.format("rediszsetq-consumer-multi[%s]-%d", queueName, i));
            dequeueThread.start();

            dequeueThreads.add(dequeueThread);
        }
    }

    @Override
    public void stop() {
        dequeueThreads.forEach(dequeueThread -> {
            dequeueThread.setStopRequested(true);
        });
        dequeueThreads.clear();
    }

    private List<MessageStatusRecord> saveProcessingTask(List<Message> messageResults) {
        List<MessageStatusRecord> messageStatusRecords = messageResults.stream()
                .map(MessageStatusRecord::new)
                .collect(Collectors.toList());
        redisTemplate.opsForList().rightPushAll(PROCESSING_TASKS_QNAME + ClientUtil.getClientName(), messageStatusRecords);
        redisTemplate.expire(PROCESSING_TASKS_QNAME + ClientUtil.getClientName(), 1, TimeUnit.DAYS);
        return messageStatusRecords;
    }

    private void saveLogTask(List<MessageStatusRecord> messageStatusRecords) {
        redisTemplate.opsForList().rightPushAll(LOG_TASKS_QNAME + ClientUtil.getClientName(), messageStatusRecords);
        redisTemplate.expire(LOG_TASKS_QNAME + ClientUtil.getClientName(), 7, TimeUnit.DAYS);
    }
}
