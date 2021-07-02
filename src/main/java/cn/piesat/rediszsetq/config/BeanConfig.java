package cn.piesat.rediszsetq.config;

import cn.piesat.rediszsetq.consumer.Consumer;
import cn.piesat.rediszsetq.persistence.RedisZSetQOps;
import cn.piesat.rediszsetq.producer.MessageProducer;
import cn.piesat.rediszsetq.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class BeanConfig {

    private final RedisTemplate<String, Object> redisTemplate;

    public BeanConfig(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Bean
    @ConditionalOnMissingBean(RedisZSetQOps.class)
    public RedisZSetQOps redisQOps() {
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
