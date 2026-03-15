package com.ext.repository;

import com.ext.model.entity.KbDocumentSegment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KbDocumentSegmentRepository extends JpaRepository<KbDocumentSegment, Long> {
    
    /**
     * 查询文档的所有分块
     */
    List<KbDocumentSegment> findByDocIdOrderBySegmentNo(Long docId);
    
    /**
     * 查询知识库的分块
     */
    Page<KbDocumentSegment> findByKbId(Long kbId, Pageable pageable);
    
    /**
     * 向量相似度搜索（使用 PGVector）
     */
    @Query(value = "SELECT * FROM kb_document_segment " +
                   "WHERE kb_id = :kbId " +
                   "ORDER BY embedding <=> :embedding LIMIT :topK", 
           nativeQuery = true)
    List<KbDocumentSegment> findSimilar(
        @Param("kbId") Long kbId,
        @Param("embedding") float[] embedding,
        @Param("topK") int topK
    );
    
    /**
     * 带分数阈值的向量搜索
     */
    @Query(value = "SELECT *, 1 - (embedding <=> :embedding) as score " +
                   "FROM kb_document_segment " +
                   "WHERE kb_id = :kbId " +
                   "AND 1 - (embedding <=> :embedding) >= :threshold " +
                   "ORDER BY embedding <=> :embedding LIMIT :topK", 
           nativeQuery = true)
    List<Object[]> findSimilarWithScore(
        @Param("kbId") Long kbId,
        @Param("embedding") float[] embedding,
        @Param("topK") int topK,
        @Param("threshold") double threshold
    );
    
    /**
     * 删除文档的分块
     */
    void deleteByDocId(Long docId);
}
