package cn.hj.rediszsetq.consumer;

import java.util.List;

public abstract class AbstractMessageListener<T> implements MessageListener<T> {

    @Override
    public void onMessage(T message) {

    }

    @Override
    public void onMessage(List<T> messages) {

    }
}
