package com.ext.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "kb_document_segment")
public class KbDocumentSegment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "doc_id", nullable = false)
    private Long docId;
    
    @Column(name = "kb_id", nullable = false)
    private Long kbId;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(columnDefinition = "vector(1536)")
    private float[] embedding;
    
    @Column(name = "segment_no", nullable = false)
    private Integer segmentNo;
    
    @Column(columnDefinition = "jsonb")
    private String metadata;
    
    @Column(name = "token_count")
    private Integer tokenCount = 0;
    
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
