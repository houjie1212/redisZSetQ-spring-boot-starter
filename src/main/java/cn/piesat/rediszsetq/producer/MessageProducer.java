package cn.piesat.rediszsetq.producer;

import cn.piesat.rediszsetq.model.Message;
import cn.piesat.rediszsetq.persistence.RedisZSetQOps;
import org.springframework.util.Assert;

import java.util.UUID;

public class MessageProducer {

    private final int defaultPriority = 0;
    private final int defaultExpire = 300;

    private final RedisZSetQOps redisZSetQOps;

    public MessageProducer(RedisZSetQOps redisZSetQOps) {
        this.redisZSetQOps = redisZSetQOps;
    }

    public <T> void sendMessage(String queueName, T payload, int priority, int expire, int consumerTimeout) {
        Assert.hasText(queueName, "队列名不能为空");
        Message<T> message = Message.create(UUID.randomUUID().toString(), payload);
        message.setQueueName(queueName)
                .setPriority(priority)
                .setExpire(expire)
                .setConsumerTimeout(consumerTimeout);
        redisZSetQOps.enqueue(queueName, message, priority, expire);
    }

    public <T> void sendMessage(String queueName, T payload) {
        sendMessage(queueName, payload, defaultPriority, defaultExpire, 0);
    }

    public <T> void sendMessage(String queueName, T payload, int priority) {
        sendMessage(queueName, payload, priority, defaultExpire, 0);
    }

    public <T> void sendMessage(String queueName, T payload, int priority, int consumerTimeout) {
        sendMessage(queueName, payload, priority, defaultExpire, consumerTimeout);
    }

    public void sendMessage(Message message) {
        Assert.hasText(message.getQueueName(), "队列名不能为空");
        redisZSetQOps.enqueue(message.getQueueName(), message, message.getPriority(), message.getExpire());
    }

}
