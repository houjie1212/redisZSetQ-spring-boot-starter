package cn.piesat.rediszsetq.model;

import java.util.Date;

public class MessageStatusRecord extends Message {

    private Date consumerStartTime;

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
        this.setConsumerStartTime(new Date());
    }

    public Date getConsumerStartTime() {
        return consumerStartTime;
    }

    public MessageStatusRecord setConsumerStartTime(Date consumerStartTime) {
        this.consumerStartTime = consumerStartTime;
        return this;
    }
}
