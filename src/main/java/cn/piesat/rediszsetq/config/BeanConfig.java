package cn.piesat.rediszsetq.config;

import cn.piesat.rediszsetq.consumer.Consumer;
import cn.piesat.rediszsetq.persistence.RedisZSetQOps;
import cn.piesat.rediszsetq.producer.MessageProducer;
import cn.piesat.rediszsetq.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 自定义Bean声明
 */
@Configuration
@EnableConfigurationProperties({RedisZSetQConsumerProperties.class})
@ComponentScan("cn.piesat.rediszsetq")
public class BeanConfig {

    private static final Logger log = LoggerFactory.getLogger(BeanConfig.class);

    @Bean
    public RedisZSetQOps redisQOps(@Qualifier("zsetQRedisTemplate") RedisTemplate<String, Object> redisTemplate) {
        return new RedisZSetQOps(redisTemplate);
    }

    @Bean
    public MessageProducer messageProducer(RedisZSetQOps redisZSetQOps) {
        return new MessageProducer(redisZSetQOps);
    }

    @Bean
    public Consumer consumer(@Qualifier("zsetQRedisTemplate") RedisTemplate<String, Object> redisTemplate) {
        return new Consumer(redisTemplate);
    }

    @Bean
    public JsonUtil jsonUtil(ObjectMapper objectMapper) {
        return new JsonUtil(objectMapper);
    }
}
