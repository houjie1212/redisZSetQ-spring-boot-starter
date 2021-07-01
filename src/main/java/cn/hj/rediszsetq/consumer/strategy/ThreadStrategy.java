package cn.hj.rediszsetq.consumer.strategy;

import cn.hj.rediszsetq.consumer.MessageListener;

public interface ThreadStrategy<T> {

    void start(String queueName, MessageListener<T> messageListener);
}
