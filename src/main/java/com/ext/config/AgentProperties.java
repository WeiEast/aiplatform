package com.ext.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 智能体配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {
    private String id;
    private String name;
    private String description;
}
