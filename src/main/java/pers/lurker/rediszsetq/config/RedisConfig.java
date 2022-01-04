package pers.lurker.rediszsetq.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * redis连接配置
 */
@Configuration("zsetQRedisConfig")
@EnableConfigurationProperties(RedisProperties.class)
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Bean("zsetQRedisConnectionFactory")
    public RedisConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
        RedisConfiguration redisConfiguration = getRedisConfiguration(redisProperties);

        RedisConnectionFactory redisConnectionFactory =  getLettuceConnectionFactory(redisProperties, redisConfiguration);;
        if (redisConnectionFactory == null) {
            redisConnectionFactory =  getJedisConnectionFactory(redisProperties, redisConfiguration);
        }
        if (redisConnectionFactory == null) {
            redisConnectionFactory = getMutableLettuceConnectionFactory(redisConfiguration);
        }
        return redisConnectionFactory;
    }

    @Bean("zsetQRedisTemplate")
    public RedisTemplate<String, Object> redisTemplate(@Qualifier("zsetQRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
        log.debug("-----------> create zsetq redisTemplate bean...");
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

    private LettuceConnectionFactory getLettuceConnectionFactory(RedisProperties redisProperties, RedisConfiguration redisConfiguration) {
        LettuceClientConfiguration lettuceClientConfig = getLettuceClientConfig(redisProperties);
        if (lettuceClientConfig == null) {
            return null;
        }
        LettuceConnectionFactory factory;
        if (redisConfiguration instanceof RedisSentinelConfiguration) {
            factory = new LettuceConnectionFactory((RedisSentinelConfiguration) redisConfiguration, lettuceClientConfig);
        }
        else if (redisConfiguration instanceof RedisClusterConfiguration) {
            factory = new LettuceConnectionFactory((RedisClusterConfiguration) redisConfiguration, lettuceClientConfig);
        }
        else {
            factory = new LettuceConnectionFactory((RedisStandaloneConfiguration) redisConfiguration, lettuceClientConfig);
        }
//        factory.setShareNativeConnection(true);
//        factory.setValidateConnection(false);
        return factory;
    }

    private LettuceConnectionFactory getMutableLettuceConnectionFactory(RedisConfiguration redisConfiguration) {
        LettuceConnectionFactory factory;
        if (redisConfiguration instanceof RedisSentinelConfiguration) {
            factory = new LettuceConnectionFactory((RedisSentinelConfiguration) redisConfiguration);
        }
        else if (redisConfiguration instanceof RedisClusterConfiguration) {
            factory = new LettuceConnectionFactory((RedisClusterConfiguration) redisConfiguration);
        }
        else {
            factory = new LettuceConnectionFactory((RedisStandaloneConfiguration) redisConfiguration);
        }
//        factory.setShareNativeConnection(true);
//        factory.setValidateConnection(false);
        return factory;
    }

    private JedisConnectionFactory getJedisConnectionFactory(RedisProperties redisProperties, RedisConfiguration redisConfiguration) {
        JedisClientConfiguration jedisClientConfig = getJedisClientConfig(redisProperties);
        if (jedisClientConfig == null) {
            return null;
        }
        JedisConnectionFactory factory;
        if (redisConfiguration instanceof RedisSentinelConfiguration) {
            factory = new JedisConnectionFactory((RedisSentinelConfiguration) redisConfiguration, jedisClientConfig);
        }
        else if (redisConfiguration instanceof RedisClusterConfiguration) {
            factory = new JedisConnectionFactory((RedisClusterConfiguration) redisConfiguration, jedisClientConfig);
        }
        else {
            factory = new JedisConnectionFactory((RedisStandaloneConfiguration) redisConfiguration, jedisClientConfig);
        }
        return factory;
    }

    public RedisConfiguration getRedisConfiguration(RedisProperties redisProperties) {
        RedisSentinelConfiguration sentinelConfiguration = getSentinelConfiguration(redisProperties);
        if (sentinelConfiguration != null) {
            log.debug("redis configuration: sentinel");
            return sentinelConfiguration;
        }
        RedisClusterConfiguration clusterConfiguration = getClusterConfiguration(redisProperties);
        if (clusterConfiguration != null) {
            log.debug("redis configuration: cluster");
            return clusterConfiguration;
        }
        log.debug("redis configuration: standalone");
        return getStandaloneConfiguration(redisProperties);
    }

    private RedisSentinelConfiguration getSentinelConfiguration(RedisProperties redisProperties) {
        RedisProperties.Sentinel sentinel = redisProperties.getSentinel();
        if (sentinel == null) {
            return null;
        }
        RedisSentinelConfiguration config = new RedisSentinelConfiguration();
        config.master(sentinel.getMaster());

        List<RedisNode> nodes = new ArrayList<>();
        for (String node : sentinel.getNodes()) {
            String[] parts = StringUtils.split(node, ":");
            Assert.state(parts.length == 2, "redis哨兵地址配置不合法！");
            nodes.add(new RedisNode(parts[0], Integer.parseInt(parts[1])));
        }
        config.setSentinels(nodes);
        if (StringUtils.hasText(redisProperties.getPassword())) {
            config.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }
        return config;
    }

    private RedisClusterConfiguration getClusterConfiguration(RedisProperties redisProperties) {
        RedisProperties.Cluster cluster = redisProperties.getCluster();
        if (cluster == null) {
            return null;
        }
        RedisClusterConfiguration config = new RedisClusterConfiguration(cluster.getNodes());
        if (cluster.getMaxRedirects() != null) {
            config.setMaxRedirects(cluster.getMaxRedirects());
        }
        if (StringUtils.hasText(redisProperties.getPassword())) {
            config.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }
        return config;
    }

    private RedisStandaloneConfiguration getStandaloneConfiguration(RedisProperties redisProperties) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setDatabase(redisProperties.getDatabase());
        config.setHostName(redisProperties.getHost());
        config.setPort(redisProperties.getPort());
        if (StringUtils.hasText(redisProperties.getPassword())) {
            config.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }
        return config;
    }

    private LettuceClientConfiguration getLettuceClientConfig(RedisProperties redisProperties) {
        RedisProperties.Lettuce lettuce = redisProperties.getLettuce();
        if (lettuce == null || lettuce.getPool() == null) {
            return null;
        }
        GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
        genericObjectPoolConfig.setMaxIdle(lettuce.getPool().getMaxIdle());
        genericObjectPoolConfig.setMinIdle(lettuce.getPool().getMinIdle());
        genericObjectPoolConfig.setMaxTotal(lettuce.getPool().getMaxActive());
        genericObjectPoolConfig.setMaxWaitMillis(lettuce.getPool().getMaxWait().toMillis());
        genericObjectPoolConfig.setTimeBetweenEvictionRunsMillis(lettuce.getPool().getTimeBetweenEvictionRuns().toMillis());
        return LettucePoolingClientConfiguration.builder()
                .commandTimeout(redisProperties.getTimeout())
                .shutdownTimeout(lettuce.getShutdownTimeout())
                .poolConfig(genericObjectPoolConfig)
                .build();
    }

    private JedisClientConfiguration getJedisClientConfig(RedisProperties redisProperties) {
        RedisProperties.Jedis jedis = redisProperties.getJedis();
        if (jedis == null || jedis.getPool() == null) {
            return null;
        }
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(jedis.getPool().getMaxIdle());
        jedisPoolConfig.setMinIdle(jedis.getPool().getMinIdle());
        jedisPoolConfig.setMaxTotal(jedis.getPool().getMaxActive());
        jedisPoolConfig.setMaxWaitMillis(jedis.getPool().getMaxWait().toMillis());
        if (jedis.getPool().getTimeBetweenEvictionRuns() != null) {
            jedisPoolConfig.setTimeBetweenEvictionRunsMillis(jedis.getPool().getTimeBetweenEvictionRuns().toMillis());
        }
        return ((JedisClientConfiguration.JedisPoolingClientConfigurationBuilder) JedisClientConfiguration.builder())
                .poolConfig(jedisPoolConfig)
                .build();
    }
}
