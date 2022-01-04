package pers.lurker.rediszsetq;

import pers.lurker.rediszsetq.config.BeanConfig;
import pers.lurker.rediszsetq.config.RedisConfig;
import pers.lurker.rediszsetq.consumer.MessageListenerContainer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({RedisConfig.class, BeanConfig.class, MessageListenerContainer.class})
public class RedisZsetQAutoConfigure {
}
