package pers.lurker.rediszsetq.consumer.strategy;

import pers.lurker.rediszsetq.consumer.MessageListener;

public interface ThreadStrategy<T> {

    /**
     * 启动消费线程
     * @param queueName 队列名
     * @param messageListener 客户端监听器
     */
    void start(String groupName, String queueName, MessageListener<T> messageListener);

    /**
     * 停止消费线程
     */
    void stop();
}
