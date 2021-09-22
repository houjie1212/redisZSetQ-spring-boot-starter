package pers.rediszsetq.consumer.strategy;

import pers.rediszsetq.consumer.MessageListener;

public interface ThreadStrategy<T> {

    String PROCESSING_TASKS_QNAME = "ZSetQ-processing-tasks:";
    String LOG_TASKS_QNAME = "ZSetQ-log-tasks:";

    void start(String queueName, MessageListener<T> messageListener);

    void stop();
}
