package pers.lurker.rediszsetq.model;

import java.util.Map;

public class MessageGroup {

    public static final String DEFAULT_GROUP = "default";

    private String groupName;
    private Map<String, String> properties;

    public MessageGroup self() {
        return this;
    }

    public String getGroupName() {
        return groupName;
    }

    public MessageGroup setGroupName(String groupName) {
        this.groupName = groupName;
        return this;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public MessageGroup setProperties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }
}
