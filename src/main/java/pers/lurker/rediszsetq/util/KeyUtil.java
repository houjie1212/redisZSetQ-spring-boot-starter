package pers.lurker.rediszsetq.util;

public class KeyUtil {

    public static final String ROOT = "ZSetQ:";
    public static final String TASK_RANK = "task-rank:";
    public static final String TASK_RUNNING = "task-running:";
    public static final String TASK_STATUS = "task-status:";
    public static final String TASK_LOG = "task-log:";

    public static String taskRankKey(String groupName, String queueName) {
        return ROOT + groupName + ":" + TASK_RANK + queueName;
    }

    public static String taskRunningKey(String groupName, String queueName) {
        return ROOT + groupName + ":" + TASK_RUNNING + queueName;
    }

    public static String taskStatusKeyPrefix(String groupName, String queueName) {
        return ROOT + groupName + ":" + TASK_STATUS + queueName + ":";
    }

    public static String taskLogKey(String groupName, String queueName) {
        return ROOT + groupName + ":" + TASK_LOG + queueName;
    }
}
