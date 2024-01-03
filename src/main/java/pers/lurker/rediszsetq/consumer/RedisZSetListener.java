package pers.lurker.rediszsetq.consumer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisZSetListener {

    /**
     * 消费队列名
     */
    String[] value() default {};

    /**
     * 并发线程数
     */
    String concurrency() default "0";

    /**
     * 拉取消息数量
     */
    String fetchCount() default "0";

    /**
     * 消费消息为空时，休眠时间（秒）
     */
    String restTimeIfConsumeNull() default "1";
}
