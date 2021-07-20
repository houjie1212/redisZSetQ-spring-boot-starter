# redisZSetQ
用Redis Sorted-Set实现的优先级消息队列，可靠消费

## Maven Dependency
Clone this repo to local and add dependency.
```xml
<dependency>
    <groupId>cn.piesat</groupId>
    <artifactId>redisZSetQ-spring-boot-starter</artifactId>
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

rediszsetq.enabled=true
# 消费执行超时时间（秒），默认60
rediszsetq.consumer.timeout=60
# 消费执行超时检查频率（秒/次），默认5
rediszsetq.consumer.timeout-check-interval=5
```
### Enable Annotation

```java
import cn.piesat.rediszsetq.config.EnableRedisZSetQ;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRedisZSetQ
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
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
继承 MessageListenerAdapter<T> 并在 onMessage 方法上添加 @RedisZSetListener 注解
##### @RedisZSetListener 属性:
- value: 队列名，必须
- concurrency: 并发线程数，非必须，默认1
- fetchCount: 拉取消息数，非必须，只有接收List生效，默认1
- restTimeIfConsumeNull: 消费消息为空时，休眠时间（秒），默认1

```java
import MessageListenerAdapter;
import RedisZSetListener;
import cn.piesat.rediszsetq.consumer.Consumer;
import cn.piesat.rediszsetq.model.Message;
import org.springframework.stereotype.Component;

@Component
public class StringMessageListener extends MessageListenerAdapter<String> {

    @Override
    @RedisZSetListener("queueName")
    public void onMessage(Message<String> message, Consumer consumer) {
        System.out.println(message);
        consumer.ack(message);
    }

    @Override
    @RedisZSetListener(value = "queueName", concurrency = 2, fetchCount = 2)
    public void onMessage(List<Message<String>> messages, Consumer consumer) {
        messages.forEach(message -> System.out.println(message));
        consumer.ack(messages);
    }
}
```
