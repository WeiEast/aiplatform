package com.ext.example;

import com.ext.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 简单的使用示例
 */
@Component
public class SimpleChatExample implements CommandLineRunner {

    @Autowired
    private ChatService chatService;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== 通义千问 AI 助手 ===");
        System.out.println("提示：请使用 API 进行测试，例如：");
        System.out.println("");
        System.out.println("curl -X POST http://localhost:8080/api/chat/send \\");
        System.out.println("  -H \"Content-Type: application/json\" \\");
        System.out.println("  -d '{\"conversationId\":\"test-123\",\"message\":\"你好，请介绍一下你自己\"}'");
        System.out.println("");
        System.out.println("服务已启动，请访问 http://localhost:8080/api/chat/health 检查状态");
    }
}
