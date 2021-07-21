package cn.piesat.rediszsetq.config;

import cn.piesat.rediszsetq.consumer.MessageListenerContainer;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({RedisConfig.class, BeanConfig.class, MessageListenerContainer.class})
public @interface EnableRedisZSetQ {
}
