# redisZSetQ
用Redis Sorted-Set实现的优先级消息队列

## Maven Dependency
Clone this repo to local and add dependency.
```xml
<dependency>
    <groupId>cn.hj</groupId>
    <artifactId>redisZSetQ</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```
## Useage
### Config
```properties
spring.redis.host=
spring.redis.port=
spring.redis.password=
spring.redis.database=
spring.redis.timeout=
spring.redis.lettuce.pool.max-active=
spring.redis.lettuce.pool.max-wait=
spring.redis.lettuce.pool.max-idle=
spring.redis.lettuce.pool.min-idle=

redisZSetQ.enabled=true
```
### MessageProducer

```java
import org.springframework.beans.factory.annotation.Autowired;

@Autowired
private MessageProducer messageProducer;

public void produce() {
    // 队列名, 消息内容, 优先级
    messageProducer.send("queueName", "hello", 0);
}
```
### MessageListener
##### @RedisZSetListener属性:
- value: 队列名
- concurrency: 并发线程数
- fetchCount: 拉取消息数

```java
import cn.hj.rediszsetq.consumer.AbstractMessageListener;
import cn.hj.rediszsetq.consumer.RedisZSetListener;
import org.springframework.stereotype.Component;

@Component
public class StringMessageListener extends AbstractMessageListener<String> {

    @Override
    @RedisZSetListener("queueName")
    public void onMessage(String message) {
        System.out.println(message);
    }

    @Override
    @RedisZSetListener(value = "queueName", concurrency = 2, fetchCount = 2)
    public void onMessage(List<String> messages) {
        messages.forEach(message -> System.out.println(message));
    }
}
```