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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SingleThreadStrategy implements ThreadStrategy {

    private static final Logger log = LoggerFactory.getLogger(SingleThreadStrategy.class);

    private final int concurrency;
    private final List<DequeueThread> dequeueThreads;

    @Autowired
    private RedisZSetQOps redisZSetQOps;
    @Autowired
    @Qualifier("zsetQRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private Consumer consumer;

    public SingleThreadStrategy(int concurrency) {
        this.concurrency = concurrency;
        dequeueThreads = new ArrayList<>(concurrency);
    }

    @Override
    public void start(String queueName, MessageListener messageListener) {
        for (int i = 0; i < concurrency; i++) {
            DequeueThread dequeueThread = new DequeueThread(() -> {
                Message messageResult = redisZSetQOps.dequeue(queueName);
                try {
                    if (messageResult == null) {
                        TimeUnit.SECONDS.sleep(1);
                    } else {
                        // 放入记录队列，标记任务执行中
                        MessageStatusRecord messageStatusRecord = new MessageStatusRecord(messageResult);
                        redisTemplate.opsForList().rightPush(PROCESSING_TASKS_QNAME, messageStatusRecord);
                        redisTemplate.expire(PROCESSING_TASKS_QNAME, 1, TimeUnit.DAYS);

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
        dequeueThreads.forEach(dequeueThread -> {
            dequeueThread.setStopRequested(true);
        });
        dequeueThreads.clear();
    }
}
