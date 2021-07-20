package cn.piesat.rediszsetq.consumer;

import cn.piesat.rediszsetq.config.RedisZSetQConsumerProperties;
import cn.piesat.rediszsetq.consumer.strategy.MultiThreadStrategy;
import cn.piesat.rediszsetq.consumer.strategy.SingleThreadStrategy;
import cn.piesat.rediszsetq.consumer.strategy.ThreadStrategy;
import cn.piesat.rediszsetq.model.Message;
import cn.piesat.rediszsetq.model.MessageStatusRecord;
import cn.piesat.rediszsetq.producer.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;

@Component
public class MessageListenerContainer implements SmartLifecycle, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(MessageListenerContainer.class);

    private boolean isRunning;
    private ApplicationContext applicationContext;
    private List<ThreadStrategy> threadStrategies = new ArrayList<>();
    private Timer processingTaskTimer;

    @Autowired
    @Qualifier("zsetQRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private MessageProducer messageProducer;
    @Autowired
    private RedisZSetQConsumerProperties redisZSetQConsumerProperties;

    @Override
    public void start() {
        log.info("MessageListenerContainer start");
        startMessageListeners();
        processingTaskTimer = startProcessingTaskListener();
        isRunning = true;
    }

    @Override
    public void stop() {
        log.info("MessageListenerContainer stop");
        isRunning = false;
        threadStrategies.forEach(ThreadStrategy::stop);
        processingTaskTimer.cancel();
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private Timer startProcessingTaskListener() {
        Timer timer = new Timer("rediszsetq-processing-tasks");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                List<Object> processingTasks = redisTemplate.opsForList().range(ThreadStrategy.PROCESSING_TASKS_QNAME, 0, 9);
                processingTasks.forEach(task -> {
                    MessageStatusRecord msr = (MessageStatusRecord) task;
                    Duration duration = Duration.ofMillis(new Date().getTime() - msr.getConsumerStartTime().getTime());
                    int consumerTimeout = msr.getConsumerTimeout() > 0 ? msr.getConsumerTimeout() : redisZSetQConsumerProperties.getTimeout();
                    if (duration.getSeconds() > consumerTimeout) {
                        log.info("检测到队列[{}]的消息{}执行超时，重新入队", msr.getQueueName(), msr.getPayload());
                        Long remove = redisTemplate.opsForList().remove(ThreadStrategy.PROCESSING_TASKS_QNAME, 0, msr);
                        if (remove != null && remove > 0) {
                            messageProducer.sendMessage(msr.setRetryCount(msr.getRetryCount() + 1));
                        }
                    }
                });
            }
        }, 0, redisZSetQConsumerProperties.getTimeoutCheckInterval() * 1000L);
        return timer;
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
                    threadStrategies.add(messageConsumer.getThreadStrategy());
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
                    threadStrategies.add(messageConsumer.getThreadStrategy());
                    log.info("启动消息监听器[{}].onMessage(List<T>)，消费队列[{}]", k, onMessagesAnnotation.value());
                }
            }
        });
    }
}
