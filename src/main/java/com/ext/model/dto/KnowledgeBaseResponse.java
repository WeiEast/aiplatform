package com.ext.model.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class KnowledgeBaseResponse {
    private Long id;
    private String name;
    private String description;
    private String creator;
    private String businessLine;
    private LocalDateTime createTime;
    private Long documentCount;
}
