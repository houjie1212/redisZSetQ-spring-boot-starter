package pers.rediszsetq.model;

import pers.rediszsetq.util.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;

public class MessageStatusRecord extends Message {

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat( pattern="yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime consumerStartTime;

    public MessageStatusRecord() {
    }

    public MessageStatusRecord(Message message) {
        this.setId(message.getId());
        this.setCreation(message.getCreation());
        this.setRetryCount(message.getRetryCount());
        this.setPayload(message.getPayload());
        this.setQueueName(message.getQueueName());
        this.setPriority(message.getPriority());
        this.setExpire(message.getExpire());
        this.setConsumerStartTime(DateUtil.getNow());
        this.setConsumerTimeout(message.getConsumerTimeout());
    }

    public LocalDateTime getConsumerStartTime() {
        return consumerStartTime;
    }

    public MessageStatusRecord setConsumerStartTime(LocalDateTime consumerStartTime) {
        this.consumerStartTime = consumerStartTime;
        return this;
    }
}
