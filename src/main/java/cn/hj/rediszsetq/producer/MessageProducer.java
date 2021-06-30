package cn.hj.rediszsetq.producer;

import cn.hj.rediszsetq.model.Message;
import cn.hj.rediszsetq.persistence.RedisQOps;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.UUID;

@Component
public class MessageProducer {

    private final int defaultPriority = 0;
    private final int defaultExpire = 30;

    private final RedisQOps redisQOps;

    public MessageProducer(RedisQOps redisQOps) {
        this.redisQOps = redisQOps;
    }

    public <T> void sendMessage(String queueName, T payload, int priority, int expire) {
        Message<T> message = Message.create(UUID.randomUUID().toString(), payload);
        message.setCreation(Calendar.getInstance());
        redisQOps.enqueue(queueName, message, priority, expire);
    }

    public <T> void sendMessage(String queueName, T payload) {
        sendMessage(queueName, payload, defaultPriority, defaultExpire);
    }

    public <T> void sendMessage(String queueName, T payload,  int priority) {
        sendMessage(queueName, payload, priority, defaultExpire);
    }

}
