package pers.lurker.rediszsetq.producer;

import cn.hutool.core.util.IdUtil;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import pers.lurker.rediszsetq.config.RedisZSetQProperties;
import pers.lurker.rediszsetq.model.Message;
import pers.lurker.rediszsetq.persistence.RedisZSetQService;

public class MessageProducer {

    public final int DEFAULT_RETRY_COUNT = 0;
    public final int DEFAULT_PRIORITY = 0;

    private final RedisZSetQProperties properties;
    private final RedisZSetQService redisZSetQService;

    public MessageProducer(RedisZSetQProperties properties, RedisZSetQService redisZSetQService) {
        this.properties = properties;
        this.redisZSetQService = redisZSetQService;
    }

    /**
     * @param queueName 队列名
     * @param payload 消息内容
     * @param priority 优先级
     * @param consumerTimeout 消费超时时间
     */
    public <T> void sendMessage(String groupName, String queueName, T payload,
        int priority, int retryCount, int consumerTimeout) {
        Assert.hasText(queueName, "队列名不能为空");
        Message<T> message = Message.create(
            IdUtil.getSnowflake(properties.getSnowflakeWorkerId()).nextIdStr(), payload);
        message.setGroupName(groupName)
            .setQueueName(queueName)
            .setPriority(priority)
            .setRetryCount(retryCount)
            .setConsumerTimeout(consumerTimeout);
        redisZSetQService.getByGroupName(groupName).enqueue(message, priority);
    }

    /**
     * @param queueName 队列名
     * @param payload 消息内容
     */
    public <T> void sendMessage(String groupName, String queueName, T payload) {
        sendMessage(groupName, queueName, payload, DEFAULT_PRIORITY, DEFAULT_RETRY_COUNT, 0);
    }

    /**
     * @param queueName 队列名
     * @param payload 消息内容
     * @param priority 优先级
     */
    public <T> void sendMessage(String groupName, String queueName, T payload, int priority) {
        sendMessage(groupName, queueName, payload, priority, DEFAULT_RETRY_COUNT, 0);
    }

    /**
     * @param queueName 队列名
     * @param payload 消息内容
     * @param priority 优先级
     * @param retryCount 过期时间
     */
    public <T> void sendMessage(String groupName, String queueName, T payload, int priority, int retryCount) {
        sendMessage(groupName, queueName, payload, priority, retryCount, 0);
    }

    public void sendMessage(Message message) {
        Assert.hasText(message.getQueueName(), "队列名不能为空");
        if (!StringUtils.hasText(message.getId())) {
            message.setId(IdUtil.getSnowflake(properties.getSnowflakeWorkerId()).nextIdStr());
        }
        redisZSetQService.getByGroupName(message.getGroupName())
            .enqueue(message, message.getPriority());
    }

}
