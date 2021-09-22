package cn.rediszsetq.persistence;

import cn.rediszsetq.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.List;

public class RedisZSetQOps {

    private static final Logger log = LoggerFactory.getLogger(RedisZSetQOps.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisZSetQOps(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public <T> void enqueue(String key, Message<T> value, int priority, int expire) {
        String enqueueLua =
                "redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1]) " +
                        "redis.call('EXPIRE', KEYS[1], ARGV[3])";
        DefaultRedisScript<Object> redisScript = new DefaultRedisScript<>(enqueueLua);
        redisTemplate.execute(redisScript, Collections.singletonList(key), value, priority, expire);
    }

    public Message dequeue(String key) {
        String dequeueLua = "local result = redis.call('ZREVRANGE', KEYS[1], 0, 0) " +
                "local ele = result[1] " +
                "if ele then " +
                    "redis.call('ZREM', KEYS[1], ele) " +
                    "return ele " +
                "else " +
                    "return nil " +
                "end";
        DefaultRedisScript<Message> redisScript = new DefaultRedisScript(dequeueLua, Message.class);
        return redisTemplate.execute(redisScript, Collections.singletonList(key));
    }

    public <T> List<Message> dequeue(String key, int rows) {
        String dequeueLua = "local result = redis.call('ZREVRANGE', KEYS[1], 0, ARGV[1]) " +
                "for i, ele in pairs(result) do " +
                "   if ele then " +
                "       redis.call('ZREM', KEYS[1], ele) " +
                "   end " +
                "end " +
                "return result";
        DefaultRedisScript<List> redisScript = new DefaultRedisScript<>(dequeueLua, List.class);
        return redisTemplate.execute(redisScript, Collections.singletonList(key), --rows);
    }

}
