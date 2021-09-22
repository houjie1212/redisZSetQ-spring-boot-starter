package pers.rediszsetq.consumer;

import pers.rediszsetq.model.Message;
import pers.rediszsetq.consumer.strategy.ThreadStrategy;
import pers.rediszsetq.util.ClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

public class Consumer<T> {

    private static final Logger log = LoggerFactory.getLogger(Consumer.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public Consumer(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void ack(Message<T> message) {
        redisTemplate.opsForList().remove(ThreadStrategy.PROCESSING_TASKS_QNAME + ClientUtil.getClientName(), 0,
                message);
    }

    public void ack(List<Message<T>> messages) {
        messages.forEach(message ->
                redisTemplate.opsForList().remove(ThreadStrategy.PROCESSING_TASKS_QNAME + ClientUtil.getClientName(),
                        0, message));
    }
}
