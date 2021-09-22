package pers.rediszsetq.consumer;

import pers.rediszsetq.config.RedisZSetQConsumerProperties;
import pers.rediszsetq.model.Message;
import pers.rediszsetq.producer.MessageProducer;
import pers.rediszsetq.consumer.strategy.MultiThreadStrategy;
import pers.rediszsetq.consumer.strategy.SingleThreadStrategy;
import pers.rediszsetq.consumer.strategy.ThreadStrategy;
import pers.rediszsetq.model.MessageStatusRecord;
import pers.rediszsetq.util.ClientUtil;
import pers.rediszsetq.util.DateUtil;
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
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;

/**
 * 消息监听器管理容器
 */
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
        startProcessingTaskListener();
        isRunning = true;
    }

    @Override
    public void stop() {
        log.info("MessageListenerContainer stop");
        isRunning = false;
        stopMessageListeners();
        stopProcessingTaskListener();
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private void startMessageListeners() {
        Map<String, MessageListener> messageListeners = applicationContext.getBeansOfType(MessageListener.class);
        messageListeners.forEach((k, v) -> {

            Method onMessage = ReflectionUtils.findMethod(v.getClass(), "onMessage", Message.class, Consumer.class);
            if (onMessage != null) {
                RedisZSetListener onMessageAnnotation = AnnotationUtils.findAnnotation(onMessage, RedisZSetListener.class);
                if (onMessageAnnotation != null && StringUtils.hasText(onMessageAnnotation.value())) {
                    MessageConsumer messageConsumer = new MessageConsumer(applicationContext);
                    applicationContext.getAutowireCapableBeanFactory().autowireBean(messageConsumer);
                    int concurrency = onMessageAnnotation.concurrency() > 0 ?
                            onMessageAnnotation.concurrency() : redisZSetQConsumerProperties.getConcurrency();
                    messageConsumer.setMessageListener(v)
                            .setQueueName(onMessageAnnotation.value())
                            .setThreadStrategy(new SingleThreadStrategy(
                                    concurrency, onMessageAnnotation.restTimeIfConsumeNull()))
                            .init();
                    threadStrategies.add(messageConsumer.getThreadStrategy());
                    log.info("启动消息监听器[{}].onMessage(T)，消费队列[{}]", k, onMessageAnnotation.value());
                }
            }

            Method onMessageList = ReflectionUtils.findMethod(v.getClass(), "onMessage", List.class, Consumer.class);
            if (onMessageList != null) {
                RedisZSetListener onMessagesAnnotation = AnnotationUtils.findAnnotation(onMessageList, RedisZSetListener.class);
                if (onMessagesAnnotation != null && StringUtils.hasText(onMessagesAnnotation.value())) {
                    MessageConsumer messageConsumer = new MessageConsumer(applicationContext);
                    applicationContext.getAutowireCapableBeanFactory().autowireBean(messageConsumer);
                    int concurrency = onMessagesAnnotation.concurrency() > 0 ?
                            onMessagesAnnotation.concurrency() : redisZSetQConsumerProperties.getConcurrency();
                    int fetchCount = onMessagesAnnotation.fetchCount() > 0 ?
                            onMessagesAnnotation.fetchCount() : redisZSetQConsumerProperties.getFetchCount();
                    messageConsumer.setMessageListener(v)
                            .setQueueName(onMessagesAnnotation.value())
                            .setThreadStrategy(new MultiThreadStrategy(
                                    concurrency, onMessagesAnnotation.restTimeIfConsumeNull(), fetchCount))
                            .init();
                    threadStrategies.add(messageConsumer.getThreadStrategy());
                    log.info("启动消息监听器[{}].onMessage(List<T>)，消费队列[{}]", k, onMessagesAnnotation.value());
                }
            }
        });
    }

    private void startProcessingTaskListener() {
        if (threadStrategies.size() == 0) {
            return;
        }
        processingTaskTimer = new Timer("rediszsetq-processing-tasks");
        String processQName = ThreadStrategy.PROCESSING_TASKS_QNAME + ClientUtil.getClientName();
        processingTaskTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                List<Object> processingTasks = redisTemplate.opsForList().range(processQName, 0, 9);
                processingTasks.forEach(task -> {
                    MessageStatusRecord msr = (MessageStatusRecord) task;
                    Duration duration = Duration.ofMillis(DateUtil.getMilli(DateUtil.getNow()) - DateUtil.getMilli(msr.getConsumerStartTime()));
                    int consumerTimeout = msr.getConsumerTimeout() > 0 ? msr.getConsumerTimeout() : redisZSetQConsumerProperties.getTimeout();
                    if (duration.getSeconds() > consumerTimeout) {
                        log.info("检测到队列[{}]的消息{}执行超时，重新入队", msr.getQueueName(), msr.getPayload());
                        Long remove = redisTemplate.opsForList().remove(processQName, 0, msr);
                        if (remove != null && remove > 0) {
                            if (redisZSetQConsumerProperties.getMaxRetryCount() < 0 ||
                                    msr.getRetryCount() < redisZSetQConsumerProperties.getMaxRetryCount()) {
                                messageProducer.sendMessage(msr.setRetryCount(msr.getRetryCount() + 1));
                                log.info("检测到队列[{}]的消息{}执行超时，重新入队", msr.getQueueName(), msr.getId());
                            } else {
                                // 是否需要记录？
                                log.warn("检测到队列[{}]的消息{}已重试{}次，最多重试{}次，将被丢弃", msr.getQueueName(), msr.getId(),
                                        msr.getRetryCount(), redisZSetQConsumerProperties.getMaxRetryCount());
                            }
                        }
                    }
                });
            }
        }, 0, redisZSetQConsumerProperties.getTimeoutCheckInterval() * 1000L);
    }

    private void stopMessageListeners() {
        threadStrategies.forEach(ThreadStrategy::stop);
    }

    private void stopProcessingTaskListener() {
        if (processingTaskTimer != null) {
            processingTaskTimer.cancel();
        }
    }

}
