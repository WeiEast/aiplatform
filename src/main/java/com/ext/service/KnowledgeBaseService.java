package com.ext.service;

import com.ext.model.dto.CreateKbRequest;
import com.ext.model.dto.KnowledgeBaseResponse;
import com.ext.model.entity.KnowledgeBase;
import com.ext.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {
    
    private final KnowledgeBaseRepository kbRepository;
    
    /**
     * 创建知识库
     */
    @Transactional
    public KnowledgeBase create(CreateKbRequest request, String creator) {
        // 检查名称是否重复
        kbRepository.findByNameAndBusinessLineAndIsDeleted(
            request.getName(), 
            request.getBusinessLine(), 
            0
        ).ifPresent(existing -> {
            throw new IllegalArgumentException("该知识库已存在");
        });
        
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kb.setCreator(creator);
        kb.setBusinessLine(request.getBusinessLine());
        
        KnowledgeBase saved = kbRepository.save(kb);
        log.info("创建知识库成功：id={}, name={}", saved.getId(), saved.getName());
        
        return saved;
    }
    
    /**
     * 获取知识库列表
     */
    @Transactional(readOnly = true)
    public Page<KnowledgeBaseResponse> list(Integer page, Integer size, 
                                           String businessLine, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size, 
            Sort.by(Sort.Direction.DESC, "createTime"));
        
        Page<KnowledgeBase> kbPage;
        
        if (keyword != null && !keyword.isEmpty()) {
            kbPage = kbRepository.searchByKeyword(keyword, pageable);
        } else if (businessLine != null && !businessLine.isEmpty()) {
            kbPage = kbRepository.findByBusinessLineAndIsDeleted(businessLine, 0, pageable);
        } else {
            kbPage = kbRepository.findByIsDeletedOrderByCreateTimeDesc(0, pageable);
        }
        
        return kbPage.map(this::convertToResponse);
    }
    
    /**
     * 获取知识库详情
     */
    @Transactional(readOnly = true)
    public KnowledgeBaseResponse getById(Long id) {
        KnowledgeBase kb = kbRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("知识库不存在"));
        
        if (kb.getIsDeleted() != 0) {
            throw new IllegalArgumentException("知识库已被删除");
        }
        
        return convertToResponse(kb);
    }
    
    /**
     * 更新知识库
     */
    @Transactional
    public KnowledgeBase update(Long id, CreateKbRequest request) {
        KnowledgeBase kb = kbRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("知识库不存在"));
        
        if (kb.getIsDeleted() != 0) {
            throw new IllegalArgumentException("知识库已被删除");
        }
        
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kb.setBusinessLine(request.getBusinessLine());
        
        KnowledgeBase updated = kbRepository.save(kb);
        log.info("更新知识库成功：id={}", id);
        
        return updated;
    }
    
    /**
     * 删除知识库（逻辑删除）
     */
    @Transactional
    public void delete(Long id) {
        KnowledgeBase kb = kbRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("知识库不存在"));
        
        kb.setIsDeleted(1);
        kbRepository.save(kb);
        log.info("删除知识库成功：id={}", id);
    }
    
    private KnowledgeBaseResponse convertToResponse(KnowledgeBase kb) {
        long docCount = kbRepository.countByIdAndIsDeleted(kb.getId(), 0);
        
        return KnowledgeBaseResponse.builder()
            .id(kb.getId())
            .name(kb.getName())
            .description(kb.getDescription())
            .creator(kb.getCreator())
            .businessLine(kb.getBusinessLine())
            .createTime(kb.getCreateTime())
            .documentCount(docCount)
            .build();
    }
}
