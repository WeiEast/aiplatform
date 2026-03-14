package com.ext.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 聊天控制器集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 确保 Redis 可用（如果 Redis 未启动，这些测试会失败）
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/chat/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").exists());
    }

    @Test
    void testListAgents() throws Exception {
        mockMvc.perform(get("/api/chat/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").isNumber());
    }

    @Test
    void testSendMessage_WithDefaultAgent() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("message", "你好");

        mockMvc.perform(post("/api/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.response").exists())
                .andExpect(jsonPath("$.conversationId").exists());
    }

    @Test
    void testSendMessage_WithSpecificAgent() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("agentId", "general-assistant");
        request.put("conversationId", "test-conv-123");
        request.put("message", "测试消息");

        mockMvc.perform(post("/api/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.agentId").value("general-assistant"))
                .andExpect(jsonPath("$.conversationId").value("test-conv-123"));
    }

    @Test
    void testSendMessage_EmptyMessage() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("message", "");

        mockMvc.perform(post("/api/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testSendMessage_NullMessage() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("message", null);

        mockMvc.perform(post("/api/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testSendMessage_InvalidAgentId() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("agentId", "non-existent-agent");
        request.put("message", "测试");

        mockMvc.perform(post("/api/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testClearConversation() throws Exception {
        String conversationId = "test-clear-" + System.currentTimeMillis();
        
        // 先发送一条消息创建会话
        Map<String, String> createRequest = new HashMap<>();
        createRequest.put("conversationId", conversationId);
        createRequest.put("message", "创建会话");
        
        mockMvc.perform(post("/api/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk());

        // 清空会话
        mockMvc.perform(delete("/api/chat/conversation/{agentId}/{conversationId}", 
                        "general-assistant", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("会话已清空"));
    }
}
