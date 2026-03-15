package com.ext.controller;

import com.ext.model.dto.CreateKbRequest;
import com.ext.model.dto.KnowledgeBaseResponse;
import com.ext.model.entity.KnowledgeBase;
import com.ext.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "知识库管理", description = "知识库 CRUD 接口")
@RestController
@RequestMapping("/api/v1/knowledge-base")
@RequiredArgsConstructor
public class KnowledgeBaseController {
    
    private final KnowledgeBaseService kbService;
    
    @PostMapping
    @Operation(summary = "创建知识库")
    public ResponseEntity<KnowledgeBase> create(
            @Valid @RequestBody CreateKbRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String creator) {
        
        KnowledgeBase created = kbService.create(request, creator);
        return ResponseEntity.ok(created);
    }
    
    @GetMapping
    @Operation(summary = "获取知识库列表")
    public ResponseEntity<Page<KnowledgeBaseResponse>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String businessLine,
            @RequestParam(required = false) String keyword) {
        
        Page<KnowledgeBaseResponse> result = kbService.list(page, size, businessLine, keyword);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取知识库详情")
    public ResponseEntity<KnowledgeBaseResponse> getById(@PathVariable Long id) {
        KnowledgeBaseResponse response = kbService.getById(id);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新知识库")
    public ResponseEntity<KnowledgeBase> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateKbRequest request) {
        
        KnowledgeBase updated = kbService.update(id, request);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识库")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        kbService.delete(id);
        return ResponseEntity.ok().build();
    }
}
