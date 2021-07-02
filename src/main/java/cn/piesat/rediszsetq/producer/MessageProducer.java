package cn.piesat.rediszsetq.producer;

import cn.piesat.rediszsetq.model.Message;
import cn.piesat.rediszsetq.persistence.RedisZSetQOps;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
public class MessageProducer {

    private final int defaultPriority = 0;
    private final int defaultExpire = 300;

    private final RedisZSetQOps redisZSetQOps;

    public MessageProducer(RedisZSetQOps redisZSetQOps) {
        this.redisZSetQOps = redisZSetQOps;
    }

    public <T> void sendMessage(String queueName, T payload, int priority, int expire) {
        Message<T> message = Message.create(UUID.randomUUID().toString(), payload);
        message.setQueueName(queueName)
                .setPriority(priority)
                .setExpire(expire);
        redisZSetQOps.enqueue(queueName, message, priority, expire);
    }

    public <T> void sendMessage(String queueName, T payload) {
        sendMessage(queueName, payload, defaultPriority, defaultExpire);
    }

    public <T> void sendMessage(String queueName, T payload,  int priority) {
        sendMessage(queueName, payload, priority, defaultExpire);
    }

    public void sendMessage(Message message) {
        if (StringUtils.isEmpty(message.getQueueName())) {
            throw new IllegalArgumentException("队列名不能为空");
        }
        redisZSetQOps.enqueue(message.getQueueName(), message, message.getPriority(), message.getExpire());
    }

}
