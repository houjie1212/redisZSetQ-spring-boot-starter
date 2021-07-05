package cn.piesat.rediszsetq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rediszsetq")
public class RedisZSetQProperties {

    private int consumerTimeout = 60;

    public int getConsumerTimeout() {
        return consumerTimeout;
    }

    public RedisZSetQProperties setConsumerTimeout(int consumerTimeout) {
        this.consumerTimeout = consumerTimeout;
        return this;
    }
}
