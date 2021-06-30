package cn.hj.rediszsetq.producer;

import cn.hj.rediszsetq.model.Message;
import cn.hj.rediszsetq.persistence.RedisZSetQOps;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.UUID;

@Component
public class MessageProducer {

    private final int defaultPriority = 0;
    private final int defaultExpire = 30;

    private final RedisZSetQOps redisZSetQOps;

    public MessageProducer(RedisZSetQOps redisZSetQOps) {
        this.redisZSetQOps = redisZSetQOps;
    }

    public <T> void sendMessage(String queueName, T payload, int priority, int expire) {
        Message<T> message = Message.create(UUID.randomUUID().toString(), payload);
        message.setCreation(Calendar.getInstance());
        redisZSetQOps.enqueue(queueName, message, priority, expire);
    }

    public <T> void sendMessage(String queueName, T payload) {
        sendMessage(queueName, payload, defaultPriority, defaultExpire);
    }

    public <T> void sendMessage(String queueName, T payload,  int priority) {
        sendMessage(queueName, payload, priority, defaultExpire);
    }

}
