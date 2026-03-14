package com.ext.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationContext {
    private String id;                    // 会话 ID
    private List<ChatMessage> messages;   // 消息列表
    private Long createdAt;               // 创建时间
    private Long updatedAt;               // 更新时间
    private Integer messageCount;         // 消息数量
}
