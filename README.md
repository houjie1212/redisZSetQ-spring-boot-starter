# redisZSetQ
用Redis Sorted-Set实现的优先级消息队列，可靠消费

## Maven Dependency
```xml
<dependency>
    <groupId>pers</groupId>
    <artifactId>redisZSetQ-spring-boot-starter</artifactId>
    <version>${version}</version>
</dependency>
```

## Useage
### Config
根据配置信息自动选择集群类型和连接池类型：
- 集群优先级：sentinel > cluster > standalone
- 连接池优先级：lettuce > jedis > null
```properties
spring.redis.host=
spring.redis.port=
spring.redis.password=
spring.redis.database=
spring.redis.timeout=

# 消费执行超时时间（秒），默认3600
rediszsetq.consumer.timeout=3600
# 消费执行超时检查频率（秒/次），默认5
rediszsetq.consumer.timeout-check-interval=5
# 消费并发线程数，默认1
rediszsetq.consumer.concurrency=1
# 消费每次拉取消息数量，默认2
rediszsetq.consumer.fetch-count=2
# 最大重试次数，-1不限制，0不重试，>0 n次
max-retry-count=5
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
- concurrency: 并发线程数，非必须
- fetchCount: 拉取消息数，非必须，只有接收List生效
- restTimeIfConsumeNull: 消费消息为空时，休眠时间（秒），默认1

```java
import pers.rediszsetq.consumer.MessageListener;
import pers.rediszsetq.consumer.RedisZSetListener;
import pers.rediszsetq.consumer.Consumer;
import pers.rediszsetq.model.Message;
import org.springframework.stereotype.Component;

@Component
public class StringMessageListener implements MessageListener<String> {

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
