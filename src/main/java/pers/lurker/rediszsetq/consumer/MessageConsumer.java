package pers.lurker.rediszsetq.consumer;

import pers.lurker.rediszsetq.consumer.strategy.ThreadStrategy;
import pers.lurker.rediszsetq.consumer.strategy.SingleThreadStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class MessageConsumer<T> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ApplicationContext applicationContext;

    private String groupName;
    private String queueName;
    private MessageListener<T> messageListener;
    private ThreadStrategy threadStrategy;

    public MessageConsumer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 初始化属性并启动
     */
    public void init() {
        applicationContext.getAutowireCapableBeanFactory().autowireBean(threadStrategy);
        startConsumer();
    }

    /**
     * 启动消费线程
     */
    public void startConsumer() {
        if (threadStrategy == null) {
            threadStrategy = new SingleThreadStrategy(1, 1);
        }
        threadStrategy.start(groupName, queueName, messageListener);
    }

    public MessageConsumer<T> setGroupName(String groupName) {
        this.groupName = groupName;
        return this;
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
