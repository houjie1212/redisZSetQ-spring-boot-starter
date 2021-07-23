package cn.piesat.rediszsetq.consumer.strategy;

import cn.piesat.rediszsetq.consumer.MessageListener;

public interface ThreadStrategy<T> {

    String PROCESSING_TASKS_QNAME = "ZSetQ-processing-tasks:";

    void start(String queueName, MessageListener<T> messageListener);

    void stop();
}
