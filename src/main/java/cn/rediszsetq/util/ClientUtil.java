package cn.rediszsetq.util;

import cn.rediszsetq.config.RedisZSetQConsumerProperties;
import org.springframework.util.StringUtils;

public class ClientUtil {

    private static String applicationName;
    private static RedisZSetQConsumerProperties redisZSetQConsumerProperties;

    public ClientUtil(String applicationName, RedisZSetQConsumerProperties redisZSetQConsumerProperties) {
        ClientUtil.applicationName = applicationName;
        ClientUtil.redisZSetQConsumerProperties = redisZSetQConsumerProperties;
    }

    /**
     * 获取客户端应用名，设置监听进行中任务的队列名
     * @return
     */
    public static String getClientName() {
        if (StringUtils.hasText(redisZSetQConsumerProperties.getTimeoutCheckGroup())) {
            return redisZSetQConsumerProperties.getTimeoutCheckGroup();
        }
        if (StringUtils.hasText(applicationName)) {
            return applicationName;
        }
        return "default";
    }
}
