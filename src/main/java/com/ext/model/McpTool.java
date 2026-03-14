package com.ext.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MCP 工具模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpTool {
    private String name;           // 工具名称
    private String description;    // 工具描述
    private Map<String, Object> inputSchema;  // 输入参数 schema
    private McpToolHandler handler;  // 工具处理器
}
