package pers.lurker.rediszsetq.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.lurker.rediszsetq.model.Message;
import pers.lurker.rediszsetq.persistence.RedisZSetQService;

import java.util.List;

public class Consumer<T> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RedisZSetQService redisZSetQService;

    public Consumer(RedisZSetQService redisZSetQService) {
        this.redisZSetQService = redisZSetQService;
    }

    /**
     * 确认消费
     * @param message
     */
    public Long ack(Message<T> message) {
        return redisZSetQService.getByGroupName(message.getGroupName()).removeRunningMessage(message);
    }

    /**
     * 批量确认消费
     * @param messages
     */
    public Long ack(List<Message<T>> messages) {
        long result = 0L;
        for (Message<T> message : messages) {
            result += redisZSetQService.getByGroupName(message.getGroupName()).removeRunningMessage(message);
        }
        return result;
    }
}
