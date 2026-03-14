package com.ext.service;

import com.ext.model.AgentConfig;
import com.ext.model.ChatMessage;
import com.ext.model.RagDocument;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 通用智能体服务（所有智能体共用）
 */
@Service
public class AgentService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ContextManagementService contextManagementService;

    @Autowired
    private RagService ragService;

    @Autowired
    private McpService mcpService;

    /**
     * 与智能体对话
     */
    public String chat(AgentConfig agentConfig, String conversationId, String userMessage) {
        // 1. 添加用户消息到上下文
        ChatMessage userMsg = ChatMessage.builder()
                .role("USER")
                .content(userMessage)
                .timestamp(System.currentTimeMillis())
                .build();
        contextManagementService.addMessage(conversationId, userMsg);

        // 2. 构建 Prompt
        List<Message> messages = buildPrompt(agentConfig, conversationId, userMessage);

        // 3. 调用模型
        Prompt prompt = new Prompt(messages);
        String response = chatClient.prompt(prompt)
                .call()
                .content();

        // 4. 保存助手回复
        ChatMessage assistantMsg = ChatMessage.builder()
                .role("ASSISTANT")
                .content(response != null ? response : "抱歉，我暂时无法回答这个问题。")
                .timestamp(System.currentTimeMillis())
                .build();
        contextManagementService.addMessage(conversationId, assistantMsg);

        return response != null ? response : "抱歉，我暂时无法回答这个问题。";
    }

    /**
     * 构建 Prompt
     */
    private List<Message> buildPrompt(AgentConfig agentConfig, String conversationId, String userMessage) {
        List<Message> messages = new ArrayList<>();

        // 1. 系统提示词
        String systemPrompt = buildSystemPrompt(agentConfig);
        messages.add(new SystemMessage(systemPrompt));

        // 2. RAG 检索的上下文（如果启用）
        if (agentConfig.isRagEnabled()) {
            String ragContext = ragService.retrieveAsContext(userMessage);
            if (!ragContext.isEmpty()) {
                messages.add(new UserMessage("参考以下信息：" + ragContext));
            }
        }

        // 3. 历史对话
        List<ChatMessage> recentMessages = contextManagementService.getRecentMessages(
                conversationId, agentConfig.getMaxHistoryRounds() * 2);
        
        for (ChatMessage msg : recentMessages) {
            if ("USER".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else {
                messages.add(new org.springframework.ai.chat.messages.AssistantMessage(msg.getContent()));
            }
        }

        return messages;
    }

    /**
     * 构建系统提示词（包含 MCP 工具描述）
     */
    private String buildSystemPrompt(AgentConfig agentConfig) {
        StringBuilder prompt = new StringBuilder(agentConfig.getSystemPrompt());

        // 添加 MCP 工具描述
        if (agentConfig.getEnabledTools() != null && !agentConfig.getEnabledTools().isEmpty()) {
            prompt.append("\n\n");
            prompt.append(mcpService.getToolsDescription());
            prompt.append("\n你可以使用上述工具来帮助用户解决问题。");
        }

        return prompt.toString();
    }

    /**
     * 添加工具到 MCP 并注册到智能体
     */
    public void registerToolForAgent(String agentId, String toolName, String description, 
                                     com.ext.model.McpToolHandler handler) {
        mcpService.registerTool(toolName, description, handler);
    }
}