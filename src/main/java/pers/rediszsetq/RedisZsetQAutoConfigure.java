package pers.rediszsetq;

import pers.rediszsetq.config.BeanConfig;
import pers.rediszsetq.config.RedisConfig;
import pers.rediszsetq.consumer.MessageListenerContainer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({RedisConfig.class, BeanConfig.class, MessageListenerContainer.class})
public class RedisZsetQAutoConfigure {
}
