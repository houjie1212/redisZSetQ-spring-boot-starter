package cn.rediszsetq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rediszsetq.consumer")
public class RedisZSetQConsumerProperties {

    /**
     * 任务执行超时时间（秒）
     */
    private int timeout = 3600;
    /**
     * 任务执行超时检查时间间隔（秒）
     */
    private int timeoutCheckInterval = 5;
    /**
     * 任务执行超时检查队列名
     */
    private String timeoutCheckGroup;
    /**
     * 并发线程数
     */
    private int concurrency = 1;
    /**
     * 拉取消息数量
     */
    private int fetchCount = 2;
    /**
     * 最大重试次数，-1不限制，0不重试，>0 n次
     */
    private int maxRetryCount = 5;

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

    public String getTimeoutCheckGroup() {
        return timeoutCheckGroup;
    }

    public RedisZSetQConsumerProperties setTimeoutCheckGroup(String timeoutCheckGroup) {
        this.timeoutCheckGroup = timeoutCheckGroup;
        return this;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public RedisZSetQConsumerProperties setConcurrency(int concurrency) {
        this.concurrency = concurrency;
        return this;
    }

    public int getFetchCount() {
        return fetchCount;
    }

    public RedisZSetQConsumerProperties setFetchCount(int fetchCount) {
        this.fetchCount = fetchCount;
        return this;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public RedisZSetQConsumerProperties setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
        return this;
    }
}
