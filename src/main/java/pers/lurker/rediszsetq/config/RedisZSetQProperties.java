package pers.lurker.rediszsetq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pers.lurker.rediszsetq.model.MessageGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端配置信息
 */
@ConfigurationProperties(prefix = "rediszsetq")
public class RedisZSetQProperties {

    private int snowflakeWorkerId = 1;
    private Consumer consumer = new Consumer();
    private List<MessageGroup> messageGroups = new ArrayList<>();

    public int getSnowflakeWorkerId() {
        return snowflakeWorkerId;
    }

    public RedisZSetQProperties setSnowflakeWorkerId(int snowflakeWorkerId) {
        this.snowflakeWorkerId = snowflakeWorkerId;
        return this;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public RedisZSetQProperties setConsumer(Consumer consumer) {
        this.consumer = consumer;
        return this;
    }

    public List<MessageGroup> getMessageGroups() {
        return messageGroups;
    }

    public RedisZSetQProperties setMessageGroups(List<MessageGroup> messageGroups) {
        this.messageGroups = messageGroups;
        return this;
    }

    public static class Consumer {
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
        private int maxRetryCount = -1;
        /**
         * 是否记录日志
         */
        private boolean logEnabled;

        public int getTimeout() {
            return timeout;
        }

        public Consumer setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public int getTimeoutCheckInterval() {
            return timeoutCheckInterval;
        }

        public Consumer setTimeoutCheckInterval(int timeoutCheckInterval) {
            this.timeoutCheckInterval = timeoutCheckInterval;
            return this;
        }

        public String getTimeoutCheckGroup() {
            return timeoutCheckGroup;
        }

        public Consumer setTimeoutCheckGroup(String timeoutCheckGroup) {
            this.timeoutCheckGroup = timeoutCheckGroup;
            return this;
        }

        public int getConcurrency() {
            return concurrency;
        }

        public Consumer setConcurrency(int concurrency) {
            this.concurrency = concurrency;
            return this;
        }

        public int getFetchCount() {
            return fetchCount;
        }

        public Consumer setFetchCount(int fetchCount) {
            this.fetchCount = fetchCount;
            return this;
        }

        public int getMaxRetryCount() {
            return maxRetryCount;
        }

        public Consumer setMaxRetryCount(int maxRetryCount) {
            this.maxRetryCount = maxRetryCount;
            return this;
        }

        public boolean isLogEnabled() {
            return logEnabled;
        }

        public Consumer setLogEnabled(boolean logEnabled) {
            this.logEnabled = logEnabled;
            return this;
        }
    }

}
