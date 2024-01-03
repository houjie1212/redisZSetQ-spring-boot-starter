package pers.lurker.rediszsetq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import pers.lurker.rediszsetq.model.MessageGroup;
import pers.lurker.rediszsetq.persistence.RedisZSetQOps;
import pers.lurker.rediszsetq.util.JsonUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static pers.lurker.rediszsetq.model.MessageGroup.DEFAULT_GROUP;

/**
 * 自定义Bean声明
 */
@Configuration("zsetQRedisBeanConfig")
@EnableConfigurationProperties({RedisZSetQProperties.class})
@ComponentScan("pers.lurker.rediszsetq")
public class BeanConfig implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private Environment environment;
    public static final String BEANNAME_MESSAGEGROUP_SUFFIX = "MessageGroup";
    public static final String BEANNAME_REDISZSETQOPS_SUFFIX = "RedisZSetQOps";

    @Bean
    @ConditionalOnMissingBean
    public JsonUtil jsonUtil() {
        return new JsonUtil();
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        // 获取RedisZSetQProperties类注解中prefix的值
        Object prefix = AnnotationUtils.getAnnotationAttributes(
            AnnotationUtils.findAnnotation(RedisZSetQProperties.class, ConfigurationProperties.class)
        ).get("prefix");

        BindResult<RedisZSetQProperties> propertiesBindResult = Binder.get(environment)
            .bind(prefix.toString(), RedisZSetQProperties.class);
        if (propertiesBindResult.isBound()) {
            List<MessageGroup> messageGroups = propertiesBindResult.get().getMessageGroups();
            List<MessageGroup> trimmedGroups = messageGroups.stream()
                .peek(g -> StringUtils.trimAllWhitespace(g.getGroupName()))
                .filter(g -> StringUtils.hasText(g.getGroupName()))
                .collect(Collectors.toList());

            if (trimmedGroups.isEmpty()) {
                trimmedGroups.add(new MessageGroup().setGroupName(DEFAULT_GROUP));
            }
            trimmedGroups.forEach(g -> {
                registerMessageGroupBean(registry, g);
                registerRedisZSetQOpsBean(registry, g);
            });
        } else {
            MessageGroup defaultGroup = new MessageGroup().setGroupName(DEFAULT_GROUP);
            registerMessageGroupBean(registry, defaultGroup);
            registerRedisZSetQOpsBean(registry, defaultGroup);
        }
    }

    private void registerMessageGroupBean(BeanDefinitionRegistry registry, MessageGroup g) {
        if (g == null) {
            g = new MessageGroup().setGroupName(DEFAULT_GROUP);
        }
        RootBeanDefinition mgbd = new RootBeanDefinition(MessageGroup.class, g::self);
        registry.registerBeanDefinition(g.getGroupName() + BEANNAME_MESSAGEGROUP_SUFFIX, mgbd);
        log.info("注册 MessageGroup 实例, group: {}", g.getGroupName());
    }

    private void registerRedisZSetQOpsBean(BeanDefinitionRegistry registry, MessageGroup g) {
        if (g == null) {
            g = new MessageGroup().setGroupName(DEFAULT_GROUP);
        }
        RootBeanDefinition opsbd = new RootBeanDefinition(RedisZSetQOps.class);
        opsbd.setInstanceSupplier(new RedisZSetQOps(g.getGroupName())::self);
        registry.registerBeanDefinition(g.getGroupName() + BEANNAME_REDISZSETQOPS_SUFFIX, opsbd);
        log.info("注册 RedisZSetQOps 实例, group: {}", g.getGroupName());
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        RedisTemplate<String, Object> zsetQRedisTemplate = beanFactory.getBean("zsetQRedisTemplate", RedisTemplate.class);
        Map<String, RedisZSetQOps> zSetQOpsMap = beanFactory.getBeansOfType(RedisZSetQOps.class);
        zSetQOpsMap.values().forEach(o -> o.setRedisTemplate(zsetQRedisTemplate));
        log.info("{}", zsetQRedisTemplate);
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
