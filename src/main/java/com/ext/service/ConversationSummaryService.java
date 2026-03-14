package com.ext.service;

import com.ext.model.ChatMessage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConversationSummaryService {

    @Autowired
    private ChatClient chatClient;

    @Value("${conversation.compression-model:qwen-turbo}")
    private String compressionModel;

    /**
     * 总结对话内容
     */
    public String summarizeConversation(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        StringBuilder historyText = new StringBuilder();
        for (ChatMessage msg : messages) {
            String role = "USER".equals(msg.getRole()) ? "用户" : "助手";
            historyText.append(role).append(": ").append(msg.getContent()).append("\n");
        }

        String summaryPrompt = """
                请总结以下对话内容，提取关键信息。总结应简洁明了，保留重要细节：
                
                %s
                
                总结：
                """.formatted(historyText.toString());

        try {
            Message systemMessage = new SystemMessage("你是一个专业的对话总结助手。");
            Message userMessage = new UserMessage(summaryPrompt);
            
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
            
            String response = chatClient.prompt(prompt)
                    .call()
                    .content();
            
            return response != null ? response : "对话总结失败";
        } catch (Exception e) {
            System.err.println("总结对话失败：" + e.getMessage());
            return "对话总结失败：" + e.getMessage();
        }
    }

    /**
     * 压缩历史记录为摘要
     */
    public String compressHistory(List<ChatMessage> messages, int keepLastRounds) {
        if (messages.size() <= keepLastRounds * 2) {
            return "";
        }

        int messagesToSummarize = messages.size() - (keepLastRounds * 2);
        List<ChatMessage> messagesToProcess = messages.subList(0, messagesToSummarize);
        
        return summarizeConversation(messagesToProcess);
    }
}
