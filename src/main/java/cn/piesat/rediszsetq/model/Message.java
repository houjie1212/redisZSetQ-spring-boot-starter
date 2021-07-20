package cn.piesat.rediszsetq.model;

import cn.piesat.rediszsetq.util.JsonUtil;

import java.io.Serializable;
import java.util.Date;

public class Message<T> implements Serializable {

    private static final long serialVersionUID = 6402629253063362398L;

    private String id;
    private Date creation;
    private int retryCount = 0;
    private T payload;
    private String queueName;
    private int priority;
    private int expire;
    private int consumerTimeout;

    @Override
    public String toString() {
        return JsonUtil.obj2String(this);
    }

    public String getId() {
        return id;
    }

    public Message<T> setId(String id) {
        this.id = id;
        return this;
    }

    public Date getCreation() {
        return creation;
    }

    public Message<T> setCreation(Date creation) {
        this.creation = creation;
        return this;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Message<T> setRetryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public T getPayload() {
        return payload;
    }

    public Message<T> setPayload(T payload) {
        this.payload = payload;
        return this;
    }

    public static <T> Message<T> create(String id, T payload) {
        return new Message<T>()
                .setId(id)
                .setCreation(new Date())
                .setPayload(payload);
    }

    public String getQueueName() {
        return queueName;
    }

    public Message<T> setQueueName(String queueName) {
        this.queueName = queueName;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    public Message<T> setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public int getExpire() {
        return expire;
    }

    public Message<T> setExpire(int expire) {
        this.expire = expire;
        return this;
    }

    public int getConsumerTimeout() {
        return consumerTimeout;
    }

    public Message<T> setConsumerTimeout(int consumerTimeout) {
        this.consumerTimeout = consumerTimeout;
        return this;
    }
}
