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
@Table(name = "kb_document")
public class KbDocument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "kb_id", nullable = false)
    private Long kbId;
    
    @Column(name = "file_name", nullable = false, length = 256)
    private String fileName;
    
    @Column(name = "file_type", nullable = false, length = 32)
    private String fileType;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @Column(name = "file_path", nullable = false, length = 512)
    private String filePath;
    
    @Column(name = "file_hash", length = 64)
    private String fileHash;
    
    @Version
    private Integer version = 1;
    
    @Column(nullable = false)
    private Integer status = 0; // 0-待处理，1-处理中，2-已完成，3-失败
    
    @Column(name = "error_msg", length = 1024)
    private String errorMsg;
    
    @Column(name = "chunk_count")
    private Integer chunkCount = 0;
    
    @Column(nullable = false, length = 64)
    private String creator;
    
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    @Column(name = "is_deleted", columnDefinition = "INTEGER")
    private Integer isDeleted = 0;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
