package cn.rediszsetq;

import cn.rediszsetq.config.BeanConfig;
import cn.rediszsetq.config.RedisConfig;
import cn.rediszsetq.consumer.MessageListenerContainer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({RedisConfig.class, BeanConfig.class, MessageListenerContainer.class})
public class RedisZsetQAutoConfigure {
}
