package pers.rediszsetq.consumer;

import pers.rediszsetq.model.Message;

import java.util.List;

public interface MessageListener<T> {

    default void onMessage(Message<T> message, Consumer<T> consumer) {
    }

    default void onMessage(List<Message<T>> messages, Consumer<T> consumer) {
    }
}
