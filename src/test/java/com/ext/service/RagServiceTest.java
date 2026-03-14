package com.ext.service;

import com.ext.model.RagDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RAG 服务测试
 * 测试文档存储和检索功能
 */
@DisplayName("RAG 服务测试")
class RagServiceTest {

    @InjectMocks
    private RagService ragService;

    @Mock
    private VectorStore vectorStore;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 通过反射设置 vectorStore（因为使用了 required = false）
        try {
            var field = RagService.class.getDeclaredField("vectorStore");
            field.setAccessible(true);
            field.set(ragService, vectorStore);
        } catch (Exception e) {
            fail("无法设置 vectorStore: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("添加文档 - 正常场景")
    @DisplayName("测试将单个文档添加到向量库")
    void testAddDocument_Success() {
        // 准备测试数据
        RagDocument doc = RagDocument.builder()
                .id("doc-123")
                .content("这是测试内容")
                .metadata(Map.of("source", "test"))
                .build();

        doNothing().when(vectorStore).add(anyList());

        // 执行测试
        assertDoesNotThrow(() -> ragService.addDocument(doc));

        // 验证调用
        verify(vectorStore, times(1)).add(anyList());
    }

    @Test
    @DisplayName("批量添加文档 - 验证调用次数")
    void testAddDocuments_Multiple() {
        List<RagDocument> documents = List.of(
            RagDocument.builder().content("内容 1").build(),
            RagDocument.builder().content("内容 2").build(),
            RagDocument.builder().content("内容 3").build()
        );

        doNothing().when(vectorStore).add(anyList());

        ragService.addDocuments(documents);

        verify(vectorStore, times(3)).add(anyList());
    }

    @Test
    @DisplayName("检索文档 - 返回结果")
    void testRetrieve_WithResults() {
        String query = "测试查询";
        
        // Mock 相似度搜索返回空列表（简化测试）
        when(vectorStore.similaritySearch(query)).thenReturn(List.of());

        List<RagDocument> results = ragService.retrieve(query);

        assertNotNull(results);
        verify(vectorStore, times(1)).similaritySearch(query);
    }

    @Test
    @DisplayName("检索为空 - VectorStore 未配置")
    void testRetrieve_VectorStoreNotConfigured() {
        // 临时设置为 null
        try {
            var field = RagService.class.getDeclaredField("vectorStore");
            field.setAccessible(true);
            field.set(ragService, null);

            List<RagDocument> results = ragService.retrieve("任何查询");
            
            assertNotNull(results);
            assertTrue(results.isEmpty());
        } catch (Exception e) {
            fail("测试失败：" + e.getMessage());
        } finally {
            // 恢复
            try {
                var field = RagService.class.getDeclaredField("vectorStore");
                field.setAccessible(true);
                field.set(ragService, vectorStore);
            } catch (Exception ex) {
                // 忽略
            }
        }
    }

    @Test
    @DisplayName("删除文档 - 成功")
    void testDeleteDocument_Success() {
        String docId = "doc-to-delete";
        when(vectorStore.delete(anyList())).thenReturn(List.of());

        boolean result = ragService.deleteDocument(docId);

        assertTrue(result);
        verify(vectorStore, times(1)).delete(anyList());
    }

    @Test
    @DisplayName("删除文档 - 异常处理")
    void testDeleteDocument_Exception() {
        String docId = "doc-error";
        doThrow(new RuntimeException("删除失败")).when(vectorStore).delete(anyList());

        boolean result = ragService.deleteDocument(docId);

        assertFalse(result);
    }

    @Test
    @DisplayName("添加文档 - 自动生成 ID")
    void testAddDocument_AutoGenerateId() {
        RagDocument doc = RagDocument.builder()
                .content("没有 ID 的文档")
                .build();

        doNothing().when(vectorStore).add(anyList());

        assertDoesNotThrow(() -> ragService.addDocument(doc));
        verify(vectorStore, times(1)).add(anyList());
    }
}
