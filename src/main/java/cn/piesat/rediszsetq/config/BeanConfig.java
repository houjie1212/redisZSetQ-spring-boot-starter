package cn.piesat.rediszsetq.config;

import cn.piesat.rediszsetq.consumer.Consumer;
import cn.piesat.rediszsetq.persistence.RedisZSetQOps;
import cn.piesat.rediszsetq.producer.MessageProducer;
import cn.piesat.rediszsetq.util.JsonUtil;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@ConditionalOnProperty(prefix = "rediszsetq", value = "enabled", havingValue = "true")
@EnableConfigurationProperties({RedisZSetQConsumerProperties.class})
@ComponentScan("cn.piesat.rediszsetq")
public class BeanConfig {

    private static final Logger log = LoggerFactory.getLogger(BeanConfig.class);

    @Bean("zsetQLettuceConnectionFactory")
    public LettuceConnectionFactory lettuceConnectionFactory(RedisProperties redisProperties) {
        RedisProperties.Lettuce lettuce = redisProperties.getLettuce();
        GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
        genericObjectPoolConfig.setMaxIdle(lettuce.getPool().getMaxIdle());
        genericObjectPoolConfig.setMinIdle(lettuce.getPool().getMinIdle());
        genericObjectPoolConfig.setMaxTotal(lettuce.getPool().getMaxActive());
        genericObjectPoolConfig.setMaxWaitMillis(lettuce.getPool().getMaxWait().toMillis());
        genericObjectPoolConfig.setTimeBetweenEvictionRunsMillis(100);

        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setDatabase(redisProperties.getDatabase());
        redisStandaloneConfiguration.setHostName(redisProperties.getHost());
        redisStandaloneConfiguration.setPort(redisProperties.getPort());
        redisStandaloneConfiguration.setPassword(RedisPassword.of(redisProperties.getPassword()));

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(redisProperties.getTimeout())
                .shutdownTimeout(lettuce.getShutdownTimeout())
                .poolConfig(genericObjectPoolConfig)
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);
//        factory.setShareNativeConnection(true);
//        factory.setValidateConnection(false);
        return factory;
    }

    @Bean("zsetQRedisTemplate")
    public RedisTemplate<String, Object> redisTemplate(@Qualifier("zsetQLettuceConnectionFactory") LettuceConnectionFactory connectionFactory) {
        log.info("-----------> create zsetq redisTemplate bean...");
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());

        Jackson2JsonRedisSerializer<Object> redisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        redisSerializer.setObjectMapper(mapper);
        template.setValueSerializer(redisSerializer);

        template.afterPropertiesSet();
        return template;
    }

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
