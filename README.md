# redisZSetQ
用Redis Sorted-Set实现的优先级消息队列，可靠消费

## Maven Dependency
Clone this repo to local and add dependency.
```xml
<dependency>
    <groupId>cn.piesat</groupId>
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
### Enable Annotation

```java
import cn.rediszsetq.EnableRedisZSetQ;
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
- concurrency: 并发线程数，非必须
- fetchCount: 拉取消息数，非必须，只有接收List生效
- restTimeIfConsumeNull: 消费消息为空时，休眠时间（秒），默认1

```java
import MessageListenerAdapter;
import RedisZSetListener;
import Consumer;
import Message;
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
