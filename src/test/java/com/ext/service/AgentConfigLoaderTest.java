package com.ext.service;

import com.ext.model.AgentConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 智能体配置加载器测试
 */
class AgentConfigLoaderTest {

    @InjectMocks
    private AgentConfigLoader agentConfigLoader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testLoadAllAgents() throws Exception {
        // 执行加载
        agentConfigLoader.loadAllAgents();

        // 验证加载成功
        assertTrue(agentConfigLoader.getAllAgentConfigs().size() > 0, "应该至少加载一个智能体");
    }

    @Test
    void testGetAgentConfig() throws Exception {
        // 先加载所有配置
        agentConfigLoader.loadAllAgents();

        // 测试获取通用助手配置
        AgentConfig config = agentConfigLoader.getAgentConfig("general-assistant");
        
        assertNotNull(config, "通用助手配置不应为空");
        assertEquals("general-assistant", config.getId());
        assertEquals("通用助手", config.getName());
        assertNotNull(config.getSystemPrompt(), "系统提示词不应为空");
    }

    @Test
    void testGetAgentConfig_NotFound() throws Exception {
        agentConfigLoader.loadAllAgents();

        AgentConfig config = agentConfigLoader.getAgentConfig("non-existent-agent");
        
        assertNull(config, "不存在的智能体配置应返回 null");
    }

    @Test
    void testHasAgent() throws Exception {
        agentConfigLoader.loadAllAgents();

        assertTrue(agentConfigLoader.hasAgent("general-assistant"), "应该存在通用助手");
        assertTrue(agentConfigLoader.hasAgent("coding-assistant"), "应该存在编程助手");
        assertFalse(agentConfigLoader.hasAgent("fake-agent"), "不应该存在虚假的智能体");
    }

    @Test
    void testReload() throws Exception {
        // 首次加载
        agentConfigLoader.loadAllAgents();
        int firstCount = agentConfigLoader.getAllAgentConfigs().size();

        // 重新加载
        agentConfigLoader.reload();
        int secondCount = agentConfigLoader.getAllAgentConfigs().size();

        // 验证重新加载成功
        assertEquals(firstCount, secondCount, "重新加载后智能体数量应该相同");
    }
}
