package pers.lurker.rediszsetq.consumer;

import pers.lurker.rediszsetq.model.Message;

import java.util.List;

/**
 * 消息监听器适配器
 * @param <T>
 */
public abstract class MessageListenerAdapter<T> implements MessageListener<T> {

    @Override
    public void onMessage(Message<T> message, Consumer<T> consumer) {}

    @Override
    public void onMessage(List<Message<T>> messages, Consumer<T> consumer) {}
}
