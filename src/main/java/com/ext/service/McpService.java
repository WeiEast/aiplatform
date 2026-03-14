package com.ext.service;

import com.ext.model.McpTool;
import com.ext.model.McpToolHandler;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * MCP (Model Context Protocol) 服务
 * 提供工具注册和调用功能
 */
@Service
public class McpService {

    private final Map<String, McpTool> tools = new ConcurrentHashMap<>();

    /**
     * 注册工具
     */
    public void registerTool(String name, String description, McpToolHandler handler) {
        McpTool tool = McpTool.builder()
                .name(name)
                .description(description)
                .handler(handler)
                .build();
        tools.put(name, tool);
    }

    /**
     * 注册工具（带输入 schema）
     */
    public void registerTool(String name, String description, 
                            Map<String, Object> inputSchema, 
                            McpToolHandler handler) {
        McpTool tool = McpTool.builder()
                .name(name)
                .description(description)
                .inputSchema(inputSchema)
                .handler(handler)
                .build();
        tools.put(name, tool);
    }

    /**
     * 调用工具
     */
    public String callTool(String toolName, Map<String, Object> arguments) {
        McpTool tool = tools.get(toolName);
        if (tool == null) {
            return "错误：未找到工具 '" + toolName + "'";
        }
        
        try {
            return tool.getHandler().handle(arguments);
        } catch (Exception e) {
            return "工具执行失败：" + e.getMessage();
        }
    }

    /**
     * 获取所有已注册的工具
     */
    public List<McpTool> getAllTools() {
        return List.copyOf(tools.values());
    }

    /**
     * 获取工具描述信息（用于 prompt）
     */
    public String getToolsDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("可用的工具：\n");
        for (McpTool tool : tools.values()) {
            sb.append("- ").append(tool.getName())
              .append(": ").append(tool.getDescription()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 移除工具
     */
    public void unregisterTool(String toolName) {
        tools.remove(toolName);
    }
}
