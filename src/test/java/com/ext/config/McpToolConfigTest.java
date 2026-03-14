package com.ext.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * MCP 工具配置测试
 * 验证 MCP 工具初始化
 */
@DisplayName("MCP 工具配置测试")
@SpringBootTest
class McpToolConfigTest {

    @Test
    @DisplayName("上下文加载 - 不抛异常")
    void testContextLoads() {
        // 简单的烟雾测试，确保配置类可以加载
        assertDoesNotThrow(() -> {
            // Spring 会自动配置所有@Component 类
            // 如果能执行到这里，说明配置加载成功
        });
    }
}
