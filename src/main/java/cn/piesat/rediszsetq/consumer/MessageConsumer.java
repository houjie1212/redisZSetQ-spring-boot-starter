package cn.piesat.rediszsetq.consumer;

import cn.piesat.rediszsetq.consumer.strategy.SingleThreadStrategy;
import cn.piesat.rediszsetq.consumer.strategy.ThreadStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class MessageConsumer<T> {

    private static final Logger log = LoggerFactory.getLogger(MessageConsumer.class);

    private final ApplicationContext applicationContext;

    private String queueName;
    private MessageListener<T> messageListener;

    private ThreadStrategy threadStrategy;

    public MessageConsumer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void init() {
        applicationContext.getAutowireCapableBeanFactory().autowireBean(threadStrategy);
        startConsumer();
    }

    public void startConsumer() {
        if (threadStrategy == null) {
            threadStrategy = new SingleThreadStrategy(1, 1);
        }
        threadStrategy.start(queueName, messageListener);
    }

    public MessageConsumer setQueueName(String queueName) {
        this.queueName = queueName;
        return this;
    }

    public MessageConsumer<T> setMessageListener(MessageListener<T> messageListener) {
        this.messageListener = messageListener;
        return this;
    }

    public MessageConsumer<T> setThreadStrategy(ThreadStrategy threadStrategy) {
        this.threadStrategy = threadStrategy;
        return this;
    }

    public ThreadStrategy getThreadStrategy() {
        return threadStrategy;
    }
}
