package cn.hj.rediszsetq.persistence;

import cn.hj.rediszsetq.model.Message;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class RedisZSetQOps {

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

    public <T> T dequeue(String key, Class<T> resultClass) {
        String dequeueLua = "local result = redis.call('ZREVRANGE', KEYS[1], 0, 0) " +
                "local ele = result[1] " +
                "if ele then " +
                "   redis.call('ZREM', KEYS[1], ele) " +
                "   return ele " +
                "else " +
                "   return nil " +
                "end";
        DefaultRedisScript<T> redisScript = new DefaultRedisScript<>(dequeueLua, resultClass);
        return redisTemplate.execute(redisScript, Collections.singletonList(key));
    }

    public <T> List<T> dequeue(String key, Class<T> resultClass, int rows) {
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
