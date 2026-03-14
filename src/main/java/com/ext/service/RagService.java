package com.ext.service;

import com.ext.model.RagDocument;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval-Augmented Generation) 服务
 * 提供文档存储和检索功能
 */
@Service
public class RagService {

    @Autowired(required = false)
    private VectorStore vectorStore;

    @Value("${rag.top-k:4}")
    private int topK;

    @Value("${rag.similarity-threshold:0.7}")
    private double similarityThreshold;

    /**
     * 添加文档到向量库
     */
    public void addDocument(RagDocument ragDoc) {
        if (vectorStore == null) {
            throw new IllegalStateException("VectorStore 未配置");
        }

        Map<String, Object> metadata = new HashMap<>();
        if (ragDoc.getMetadata() != null) {
            metadata.putAll(ragDoc.getMetadata());
        }
        metadata.put("id", ragDoc.getId() != null ? ragDoc.getId() : UUID.randomUUID().toString());
        metadata.put("timestamp", System.currentTimeMillis());

        Document document = new Document(
                metadata.get("id").toString(),
                ragDoc.getContent(),
                metadata
        );

        vectorStore.add(List.of(document));
    }

    /**
     * 批量添加文档
     */
    public void addDocuments(List<RagDocument> documents) {
        for (RagDocument doc : documents) {
            addDocument(doc);
        }
    }

    /**
     * 检索相关文档
     */
    public List<RagDocument> retrieve(String query) {
        if (vectorStore == null) {
            return Collections.emptyList();
        }

        List<Document> similarDocuments = vectorStore.similaritySearch(query);
        
        return similarDocuments.stream()
                .limit(topK)
                .map(doc -> {
                    RagDocument ragDoc = RagDocument.builder()
                            .id(doc.getMetadata().getOrDefault("id", "").toString())
                            .content(doc.getContent())
                            .metadata(doc.getMetadata())
                            .score(doc.getScore())
                            .build();
                    
                    // 过滤低于阈值的文档
                    if (doc.getScore() != null && doc.getScore() < similarityThreshold) {
                        return null;
                    }
                    return ragDoc;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 检索并格式化文档（用于 prompt）
     */
    public String retrieveAsContext(String query) {
        List<RagDocument> documents = retrieve(query);
        
        if (documents.isEmpty()) {
            return "未找到相关信息。";
        }

        StringBuilder context = new StringBuilder();
        context.append("检索到的相关信息：\n\n");
        
        for (int i = 0; i < documents.size(); i++) {
            RagDocument doc = documents.get(i);
            context.append("[").append(i + 1).append("] ");
            context.append(doc.getContent());
            
            if (doc.getMetadata() != null && doc.getMetadata().containsKey("source")) {
                context.append(" (来源：").append(doc.getMetadata().get("source")).append(")");
            }
            context.append("\n\n");
        }

        return context.toString();
    }

    /**
     * 删除文档（通过 ID）
     */
    public boolean deleteDocument(String documentId) {
        if (vectorStore == null) {
            return false;
        }
        
        try {
            vectorStore.delete(List.of(documentId));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 清空所有文档
     */
    public void clearAll() {
        if (vectorStore != null) {
            // 注意：具体的清空操作依赖于 VectorStore 实现
            throw new UnsupportedOperationException("清空操作依赖于具体的 VectorStore 实现");
        }
    }
}
