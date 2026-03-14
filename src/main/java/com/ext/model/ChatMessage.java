package com.ext.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String role;      // USER 或 ASSISTANT
    private String content;   // 消息内容
    private Long timestamp;   // 时间戳
}
