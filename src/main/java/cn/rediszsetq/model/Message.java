package cn.rediszsetq.model;

import cn.rediszsetq.util.DateUtil;
import cn.rediszsetq.util.JsonUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message<T> implements Serializable {

    private static final long serialVersionUID = 6402629253063362398L;

    private String id;
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat( pattern="yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime creation;
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

    public LocalDateTime getCreation() {
        return creation;
    }

    public Message<T> setCreation(LocalDateTime creation) {
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
                .setCreation(DateUtil.getNow())
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
