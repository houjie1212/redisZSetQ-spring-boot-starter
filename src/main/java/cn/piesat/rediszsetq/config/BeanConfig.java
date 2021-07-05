package cn.piesat.rediszsetq.config;

import cn.piesat.rediszsetq.consumer.Consumer;
import cn.piesat.rediszsetq.persistence.RedisZSetQOps;
import cn.piesat.rediszsetq.producer.MessageProducer;
import cn.piesat.rediszsetq.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

public class BeanConfig {

    @Bean
    public RedisZSetQOps redisQOps(RedisTemplate<String, Object> redisTemplate) {
        return new RedisZSetQOps(redisTemplate);
    }

    @Bean
    public MessageProducer messageProducer(RedisZSetQOps redisZSetQOps) {
        return new MessageProducer(redisZSetQOps);
    }

    @Bean
    public Consumer consumer(RedisTemplate<String, Object> redisTemplate) {
        return new Consumer(redisTemplate);
    }

    @Bean
    public JsonUtil jsonUtil(ObjectMapper objectMapper) {
        return new JsonUtil(objectMapper);
    }
}
