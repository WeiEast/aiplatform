package com.ext.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ext.model.ChatMessage;
import com.ext.model.ConversationContext;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class ContextManagementService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${conversation.max-history-rounds:5}")
    private int maxHistoryRounds;

    @Value("${context.storage.path:context}")
    private String contextStoragePath;

    private static final String CONTEXT_PREFIX = "conversation:";

    /**
     * 获取会话上下文
     */
    public ConversationContext getContext(String conversationId) {
        String key = CONTEXT_PREFIX + conversationId;
        RBucket<ConversationContext> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    /**
     * 保存会话上下文到 Redis
     */
    public void saveContext(ConversationContext context) {
        String key = CONTEXT_PREFIX + context.getId();
        RBucket<ConversationContext> bucket = redissonClient.getBucket(key);
        bucket.set(context);
    }

    /**
     * 添加消息并检查是否需要总结存储到文件
     */
    public synchronized ConversationContext addMessage(String conversationId, ChatMessage message) {
        ConversationContext context = getContext(conversationId);
        
        if (context == null) {
            context = ConversationContext.builder()
                    .id(conversationId)
                    .messages(new ArrayList<>())
                    .createdAt(System.currentTimeMillis())
                    .updatedAt(System.currentTimeMillis())
                    .messageCount(0)
                    .build();
        }

        context.getMessages().add(message);
        context.setMessageCount(context.getMessageCount() + 1);
        context.setUpdatedAt(System.currentTimeMillis());

        // 检查是否超过窗口长度
        if (context.getMessageCount() > maxHistoryRounds * 2) {
            // 需要总结并存储到文件
            summarizeAndSaveToFile(context);
            
            // 保留最近的消息
            int startIndex = context.getMessages().size() - maxHistoryRounds;
            if (startIndex > 0) {
                List<ChatMessage> recentMessages = context.getMessages().subList(startIndex, context.getMessages().size());
                context.setMessages(recentMessages);
                context.setMessageCount(recentMessages.size());
            }
        }

        saveContext(context);
        return context;
    }

    /**
     * 总结对话并存储到文件
     */
    private void summarizeAndSaveToFile(ConversationContext context) {
        try {
            // 创建目录
            Path dir = Paths.get(contextStoragePath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            // 文件路径：context/{id}.json
            File file = new File(contextStoragePath, context.getId() + ".json");
            
            // 将上下文写入文件
            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(context);
            
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(jsonContent);
            }

            System.out.println("已将会话 " + context.getId() + " 总结存储到文件：" + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("存储上下文到文件失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从文件加载历史上下文
     */
    public ConversationContext loadContextFromFile(String conversationId) {
        try {
            File file = new File(contextStoragePath, conversationId + ".json");
            if (file.exists()) {
                return objectMapper.readValue(file, ConversationContext.class);
            }
        } catch (IOException e) {
            System.err.println("从文件加载上下文失败：" + e.getMessage());
        }
        return null;
    }

    /**
     * 删除会话上下文
     */
    public void deleteContext(String conversationId) {
        String key = CONTEXT_PREFIX + conversationId;
        RBucket<ConversationContext> bucket = redissonClient.getBucket(key);
        bucket.delete();

        // 同时删除文件
        File file = new File(contextStoragePath, conversationId + ".json");
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 获取最近的 N 条消息用于上下文
     */
    public List<ChatMessage> getRecentMessages(String conversationId, int limit) {
        ConversationContext context = getContext(conversationId);
        if (context == null || context.getMessages() == null) {
            return new ArrayList<>();
        }

        List<ChatMessage> messages = context.getMessages();
        int size = messages.size();
        
        if (size <= limit) {
            return messages;
        }
        
        return messages.subList(size - limit, size);
    }
}
