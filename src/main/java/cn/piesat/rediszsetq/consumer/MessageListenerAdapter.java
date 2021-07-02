package cn.piesat.rediszsetq.consumer;

import cn.piesat.rediszsetq.model.Message;

import java.util.List;

public abstract class MessageListenerAdapter<T> implements MessageListener<T> {

    @Override
    public void onMessage(Message<T> message, Consumer consumer) {

    }

    @Override
    public void onMessage(List<Message<T>> messages, Consumer consumer) {

    }
}
