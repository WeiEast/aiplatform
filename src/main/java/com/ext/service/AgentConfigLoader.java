package com.ext.service;

import com.ext.model.AgentConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能体配置加载器
 * 从 resources/agents/目录加载智能体配置文件
 */
@Service
public class AgentConfigLoader {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final Map<String, AgentConfig> agentConfigs = new ConcurrentHashMap<>();

    /**
     * 加载所有智能体配置
     */
    public void loadAllAgents() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:agents/*.yml");

        for (Resource resource : resources) {
            try {
                AgentConfig config = loadAgentConfig(resource);
                agentConfigs.put(config.getId(), config);
                System.out.println("已加载智能体：" + config.getId() + " - " + config.getName());
            } catch (Exception e) {
                System.err.println("加载智能体配置失败：" + resource.getFilename() + ", 错误：" + e.getMessage());
            }
        }
    }

    /**
     * 加载单个智能体配置
     */
    public AgentConfig loadAgentConfig(Resource resource) throws IOException {
        JsonNode rootNode = yamlMapper.readTree(resource.getInputStream());

        // 提取 agent 配置
        JsonNode agentNode = rootNode.path("agent");
        String id = agentNode.path("id").asText();
        String name = agentNode.path("name").asText();
        String description = agentNode.path("description").asText();

        // 提取系统提示词
        String systemPrompt = rootNode.path("system").path("prompt").asText();

        // 提取模型配置
        JsonNode modelNode = rootNode.path("spring").path("ai").path("dashscope").path("chat").path("options");
        String model = modelNode.path("model").asText("qwen-max");
        Double temperature = modelNode.has("temperature") ? modelNode.path("temperature").asDouble() : 0.7;

        // 提取上下文配置
        Integer maxHistoryRounds = rootNode.path("conversation").path("max-history-rounds").asInt(5);

        // 提取 RAG 配置
        boolean ragEnabled = rootNode.path("rag").path("enabled").asBoolean(false);
        Integer ragTopK = rootNode.path("rag").path("top-k").asInt(4);
        Double ragSimilarityThreshold = rootNode.path("rag").path("similarity-threshold").asDouble(0.7);

        return AgentConfig.builder()
                .id(id)
                .name(name)
                .description(description)
                .systemPrompt(systemPrompt)
                .model(model)
                .temperature(temperature)
                .maxHistoryRounds(maxHistoryRounds)
                .ragEnabled(ragEnabled)
                .ragTopK(ragTopK)
                .ragSimilarityThreshold(ragSimilarityThreshold)
                .build();
    }

    /**
     * 获取智能体配置
     */
    public AgentConfig getAgentConfig(String agentId) {
        return agentConfigs.get(agentId);
    }

    /**
     * 获取所有智能体配置
     */
    public List<AgentConfig> getAllAgentConfigs() {
        return new ArrayList<>(agentConfigs.values());
    }

    /**
     * 检查智能体是否存在
     */
    public boolean hasAgent(String agentId) {
        return agentConfigs.containsKey(agentId);
    }

    /**
     * 重新加载智能体配置
     */
    public void reload() throws IOException {
        agentConfigs.clear();
        loadAllAgents();
    }
}
