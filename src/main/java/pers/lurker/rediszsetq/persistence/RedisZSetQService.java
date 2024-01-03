package pers.lurker.rediszsetq.persistence;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pers.lurker.rediszsetq.model.MessageGroup.DEFAULT_GROUP;

@Component
public class RedisZSetQService {

    private final Map<String, RedisZSetQOps> redisZSetQOpsMap = new HashMap<>();

    public RedisZSetQService(List<RedisZSetQOps> redisZSetQOpsList) {
        redisZSetQOpsList.forEach(ops -> {
            redisZSetQOpsMap.put(ops.getGroupName(), ops);
        });
    }

    public RedisZSetQOps getByGroupName(String groupName) {
        return redisZSetQOpsMap.getOrDefault(groupName, redisZSetQOpsMap.get(DEFAULT_GROUP));
    }
}
