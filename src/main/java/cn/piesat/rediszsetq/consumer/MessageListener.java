package cn.piesat.rediszsetq.consumer;

import cn.piesat.rediszsetq.model.Message;

import java.util.List;

public interface MessageListener<T> {

    void onMessage(Message<T> message, Consumer consumer);

    void onMessage(List<Message<T>> messages, Consumer consumer);
}
