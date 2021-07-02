package cn.piesat.rediszsetq.consumer;

import cn.piesat.rediszsetq.consumer.strategy.MultiThreadStrategy;
import cn.piesat.rediszsetq.consumer.strategy.SingleThreadStrategy;
import cn.piesat.rediszsetq.consumer.strategy.ThreadStrategy;
import cn.piesat.rediszsetq.consumer.thread.ProcessingTaskListenerThread;
import cn.piesat.rediszsetq.model.Message;
import cn.piesat.rediszsetq.model.MessageStatusRecord;
import cn.piesat.rediszsetq.producer.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class MessageListenerContainer implements SmartLifecycle, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(MessageListenerContainer.class);

    private ApplicationContext applicationContext;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private MessageProducer messageProducer;

    @Override
    public void start() {
        startMessageListeners();
        startProcessingTaskListener();
    }

    @Override
    public void stop() {
        log.info("MessageListenerContainer stop");
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private void startProcessingTaskListener() {
        ProcessingTaskListenerThread processingTaskListenerThread = new ProcessingTaskListenerThread(() -> {
            List<Object> processingTasks = redisTemplate.opsForList().range(ThreadStrategy.PROCESSING_TASKS_QNAME, 0, 9);
            processingTasks.forEach(task -> {
                MessageStatusRecord msr = (MessageStatusRecord) task;
                Duration duration = Duration.ofMillis(new Date().getTime() - msr.getConsumerStartTime().getTime());
                if (duration.getSeconds() > 60) {
                    log.info("检测到队列[{}]的消息{}执行超时，重新入队", msr.getQueueName(), msr.getPayload());
                    redisTemplate.opsForList().remove(ThreadStrategy.PROCESSING_TASKS_QNAME, 0, msr);
                    messageProducer.sendMessage(msr.setRetryCount(msr.getRetryCount() + 1));
                }
            });
        });
        processingTaskListenerThread.setName("rediszsetq-processing-tasks");
        processingTaskListenerThread.start();
    }

    private void startMessageListeners() {
        Map<String, MessageListener> messageListeners = applicationContext.getBeansOfType(MessageListener.class);
        messageListeners.forEach((k, v) -> {

            Method onMessage = ReflectionUtils.findMethod(v.getClass(), "onMessage", Message.class, Consumer.class);
            if (onMessage != null) {
                RedisZSetListener onMessageAnnotation = AnnotationUtils.findAnnotation(onMessage, RedisZSetListener.class);
                if (onMessageAnnotation == null) {
                    log.warn("[{}]的方法onMessage没有添加RedisZSetListener注解，不会启动监听器", k);
                } else {
                    MessageConsumer messageConsumer = new MessageConsumer(applicationContext);
                    applicationContext.getAutowireCapableBeanFactory().autowireBean(messageConsumer);
                    messageConsumer.setMessageListener(v)
                            .setQueueName(onMessageAnnotation.value())
                            .setThreadStrategy(new SingleThreadStrategy(onMessageAnnotation.concurrency()))
                            .init();
                    Constructor<?>[] constructors = SingleThreadStrategy.class.getConstructors();
                    log.info("启动消息监听器[{}].onMessage(T)，消费队列[{}]", k, onMessageAnnotation.value());
                }
            }

            Method onMessageList = ReflectionUtils.findMethod(v.getClass(), "onMessage", List.class, Consumer.class);
            if (onMessageList != null) {
                RedisZSetListener onMessagesAnnotation = AnnotationUtils.findAnnotation(onMessageList, RedisZSetListener.class);
                if (onMessagesAnnotation == null) {
                    log.warn("[{}]的方法onMessage没有添加RedisZSetListener注解，不会启动监听器", k);
                } else {
                    MessageConsumer messageConsumer = new MessageConsumer(applicationContext);
                    applicationContext.getAutowireCapableBeanFactory().autowireBean(messageConsumer);
                    messageConsumer.setMessageListener(v)
                            .setQueueName(onMessagesAnnotation.value())
                            .setThreadStrategy(new MultiThreadStrategy(onMessagesAnnotation.concurrency(), onMessagesAnnotation.fetchCount()))
                            .init();
                    log.info("启动消息监听器[{}].onMessage(List<T>)，消费队列[{}]", k, onMessagesAnnotation.value());
                }
            }
        });
    }
}
