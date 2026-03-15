package com.ext.model.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class CreateKbRequest {
    
    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 128, message = "知识库名称不能超过 128 个字符")
    private String name;
    
    @Size(max = 512, message = "描述不能超过 512 个字符")
    private String description;
    
    @Size(max = 64, message = "业务线标识不能超过 64 个字符")
    private String businessLine = "default";
}
