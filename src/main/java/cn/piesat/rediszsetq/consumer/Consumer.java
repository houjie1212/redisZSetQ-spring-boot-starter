package cn.piesat.rediszsetq.consumer;

import cn.piesat.rediszsetq.consumer.strategy.ThreadStrategy;
import cn.piesat.rediszsetq.model.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Consumer {

    private final RedisTemplate<String, Object> redisTemplate;

    public Consumer(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void ack(Message message) {
        redisTemplate.opsForList().remove(ThreadStrategy.PROCESSING_TASKS_QNAME, 0, message);
    }

    public void ack(List<Message> messages) {
        messages.forEach(message -> redisTemplate.opsForList().remove(ThreadStrategy.PROCESSING_TASKS_QNAME, 0, message));
    }
}
