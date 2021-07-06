package cn.piesat.rediszsetq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rediszsetq.consumer")
public class RedisZSetQConsumerProperties {

    private int timeout = 60;
    private int timeoutCheckInterval = 5;

    public int getTimeout() {
        return timeout;
    }

    public RedisZSetQConsumerProperties setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public int getTimeoutCheckInterval() {
        return timeoutCheckInterval;
    }

    public RedisZSetQConsumerProperties setTimeoutCheckInterval(int timeoutCheckInterval) {
        this.timeoutCheckInterval = timeoutCheckInterval;
        return this;
    }
}
