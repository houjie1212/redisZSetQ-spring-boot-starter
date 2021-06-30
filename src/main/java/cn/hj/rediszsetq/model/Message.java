package cn.hj.rediszsetq.model;

import java.util.Calendar;

public class Message<T> {

    private String id;
    private Calendar creation;
    private Long timeToLiveSeconds;
    private int retryCount = 0;
    private T payload;

    public String getId() {
        return id;
    }

    public Message<T> setId(String id) {
        this.id = id;
        return this;
    }

    public Calendar getCreation() {
        return creation;
    }

    public Message<T> setCreation(Calendar creation) {
        this.creation = creation;
        return this;
    }

    public Long getTimeToLiveSeconds() {
        return timeToLiveSeconds;
    }

    public Message<T> setTimeToLiveSeconds(Long timeToLiveSeconds) {
        this.timeToLiveSeconds = timeToLiveSeconds;
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
                .setCreation(Calendar.getInstance())
                .setPayload(payload);
    }
}
