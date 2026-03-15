package com.ext.repository;

import com.ext.model.entity.KnowledgeBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    
    /**
     * 根据名称和业务线查找知识库
     */
    Optional<KnowledgeBase> findByNameAndBusinessLineAndIsDeleted(String name, String businessLine, Integer isDeleted);
    
    /**
     * 分页查询知识库列表
     */
    Page<KnowledgeBase> findByIsDeletedOrderByCreateTimeDesc(Integer isDeleted, Pageable pageable);
    
    /**
     * 按业务线筛选
     */
    Page<KnowledgeBase> findByBusinessLineAndIsDeleted(String businessLine, Integer isDeleted, Pageable pageable);
    
    /**
     * 关键词搜索
     */
    @Query("SELECT k FROM KnowledgeBase k WHERE k.isDeleted = 0 AND " +
           "(k.name LIKE %:keyword% OR k.description LIKE %:keyword%)")
    Page<KnowledgeBase> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 统计知识库数量
     */
    long countByIsDeleted(Integer isDeleted);
    
    /**
     * 统计知识库的文档数量
     */
    long countByIdAndIsDeleted(Long id, Integer isDeleted);
}
