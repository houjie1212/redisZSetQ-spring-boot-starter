package cn.hj.rediszsetq.consumer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisZSetListener {

    // 消费队列名
    String value();

    // 并发线程数
    int concurrency() default 1;

    // 拉取消息数量
    int fetchCount() default 1;
}
