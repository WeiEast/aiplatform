package com.ext.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * RAG 文档模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagDocument {
    private String id;              // 文档 ID
    private String content;         // 文档内容
    private Map<String, Object> metadata;  // 元数据
    private Double score;           // 相似度分数（检索时填充）
    
    public static RagDocument fromContent(String content) {
        return RagDocument.builder()
                .content(content)
                .build();
    }
    
    public static RagDocument fromContentWithMetadata(String content, Map<String, Object> metadata) {
        return RagDocument.builder()
                .content(content)
                .metadata(metadata)
                .build();
    }
}
