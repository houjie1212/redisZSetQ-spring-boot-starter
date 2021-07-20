package cn.piesat.rediszsetq.consumer.strategy;

import cn.piesat.rediszsetq.consumer.Consumer;
import cn.piesat.rediszsetq.consumer.MessageListener;
import cn.piesat.rediszsetq.consumer.thread.DequeueThread;
import cn.piesat.rediszsetq.model.Message;
import cn.piesat.rediszsetq.model.MessageStatusRecord;
import cn.piesat.rediszsetq.persistence.RedisZSetQOps;
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
                List<Message> messageList = redisZSetQOps.dequeue(queueName, fetchCount);
                try {
                    if (CollectionUtils.isEmpty(messageList)) {
                        TimeUnit.SECONDS.sleep(restTimeIfConsumeNull);
                    } else {
                        List<MessageStatusRecord> messageStatusRecordList = messageList.stream()
                                .map(MessageStatusRecord::new)
                                .peek(messageStatusRecord -> redisTemplate.opsForList().rightPush(PROCESSING_TASKS_QNAME, messageStatusRecord))
                                .collect(Collectors.toList());

                        redisTemplate.expire(PROCESSING_TASKS_QNAME, 1, TimeUnit.DAYS);
                        messageListener.onMessage(messageStatusRecordList, consumer);
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
}