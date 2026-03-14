package com.ext.controller;

import com.ext.model.AgentConfig;
import com.ext.service.AgentConfigLoader;
import com.ext.service.AgentService;
import com.ext.service.ContextManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private AgentService agentService;

    @Autowired
    private AgentConfigLoader agentConfigLoader;

    @Autowired
    private ContextManagementService contextManagementService;

    /**
     * 发送消息并获取回复
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestBody Map<String, String> request) {
        
        String agentId = request.getOrDefault("agentId", "general-assistant");
        String conversationId = request.getOrDefault("conversationId", UUID.randomUUID().toString());
        String message = request.get("message");

        if (message == null || message.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "消息内容不能为空");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // 获取智能体配置
        AgentConfig agentConfig = agentConfigLoader.getAgentConfig(agentId);
        if (agentConfig == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "未找到智能体：" + agentId);
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            String response = agentService.chat(agentConfig, conversationId, message);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("agentId", agentId);
            result.put("conversationId", conversationId);
            result.put("response", response);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "处理失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 清空会话历史
     */
    @DeleteMapping("/conversation/{agentId}/{conversationId}")
    public ResponseEntity<Map<String, Object>> clearConversation(
            @PathVariable String agentId,
            @PathVariable String conversationId) {
        
        try {
            contextManagementService.deleteContext(conversationId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "会话已清空");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "清空会话失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 获取所有可用的智能体列表
     */
    @GetMapping("/agents")
    public ResponseEntity<List<AgentConfig>> listAgents() {
        return ResponseEntity.ok(agentConfigLoader.getAllAgentConfigs());
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Qianwen AI Agent - Multi-Agent System");
        return ResponseEntity.ok(response);
    }
}
