package pers.lurker.rediszsetq.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.util.CollectionUtils;
import pers.lurker.rediszsetq.model.Message;
import pers.lurker.rediszsetq.util.JsonUtil;
import pers.lurker.rediszsetq.util.KeyUtil;

import java.util.*;
import java.util.stream.Collectors;

public class RedisZSetQOps {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private String groupName;
    private RedisTemplate<String, Object> redisTemplate;
    private final ResourceScriptSource ZSetEnqueueScript;
    private final ResourceScriptSource ZSetDequeueSingleScript;
    private final ResourceScriptSource ZSetDequeueMultiScript;
    private final ResourceScriptSource ZSetFindRangeScript;

    public RedisZSetQOps() {
        ZSetEnqueueScript = new ResourceScriptSource(new ClassPathResource("lua/ZSetEnqueue.lua"));
        ZSetDequeueSingleScript = new ResourceScriptSource(new ClassPathResource("lua/ZSetDequeueSingle.lua"));
        ZSetDequeueMultiScript = new ResourceScriptSource(new ClassPathResource("lua/ZSetDequeueMulti.lua"));
        ZSetFindRangeScript = new ResourceScriptSource(new ClassPathResource("lua/ZSetFindRange.lua"));
    }

    public RedisZSetQOps(String groupName) {
        this.groupName = groupName;
        ZSetEnqueueScript = new ResourceScriptSource(new ClassPathResource("lua/ZSetEnqueue.lua"));
        ZSetDequeueSingleScript = new ResourceScriptSource(new ClassPathResource("lua/ZSetDequeueSingle.lua"));
        ZSetDequeueMultiScript = new ResourceScriptSource(new ClassPathResource("lua/ZSetDequeueMulti.lua"));
        ZSetFindRangeScript = new ResourceScriptSource(new ClassPathResource("lua/ZSetFindRange.lua"));
    }

    public RedisZSetQOps self() {
        return this;
    }

    /**
     * 消息入队
     * @param value 消息实例
     * @param priority 优先级
     * @param <T> 消息内容的类型
     */
    public <T> void enqueue(Message<T> value, int priority) {
        DefaultRedisScript<Object> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(ZSetEnqueueScript);

        String statusKey = KeyUtil.taskStatusKeyPrefix(groupName, value.getQueueName()) + value.getId();
        redisTemplate.boundValueOps(statusKey).set(value);

        redisTemplate.execute(redisScript,
            Collections.singletonList(KeyUtil.taskRankKey(groupName, value.getQueueName())),
            value.getId(), -priority);
        log.trace("消息进入排队队列: key: {}, value: {}", statusKey, JsonUtil.obj2String(value));
    }

    /**
     * 单条消息出队
     * @param queueName 队列名
     * @return 消息实例
     */
    public Message dequeue(String queueName) {
        DefaultRedisScript<String> redisScript = new DefaultRedisScript();
        redisScript.setScriptSource(ZSetDequeueSingleScript);
        redisScript.setResultType(String.class);
        String messageId = redisTemplate.execute(redisScript, Collections.singletonList(KeyUtil.taskRankKey(groupName, queueName)));
        if (messageId == null) {
            return null;
        }
        String statusKey = KeyUtil.taskStatusKeyPrefix(groupName, queueName) + messageId;
        Message message = (Message) redisTemplate.boundValueOps(statusKey).get();
        if (message == null) {
            log.warn("消息单条弹出排队队列异常: key: {}, value: {}", statusKey, JsonUtil.obj2String(message));
        }
        return message;
    }

    /**
     * 多条消息出队
     * @param queueName 队列名
     * @param rows 拉取条数
     * @return 消息实例集合
     */
    public List<Message> dequeue(String queueName, int rows) {
        DefaultRedisScript<List> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(ZSetDequeueMultiScript);
        redisScript.setResultType(List.class);
        List messageIds = redisTemplate.execute(redisScript, Collections.singletonList(KeyUtil.taskRankKey(groupName, queueName)), --rows);
        if (CollectionUtils.isEmpty(messageIds)) {
            return Collections.emptyList();
        }
        List<Message> messages = (List<Message>) messageIds.stream().map(messageId ->
                redisTemplate.boundValueOps(KeyUtil.taskStatusKeyPrefix(groupName, queueName) + messageId).get()
            )
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if (messageIds.size() != messages.size()) {
            log.warn("消息批量弹出排队队列异常: keys: {}, values: {}", JsonUtil.obj2String(messageIds), JsonUtil.obj2String(messages));
        }
        return messages;
    }

    public Long removeRunningMessage(Message message) {
        Long remove = redisTemplate.boundListOps(KeyUtil.taskRunningKey(groupName, message.getQueueName())).remove(0, message.getId());
        if (remove > 0) {
            redisTemplate.delete(KeyUtil.taskStatusKeyPrefix(groupName, message.getQueueName()) + message.getId());
        }
        return remove;
    }

    /**
     * 查询zset中指定元素之后限定数量的元素列表
     */
    public List<Message> findRankRange(String queueName, int limit, String member) {
        DefaultRedisScript<List> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(ZSetFindRangeScript);
        redisScript.setResultType(List.class);
        List messageIds = redisTemplate.execute(redisScript,
            Collections.singletonList(KeyUtil.taskRankKey(groupName, queueName)), limit, member);
        return (List<Message>) messageIds.stream().map(messageId ->
                redisTemplate.boundValueOps(KeyUtil.taskStatusKeyPrefix(groupName, queueName) + messageId).get()
            )
            .collect(Collectors.toList());
    }

    public int findRankCount(String queueName) {
        Long count = redisTemplate.boundZSetOps(KeyUtil.taskRankKey(groupName, queueName)).zCard();
        return count.intValue();
    }

    /**
     * 删除队列消息
     * @param queueName 队列名
     * @param id
     * @return 被删除的消息
     */
    public Message removeRankMessage(String queueName, String id) {
        long remove = redisTemplate.boundZSetOps(KeyUtil.taskRankKey(groupName, queueName)).remove(id);
        if (remove > 0) {
            Object o = redisTemplate.boundValueOps(KeyUtil.taskStatusKeyPrefix(groupName, queueName) + id).get();
            redisTemplate.delete(KeyUtil.taskStatusKeyPrefix(groupName, queueName) + id);
            return (Message) o;
        }
        return null;
    }

    /**
     * 清空队列
     * @param queueName 队列名
     * @return 被删除的消息
     */
    public List<Message> clearRankMessage(String queueName) {
        BoundZSetOperations<String, Object> zsetOps = redisTemplate.boundZSetOps(KeyUtil.taskRankKey(groupName, queueName));
        Set<Object> messageIds = zsetOps.range(0, -1);
        if (CollectionUtils.isEmpty(messageIds)) {
            return Collections.emptyList();
        }
        List<Message> removed = new ArrayList<>();
        for (Object messageId : messageIds) {
            if (zsetOps.remove(messageId) > 0) {
                Object o = redisTemplate.boundValueOps(KeyUtil.taskStatusKeyPrefix(groupName, queueName) + messageId).get();
                redisTemplate.delete(KeyUtil.taskStatusKeyPrefix(groupName, queueName) + messageId);
                removed.add((Message) o);
            }
        }
        return removed;
    }

    public List<Message> findRankAll(String queueName) {
        Set<Object> rankMessages = redisTemplate.opsForZSet().range(KeyUtil.taskRankKey(groupName, queueName), 0, -1);
        return rankMessages.stream().map(messageId ->
                (Message) redisTemplate.boundValueOps(
                    KeyUtil.taskStatusKeyPrefix(groupName, queueName) + messageId).get()
            )
            .collect(Collectors.toList());
    }

    public String getGroupName() {
        return groupName;
    }

    public RedisZSetQOps setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        return this;
    }

}
