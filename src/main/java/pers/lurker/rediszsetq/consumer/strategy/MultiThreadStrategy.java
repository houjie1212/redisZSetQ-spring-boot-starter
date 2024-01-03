package pers.lurker.rediszsetq.consumer.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import pers.lurker.rediszsetq.config.RedisZSetQProperties;
import pers.lurker.rediszsetq.consumer.Consumer;
import pers.lurker.rediszsetq.consumer.MessageListener;
import pers.lurker.rediszsetq.consumer.thread.DequeueThread;
import pers.lurker.rediszsetq.model.Message;
import pers.lurker.rediszsetq.persistence.RedisZSetQService;
import pers.lurker.rediszsetq.util.DateUtil;
import pers.lurker.rediszsetq.util.KeyUtil;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MultiThreadStrategy implements ThreadStrategy {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final int concurrency;
    private final int restTimeIfConsumeNull;
    private final int fetchCount;
    private final List<DequeueThread> dequeueThreads;

    @Resource
    private RedisZSetQService redisZSetQService;
    @Resource
    @Qualifier("zsetQRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private Consumer consumer;
    @Resource
    private RedisZSetQProperties properties;

    public MultiThreadStrategy(int concurrency, int restTimeIfConsumeNull, int fetchCount) {
        this.concurrency = concurrency;
        this.restTimeIfConsumeNull = restTimeIfConsumeNull;
        this.fetchCount = fetchCount;
        dequeueThreads = new ArrayList<>(concurrency);
    }

    @Override
    public void start(String groupName, String queueName, MessageListener messageListener) {
        for (int i = 0; i < concurrency; i++) {
            DequeueThread dequeueThread = new DequeueThread(() -> {
                List<Message> messageResults = redisZSetQService.getByGroupName(groupName)
                    .dequeue(queueName, fetchCount);
                try {
                    if (CollectionUtils.isEmpty(messageResults)) {
                        TimeUnit.SECONDS.sleep(restTimeIfConsumeNull);
                    } else {
                        saveRunningTask(messageResults);
                        saveLogTask(messageResults);

                        messageListener.onMessage(messageResults, consumer);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            dequeueThread.setName(String.format("rediszsetq-consumer-multi[%s-%s]-%d", groupName, queueName, i));
            dequeueThread.start();

            dequeueThreads.add(dequeueThread);
        }
    }

    @Override
    public void stop() {
        dequeueThreads.forEach(Thread::interrupt);
        dequeueThreads.clear();
    }

    /**
     * 消息出队后，标记任务执行中，用于检查是否超时
     * @param messageResults
     * @return
     */
    private void saveRunningTask(List<Message> messageResults) {
        messageResults.forEach(messageResult -> {
            redisTemplate.boundListOps(KeyUtil.taskRunningKey(messageResult.getGroupName(), messageResult.getQueueName()))
                .rightPush(messageResult.getId());

            String statusKey = KeyUtil.taskStatusKeyPrefix(messageResult.getGroupName(), messageResult.getQueueName()) + messageResult.getId();
            BoundValueOperations<String, Object> messageOps = redisTemplate.boundValueOps(statusKey);
            Message message = (Message) messageOps.get();
            message.setStatus(1)
                .setConsumerStartTime(DateUtil.getNow());
            messageOps.set(message);
        });

    }

    /**
     * 记录日志
     * @param messageResults
     */
    private void saveLogTask(List<Message> messageResults) {
        if (properties.getConsumer().isLogEnabled()) {
            messageResults.forEach(messageResult -> {
                redisTemplate.boundListOps(KeyUtil.taskLogKey(messageResult.getGroupName(), messageResult.getQueueName())).rightPushAll(messageResult);
                redisTemplate.expire(KeyUtil.taskLogKey(messageResult.getGroupName(), messageResult.getQueueName()), 7, TimeUnit.DAYS);
            });
        }
    }
}
