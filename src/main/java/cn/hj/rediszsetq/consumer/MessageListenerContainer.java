package cn.hj.rediszsetq.consumer;

import cn.hj.rediszsetq.consumer.strategy.MultiThreadStrategy;
import cn.hj.rediszsetq.consumer.strategy.SingleThreadStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

@Component
public class MessageListenerContainer implements SmartLifecycle, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(MessageListenerContainer.class);

    private ApplicationContext applicationContext;

    @Override
    public void start() {
        Map<String, MessageListener> messageListeners = applicationContext.getBeansOfType(MessageListener.class);
        messageListeners.forEach((k, v) -> {
            ParameterizedType pt = (ParameterizedType) v.getClass().getGenericSuperclass();
            Class<?> cls = (Class) pt.getActualTypeArguments()[0];

            Method onMessage = ReflectionUtils.findMethod(v.getClass(), "onMessage", cls);
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
                    log.info("启动消息监听器[{}].onMessage(T)，消费队列[{}]", k, onMessageAnnotation.value());
                }
            }

            Method onMessageList = ReflectionUtils.findMethod(v.getClass(), "onMessage", List.class);
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
}
