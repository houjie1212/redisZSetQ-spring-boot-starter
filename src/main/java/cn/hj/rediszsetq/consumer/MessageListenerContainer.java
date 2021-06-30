package cn.hj.rediszsetq.consumer;

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
import java.util.Map;

@Component
public class MessageListenerContainer implements SmartLifecycle, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(MessageListenerContainer.class);

    private ApplicationContext applicationContext;

    @Override
    public void start() {
        Map<String, MessageListener> messageListeners = applicationContext.getBeansOfType(MessageListener.class);
        messageListeners.forEach((k, v) -> {
            ParameterizedType pt = (ParameterizedType) v.getClass().getGenericInterfaces()[0];
            Class<?> cls = (Class) pt.getActualTypeArguments()[0];
            Method onMessage = ReflectionUtils.findMethod(v.getClass(), "onMessage", cls);
            RedisZSetListener redisZSetListener = AnnotationUtils.findAnnotation(onMessage, RedisZSetListener.class);
            if (redisZSetListener == null) {
                log.warn("[{}]的方法没有添加RedisZSetListener注解，不会启动监听器", k);
                return;
            }

            MessageConsumer messageConsumer = new MessageConsumer();
            applicationContext.getAutowireCapableBeanFactory().autowireBean(messageConsumer);
            messageConsumer.setMessageListener(v);
            messageConsumer.setQueueName(redisZSetListener.value());
            messageConsumer.init();
            log.info("启动消息监听器[{}]，消费队列[{}]", k, redisZSetListener.value());
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
