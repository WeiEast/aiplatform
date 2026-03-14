package com.ext.service;

import com.ext.model.ChatMessage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ContextManagementService contextManagementService;

    @Value("${conversation.max-history-rounds:5}")
    private int maxHistoryRounds;

    private static final String SYSTEM_PROMPT = """
            你是一个有用的 AI 助手。请友好、专业地回答用户的问题。
            如果用户的问题不够明确，请主动询问更多细节。
            回答应简洁明了，重点突出。
            """;

    /**
     * 聊天方法
     */
    public String chat(String conversationId, String userMessage) {
        // 创建用户消息
        ChatMessage userMsg = ChatMessage.builder()
                .role("USER")
                .content(userMessage)
                .timestamp(System.currentTimeMillis())
                .build();

        // 添加到上下文
        contextManagementService.addMessage(conversationId, userMsg);

        // 获取最近的对话历史
        List<ChatMessage> recentMessages = contextManagementService.getRecentMessages(
                conversationId, maxHistoryRounds * 2);

        // 构建消息列表
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));

        for (ChatMessage msg : recentMessages) {
            if ("USER".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else {
                messages.add(new org.springframework.ai.chat.messages.AssistantMessage(msg.getContent()));
            }
        }

        // 调用千问模型
        Prompt prompt = new Prompt(messages);
        String response = chatClient.prompt(prompt)
                .call()
                .content();

        // 保存助手回复
        ChatMessage assistantMsg = ChatMessage.builder()
                .role("ASSISTANT")
                .content(response != null ? response : "抱歉，我暂时无法回答这个问题。")
                .timestamp(System.currentTimeMillis())
                .build();

        contextManagementService.addMessage(conversationId, assistantMsg);

        return response != null ? response : "抱歉，我暂时无法回答这个问题。";
    }

    /**
     * 清空会话历史
     */
    public void clearHistory(String conversationId) {
        contextManagementService.deleteContext(conversationId);
    }
}
