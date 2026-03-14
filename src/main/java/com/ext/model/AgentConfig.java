package com.ext.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 智能体配置模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentConfig {
    private String id;                    // 智能体 ID
    private String name;                  // 智能体名称
    private String description;           // 智能体描述
    private String systemPrompt;          // 系统提示词
    private String model;                 // 使用的模型
    private Double temperature;           // 温度参数
    private Integer maxHistoryRounds;     // 最大历史对话轮数
    private boolean ragEnabled;           // 是否启用 RAG
    private Integer ragTopK;              // RAG 检索文档数
    private Double ragSimilarityThreshold; // RAG 相似度阈值
    private List<String> enabledTools;    // 启用的 MCP 工具
}
