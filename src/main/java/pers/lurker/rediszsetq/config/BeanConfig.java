package pers.lurker.rediszsetq.config;

import pers.lurker.rediszsetq.consumer.Consumer;
import pers.lurker.rediszsetq.persistence.RedisZSetQOps;
import pers.lurker.rediszsetq.producer.MessageProducer;
import pers.lurker.rediszsetq.util.ClientUtil;
import pers.lurker.rediszsetq.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 自定义Bean声明
 */
@Configuration("zsetQRedisBeanConfig")
@EnableConfigurationProperties({RedisZSetQConsumerProperties.class})
@ComponentScan("pers.lurker.rediszsetq")
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

    @Bean
    public ClientUtil clientUtil(@Value("${spring.application.name:}") String applicationName,
                                 RedisZSetQConsumerProperties redisZSetQConsumerProperties) {
        return new ClientUtil(applicationName, redisZSetQConsumerProperties);
    }
}
