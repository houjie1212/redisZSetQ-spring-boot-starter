package pers.lurker.rediszsetq.consumer.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
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

public class SingleThreadStrategy implements ThreadStrategy {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final int concurrency;
    private final int restTimeIfConsumeNull;
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

    public SingleThreadStrategy(int concurrency, int restTimeIfConsumeNull) {
        this.concurrency = concurrency;
        this.restTimeIfConsumeNull = restTimeIfConsumeNull;
        dequeueThreads = new ArrayList<>(concurrency);
    }

    @Override
    public void start(String groupName, String queueName, MessageListener messageListener) {
        for (int i = 0; i < concurrency; i++) {
            DequeueThread dequeueThread = new DequeueThread(() -> {
                Message messageResult = redisZSetQService.getByGroupName(groupName).dequeue(queueName);
                try {
                    if (messageResult == null) {
                        TimeUnit.SECONDS.sleep(restTimeIfConsumeNull);
                    } else {
                        saveRunningTask(messageResult);
                        saveLogTask(messageResult);

                        messageListener.onMessage(messageResult, consumer);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            dequeueThread.setName(String.format("rediszsetq-consumer-single[%s-%s]-%d", groupName, queueName, i));
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
     * @param messageResult
     * @return
     */
    private void saveRunningTask(Message messageResult) {
        redisTemplate.boundListOps(KeyUtil.taskRunningKey(messageResult.getGroupName(), messageResult.getQueueName()))
            .rightPush(messageResult.getId());

        String statusKey = KeyUtil.taskStatusKeyPrefix(messageResult.getGroupName(), messageResult.getQueueName()) + messageResult.getId();
        BoundValueOperations<String, Object> messageOps = redisTemplate.boundValueOps(statusKey);
        Message message = (Message) messageOps.get();
        message.setStatus(1)
            .setConsumerStartTime(DateUtil.getNow());
        messageOps.set(message);
    }

    /**
     * 记录日志
     * @param messageResult
     */
    private void saveLogTask(Message messageResult) {
        if (properties.getConsumer().isLogEnabled()) {
            redisTemplate.boundListOps(KeyUtil.taskLogKey(messageResult.getGroupName(), messageResult.getQueueName())).rightPush(messageResult);
            redisTemplate.expire(KeyUtil.taskLogKey(messageResult.getGroupName(), messageResult.getQueueName()), 7, TimeUnit.DAYS);
        }
    }
}
