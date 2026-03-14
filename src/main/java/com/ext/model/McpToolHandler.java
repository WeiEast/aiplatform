package com.ext.model;

import java.util.Map;

/**
 * MCP 工具处理器函数式接口
 */
@FunctionalInterface
public interface McpToolHandler {
    /**
     * 处理工具调用
     * @param arguments 工具参数
     * @return 处理结果
     */
    String handle(Map<String, Object> arguments);
}
