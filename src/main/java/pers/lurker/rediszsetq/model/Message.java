package pers.lurker.rediszsetq.model;

import pers.lurker.rediszsetq.util.DateUtil;
import pers.lurker.rediszsetq.util.JsonUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    /** 创建时间 */
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat( pattern="yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime creation;
    /** 已重试次数 */
    private int retryCount = 0;
    /** 消息内容 */
    private T payload;
    /** 分组名 */
    private String groupName;
    /** 队列名 */
    private String queueName;
    /** 优先级 */
    private int priority;
    /** 在队列中的过期时间 */
    private Integer expire;
    /** 消费超时时间 */
    private int consumerTimeout;
    /**
     * 消费状态
     * 0 待消费
     * 1 消费中
     */
    private int status;
    /** 开始消费时间 */
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat( pattern="yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime consumerStartTime;

    private Object extra;

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

    public String getGroupName() {
        return groupName;
    }

    public Message<T> setGroupName(String groupName) {
        this.groupName = groupName;
        return this;
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

    public Integer getExpire() {
        return expire;
    }

    public Message<T> setExpire(Integer expire) {
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

    public int getStatus() {
        return status;
    }

    public Message<T> setStatus(int status) {
        this.status = status;
        return this;
    }

    public LocalDateTime getConsumerStartTime() {
        return consumerStartTime;
    }

    public Message<T> setConsumerStartTime(LocalDateTime consumerStartTime) {
        this.consumerStartTime = consumerStartTime;
        return this;
    }

    public Object getExtra() {
        return extra;
    }

    public Message<T> setExtra(Object extra) {
        this.extra = extra;
        return this;
    }
}
