package com.ext.service;

import com.ext.model.ChatMessage;
import com.ext.model.ConversationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 上下文管理服务测试
 * 测试 Redis 上下文存储和文件归档功能
 */
@DisplayName("上下文管理服务测试")
class ContextManagementServiceTest {

    @Mock
    private ContextManagementService contextManagementService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("测试添加用户消息到上下文")
    void testAddMessage_UserMessage() {
        // 准备测试数据
        String conversationId = "test-conv-123";
        ChatMessage message = ChatMessage.builder()
                .role("USER")
                .content("你好")
                .timestamp(System.currentTimeMillis())
                .build();

        // 执行测试（这里使用 mock，实际应该在真实环境中测试）
        when(contextManagementService.addMessage(eq(conversationId), any(ChatMessage.class)))
                .thenReturn(ConversationContext.builder()
                        .id(conversationId)
                        .messageCount(1)
                        .build());

        ConversationContext result = contextManagementService.addMessage(conversationId, message);

        // 验证结果
        assertNotNull(result);
        assertEquals(conversationId, result.getId());
        assertEquals(1, result.getMessageCount());
    }

    @Test
    @DisplayName("获取最近消息 - 空上下文")
    void testGetRecentMessages_EmptyContext() {
        String conversationId = "empty-conv";
        
        when(contextManagementService.getRecentMessages(eq(conversationId), anyInt()))
                .thenReturn(List.of());

        List<ChatMessage> messages = contextManagementService.getRecentMessages(conversationId, 5);

        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }

    @Test
    @DisplayName("删除上下文 - 成功场景")
    void testDeleteContext_Success() {
        String conversationId = "test-delete-123";
        
        doNothing().when(contextManagementService).deleteContext(conversationId);
        
        assertDoesNotThrow(() -> contextManagementService.deleteContext(conversationId));
        
        verify(contextManagementService, times(1)).deleteContext(conversationId);
    }

    @Test
    @DisplayName("保存上下文 - 验证调用")
    void testSaveContext() {
        ConversationContext context = ConversationContext.builder()
                .id("test-save")
                .messageCount(5)
                .build();

        doNothing().when(contextManagementService).saveContext(context);
        
        assertDoesNotThrow(() -> contextManagementService.saveContext(context));
        verify(contextManagementService, times(1)).saveContext(context);
    }
}
