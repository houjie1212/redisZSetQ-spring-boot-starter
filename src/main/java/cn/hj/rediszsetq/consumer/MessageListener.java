package cn.hj.rediszsetq.consumer;

public interface MessageListener<T> {

    void onMessage(T message);
}
