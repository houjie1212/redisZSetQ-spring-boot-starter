package pers.lurker.rediszsetq.util;

import org.springframework.util.StringUtils;
import pers.lurker.rediszsetq.config.RedisZSetQProperties;

public class ClientUtil {

    private static String applicationName;
    private static RedisZSetQProperties redisZSetQProperties;

    public ClientUtil(String applicationName, RedisZSetQProperties redisZSetQProperties) {
        ClientUtil.applicationName = applicationName;
        ClientUtil.redisZSetQProperties = redisZSetQProperties;
    }

    /**
     * 获取客户端应用名，设置监听进行中任务的队列名
     * @return
     */
    public static String getClientName() {
        if (StringUtils.hasText(redisZSetQProperties.getConsumer().getTimeoutCheckGroup())) {
            return redisZSetQProperties.getConsumer().getTimeoutCheckGroup();
        }
        if (StringUtils.hasText(applicationName)) {
            return applicationName;
        }
        return "default";
    }
}
