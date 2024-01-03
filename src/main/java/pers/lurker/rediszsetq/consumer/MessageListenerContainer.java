package pers.lurker.rediszsetq.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import pers.lurker.rediszsetq.config.RedisZSetQProperties;
import pers.lurker.rediszsetq.consumer.strategy.MultiThreadStrategy;
import pers.lurker.rediszsetq.consumer.strategy.SingleThreadStrategy;
import pers.lurker.rediszsetq.consumer.strategy.ThreadStrategy;
import pers.lurker.rediszsetq.model.Message;
import pers.lurker.rediszsetq.model.MessageGroup;
import pers.lurker.rediszsetq.producer.MessageProducer;
import pers.lurker.rediszsetq.util.DateUtil;
import pers.lurker.rediszsetq.util.JsonUtil;
import pers.lurker.rediszsetq.util.KeyUtil;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;

/**
 * 消息监听器管理容器
 */
@Component
@DependsOn("zsetQRedisBeanConfig")
public class MessageListenerContainer implements SmartLifecycle, ApplicationContextAware, EnvironmentAware, BeanFactoryAware {

    private static final Logger log = LoggerFactory.getLogger(MessageListenerContainer.class);

    private boolean isRunning;
    private ApplicationContext applicationContext;
    private Environment environment;
    private Set<String> queueNames = new HashSet<>();
    private List<ThreadStrategy> threadStrategies = new ArrayList<>();
    private Timer processingTaskTimer;
    private BeanExpressionContext beanExpressionContext;
    private BeanExpressionResolver resolver = new StandardBeanExpressionResolver();

    @Resource
    private List<MessageGroup> messageGroups;
    @Resource
    @Qualifier("zsetQRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private MessageProducer messageProducer;
    @Resource
    private RedisZSetQProperties redisZSetQProperties;

    @Override
    public void start() {
        log.info("rediszsetq properties: {}", JsonUtil.obj2String(redisZSetQProperties));
        log.info("MessageListenerContainer start");
        startMessageListeners();
        startRunningTaskListener();
        isRunning = true;
    }

    @Override
    public void stop() {
        log.info("MessageListenerContainer stop");
        isRunning = false;
        stopMessageListeners();
        stopRunningTaskListener();
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanExpressionContext = new BeanExpressionContext((ConfigurableBeanFactory) beanFactory, null);
    }

    /**
     * 启动消息消费监听器
     */
    private void startMessageListeners() {
        // 获取MessageListener类型的实例
        Map<String, MessageListener> messageListeners = applicationContext.getBeansOfType(MessageListener.class);
        messageListeners.forEach((k, v) -> {
            // 获取拉取单条消息方法
            Method onMessage = ReflectionUtils.findMethod(v.getClass(), "onMessage", Message.class, Consumer.class);
            if (onMessage != null) {
                RedisZSetListener onMessageAnnotation = AnnotationUtils.findAnnotation(onMessage, RedisZSetListener.class);
                // 判断方法是否有RedisZSetListener注解，且队列名不为空
                if (onMessageAnnotation != null && onMessageAnnotation.value() != null && onMessageAnnotation.value().length > 0) {
                    List<String> allValues = resolveListenerValue(onMessageAnnotation.value());

                    for (MessageGroup messageGroup : messageGroups) {
                        for (String value : allValues) {
                            int concurrencyValue = Integer.parseInt(environment.resolvePlaceholders(onMessageAnnotation.concurrency()));
                            int restTimeIfConsumeNullValue = Integer.parseInt(environment.resolvePlaceholders(onMessageAnnotation.restTimeIfConsumeNull()));
                            MessageConsumer messageConsumer = new MessageConsumer(applicationContext);
                            applicationContext.getAutowireCapableBeanFactory().autowireBean(messageConsumer);
                            int concurrency = concurrencyValue > 0 ? concurrencyValue :
                                redisZSetQProperties.getConsumer().getConcurrency();
                            messageConsumer.setMessageListener(v)
                                .setGroupName(messageGroup.getGroupName())
                                .setQueueName(value)
                                .setThreadStrategy(new SingleThreadStrategy(concurrency, restTimeIfConsumeNullValue))
                                .init();
                            threadStrategies.add(messageConsumer.getThreadStrategy());
                            queueNames.add(value);
                            log.info("启动消息监听器[{}].onMessage(T)，消费队列[{}:{}]", k, messageGroup.getGroupName(), value);
                        }
                    }
                }
            }
            // 获取拉取多条消息方法
            Method onMessageList = ReflectionUtils.findMethod(v.getClass(), "onMessage", List.class, Consumer.class);
            if (onMessageList != null) {
                RedisZSetListener onMessagesAnnotation = AnnotationUtils.findAnnotation(onMessageList, RedisZSetListener.class);
                // 判断方法是否有RedisZSetListener注解，且队列名不为空
                if (onMessagesAnnotation != null && onMessagesAnnotation.value() != null && onMessagesAnnotation.value().length > 0) {
                    List<String> allValues = resolveListenerValue(onMessagesAnnotation.value());

                    for (MessageGroup messageGroup : messageGroups) {
                        for (String value : allValues) {
                            int concurrencyValue = Integer.parseInt(environment.resolvePlaceholders(onMessagesAnnotation.concurrency()));
                            int fetchCountValue = Integer.parseInt(environment.resolvePlaceholders(onMessagesAnnotation.fetchCount()));
                            int restTimeIfConsumeNullValue =
                                Integer.parseInt(environment.resolvePlaceholders(onMessagesAnnotation.restTimeIfConsumeNull()));
                            MessageConsumer messageConsumer = new MessageConsumer(applicationContext);
                            applicationContext.getAutowireCapableBeanFactory().autowireBean(messageConsumer);
                            int concurrency = concurrencyValue > 0 ? concurrencyValue :
                                redisZSetQProperties.getConsumer().getConcurrency();
                            int fetchCount = fetchCountValue > 0 ? fetchCountValue :
                                redisZSetQProperties.getConsumer().getFetchCount();
                            messageConsumer.setMessageListener(v)
                                .setGroupName(messageGroup.getGroupName())
                                .setQueueName(value)
                                .setThreadStrategy(new MultiThreadStrategy(
                                    concurrency, restTimeIfConsumeNullValue, fetchCount))
                                .init();
                            threadStrategies.add(messageConsumer.getThreadStrategy());
                            queueNames.add(value);
                            log.info("启动消息监听器[{}].onMessage(List<T>)，消费队列[{}:{}]", k, messageGroup.getGroupName(), value);
                        }
                    }
                }
            }
        });
    }

    /**
     * 启动检查任务超时监听器
     */
    private void startRunningTaskListener() {
        if (threadStrategies.size() == 0) {
            return;
        }
        log.info("启动运行中任务监听器");
        processingTaskTimer = new Timer("rediszsetq-running-tasks");
        processingTaskTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (MessageGroup mg : messageGroups) {
                    for (String q : queueNames) {
                        List<Object> runningTasks = redisTemplate.boundListOps(KeyUtil.taskRunningKey(mg.getGroupName(), q)).range(0, 9);
                        runningTasks.forEach(value -> {
                            String taskId = (String) value;
                            String messageKey = KeyUtil.taskStatusKeyPrefix(mg.getGroupName(), q) + taskId;
                            Message message = (Message) redisTemplate.boundValueOps(messageKey).get();
                            Duration duration = Duration.ofMillis(DateUtil.getMilli(DateUtil.getNow()) - DateUtil.getMilli(message.getConsumerStartTime()));
                            int consumerTimeout = message.getConsumerTimeout() > 0 ? message.getConsumerTimeout() : redisZSetQProperties.getConsumer().getTimeout();
                            if (duration.getSeconds() > consumerTimeout) {
                                Long remove = redisTemplate.boundListOps(KeyUtil.taskRunningKey(mg.getGroupName(), q))
                                    .remove(0, value);
                                if (remove != null && remove > 0) {
                                    if (redisZSetQProperties.getConsumer().getMaxRetryCount() < 0 ||
                                        message.getRetryCount() < redisZSetQProperties.getConsumer().getMaxRetryCount()) {
                                        messageProducer.sendMessage(message.setRetryCount(message.getRetryCount() + 1));
                                        log.info("检测到队列[{}]的消息{}执行超时，重新入队", message.getQueueName(), message.getId());
                                    } else {
                                        // 是否需要记录？
                                        log.warn("检测到队列[{}]的消息{}已重试{}次，最多重试{}次，将被丢弃", message.getQueueName(), message.getId(),
                                            message.getRetryCount(), redisZSetQProperties.getConsumer().getMaxRetryCount());
                                    }
                                }
                            }
                        });
                    }
                }
            }
        }, 0, redisZSetQProperties.getConsumer().getTimeoutCheckInterval() * 1000L);
    }

    /**
     * 停止消息消费监听器
     */
    private void stopMessageListeners() {
        threadStrategies.forEach(ThreadStrategy::stop);
    }

    /**
     * 停止检查任务超时监听器
     */
    private void stopRunningTaskListener() {
        if (processingTaskTimer != null) {
            processingTaskTimer.cancel();
        }
    }

    private List<String> resolveListenerValue(String[] value) {
        List<String> result = new ArrayList<>();
        for (String queue : value) {
            Object evaluate = resolver.evaluate(environment.resolvePlaceholders(queue), beanExpressionContext);
            resolveAsString(evaluate, result);
        }
        return result;
    }

    private void resolveAsString(Object resolvedValue, List<String> result) {
        if (resolvedValue instanceof String[]) {
            for (Object object : (String[]) resolvedValue) {
                resolveAsString(object, result);
            }
        }
        else if (resolvedValue instanceof String) {
            result.add((String) resolvedValue);
        }
        else if (resolvedValue instanceof Iterable) {
            for (Object object : (Iterable<Object>) resolvedValue) {
                resolveAsString(object, result);
            }
        }
        else {
            throw new IllegalArgumentException(String.format(
                "@RedisZSetListener can't resolve '%s' as a String", resolvedValue));
        }
    }

}
