package com.ext.service;

import com.ext.model.McpTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MCP 服务测试
 */
class McpServiceTest {

    private McpService mcpService;

    @BeforeEach
    void setUp() {
        mcpService = new McpService();
    }

    @Test
    void testRegisterTool() {
        // 注册一个简单的工具
        mcpService.registerTool("test_tool", "测试工具", arguments -> {
            return "执行成功";
        });

        // 验证工具已注册
        List<McpTool> tools = mcpService.getAllTools();
        assertEquals(1, tools.size());
        assertEquals("test_tool", tools.get(0).getName());
        assertEquals("测试工具", tools.get(0).getDescription());
    }

    @Test
    void testCallTool() {
        // 注册带参数的工具
        mcpService.registerTool("calculator", "计算器", arguments -> {
            Double a = (Double) arguments.get("a");
            Double b = (Double) arguments.get("b");
            return String.valueOf(a + b);
        });

        // 调用工具
        Map<String, Object> args = new HashMap<>();
        args.put("a", 5.0);
        args.put("b", 3.0);

        String result = mcpService.callTool("calculator", args);
        assertEquals("8.0", result);
    }

    @Test
    void testCallTool_NotFound() {
        // 调用不存在的工具
        String result = mcpService.callTool("non_existent_tool", new HashMap<>());
        
        assertTrue(result.contains("错误"), "应该返回错误信息");
        assertTrue(result.contains("未找到工具"), "应该提示工具未找到");
    }

    @Test
    void testCallTool_WithException() {
        // 注册一个会抛出异常的工具
        mcpService.registerTool("error_tool", "错误工具", arguments -> {
            throw new RuntimeException("模拟异常");
        });

        String result = mcpService.callTool("error_tool", new HashMap<>());
        
        assertTrue(result.contains("工具执行失败"), "应该捕获异常并返回错误信息");
    }

    @Test
    void testGetToolsDescription() {
        // 注册多个工具
        mcpService.registerTool("tool1", "第一个工具", arguments -> "result1");
        mcpService.registerTool("tool2", "第二个工具", arguments -> "result2");

        String description = mcpService.getToolsDescription();
        
        assertNotNull(description);
        assertTrue(description.contains("可用的工具"));
        assertTrue(description.contains("tool1"));
        assertTrue(description.contains("tool2"));
        assertTrue(description.contains("第一个工具"));
        assertTrue(description.contains("第二个工具"));
    }

    @Test
    void testUnregisterTool() {
        // 注册工具
        mcpService.registerTool("temp_tool", "临时工具", arguments -> "result");
        assertEquals(1, mcpService.getAllTools().size());

        // 注销工具
        mcpService.unregisterTool("temp_tool");
        assertEquals(0, mcpService.getAllTools().size());
    }

    @Test
    void testRegisterTool_WithSchema() {
        // 准备输入 schema
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> nameProp = new HashMap<>();
        nameProp.put("type", "string");
        nameProp.put("description", "姓名");
        properties.put("name", nameProp);
        schema.put("properties", properties);

        // 注册带 schema 的工具
        mcpService.registerTool("greeting", "问候工具", schema, arguments -> {
            String name = (String) arguments.get("name");
            return "你好，" + name;
        });

        List<McpTool> tools = mcpService.getAllTools();
        assertEquals(1, tools.size());
        assertNotNull(tools.get(0).getInputSchema());
        assertEquals("object", tools.get(0).getInputSchema().get("type"));
    }
}
