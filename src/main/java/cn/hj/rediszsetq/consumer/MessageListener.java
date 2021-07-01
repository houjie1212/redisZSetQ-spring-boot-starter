package cn.hj.rediszsetq.consumer;

import java.util.List;

public interface MessageListener<T> {

    void onMessage(T message);

    void onMessage(List<T> messages);
}
