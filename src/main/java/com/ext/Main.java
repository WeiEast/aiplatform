package com.ext;

import com.ext.service.AgentConfigLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        
        // 加载所有智能体配置
        try {
            AgentConfigLoader loader = context.getBean(AgentConfigLoader.class);
            loader.loadAllAgents();
            System.out.println("已加载 " + loader.getAllAgentConfigs().size() + " 个智能体");
        } catch (Exception e) {
            System.err.println("加载智能体配置失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}
