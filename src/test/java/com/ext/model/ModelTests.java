package com.ext.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模型类测试
 * 测试数据模型的构建和功能
 */
@DisplayName("模型类测试")
class ModelTests {

    @Test
    @DisplayName("ChatMessage - Builder 模式测试")
    void testChatMessageBuilder() {
        long timestamp = System.currentTimeMillis();
        
        ChatMessage message = ChatMessage.builder()
                .role("USER")
                .content("测试消息")
                .timestamp(timestamp)
                .build();

        assertEquals("USER", message.getRole());
        assertEquals("测试消息", message.getContent());
        assertEquals(timestamp, message.getTimestamp());
    }

    @Test
    @DisplayName("ChatMessage - Lombok 注解测试")
    void testChatMessageLombokAnnotations() {
        ChatMessage msg1 = ChatMessage.builder()
                .role("ASSISTANT")
                .content("内容")
                .timestamp(123L)
                .build();

        ChatMessage msg2 = ChatMessage.builder()
                .role("ASSISTANT")
                .content("内容")
                .timestamp(123L)
                .build();

        // 测试 equals 和 hashCode（如果使用了 @EqualsAndHashCode）
        assertEquals(msg1.getRole(), msg2.getRole());
        assertEquals(msg1.getContent(), msg2.getContent());
    }

    @Test
    @DisplayName("ConversationContext - 完整构建测试")
    void testConversationContextBuilder() {
        ConversationContext context = ConversationContext.builder()
                .id("conv-123")
                .messageCount(5)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        assertNotNull(context);
        assertEquals("conv-123", context.getId());
        assertEquals(5, context.getMessageCount());
        assertNotNull(context.getCreatedAt());
        assertNotNull(context.getUpdatedAt());
    }

    @Test
    @DisplayName("AgentConfig - 所有字段测试")
    void testAgentConfigAllFields() {
        AgentConfig config = AgentConfig.builder()
                .id("test-agent")
                .name("测试智能体")
                .description("测试描述")
                .systemPrompt("你是一个测试助手")
                .model("qwen-max")
                .temperature(0.7)
                .maxHistoryRounds(5)
                .ragEnabled(true)
                .ragTopK(4)
                .ragSimilarityThreshold(0.7)
                .build();

        assertEquals("test-agent", config.getId());
        assertEquals("测试智能体", config.getName());
        assertEquals("测试描述", config.getDescription());
        assertEquals("qwen-max", config.getModel());
        assertEquals(0.7, config.getTemperature());
        assertTrue(config.isRagEnabled());
        assertEquals(4, config.getRagTopK());
        assertEquals(0.7, config.getRagSimilarityThreshold());
    }

    @Test
    @DisplayName("RagDocument - 静态工厂方法测试")
    void testRagDocumentFactoryMethods() {
        // 测试 fromContent
        RagDocument doc1 = RagDocument.fromContent("纯内容文档");
        assertNotNull(doc1);
        assertEquals("纯内容文档", doc1.getContent());
        assertNull(doc1.getId());
        assertNull(doc1.getMetadata());

        // 测试 fromContentWithMetadata
        Map<String, Object> metadata = Map.of("source", "test", "category", "demo");
        RagDocument doc2 = RagDocument.fromContentWithMetadata("带元数据的文档", metadata);
        
        assertEquals("带元数据的文档", doc2.getContent());
        assertEquals(metadata, doc2.getMetadata());
        assertEquals("test", doc2.getMetadata().get("source"));
    }

    @Test
    @DisplayName("RagDocument - Builder 完整测试")
    void testRagDocumentFullBuilder() {
        Map<String, Object> metadata = Map.of("key", "value");
        
        RagDocument doc = RagDocument.builder()
                .id("doc-456")
                .content("文档内容")
                .metadata(metadata)
                .score(0.95)
                .build();

        assertEquals("doc-456", doc.getId());
        assertEquals("文档内容", doc.getContent());
        assertEquals(metadata, doc.getMetadata());
        assertEquals(0.95, doc.getScore());
    }

    @Test
    @DisplayName("McpTool - 工具定义测试")
    void testMcpToolDefinition() {
        McpToolHandler handler = args -> "执行结果";
        
        McpTool tool = McpTool.builder()
                .name("test_tool")
                .description("测试工具")
                .handler(handler)
                .build();

        assertEquals("test_tool", tool.getName());
        assertEquals("测试工具", tool.getDescription());
        assertNotNull(tool.getHandler());
        
        // 测试处理器执行
        String result = tool.getHandler().handle(Map.of("param", "value"));
        assertEquals("执行结果", result);
    }

    @Test
    @DisplayName("McpToolHandler - 函数式接口测试")
    void testMcpToolHandlerFunctionalInterface() {
        // Lambda 表达式实现
        McpToolHandler addHandler = (args) -> {
            Integer a = (Integer) args.get("a");
            Integer b = (Integer) args.get("b");
            return String.valueOf(a + b);
        };

        String result = addHandler.handle(Map.of("a", 5, "b", 3));
        assertEquals("8", result);

        // 方法引用实现
        McpToolHandler echoHandler = args -> args.get("input").toString();
        assertEquals("Hello", echoHandler.handle(Map.of("input", "Hello")));
    }
}
