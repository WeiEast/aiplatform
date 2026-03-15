package com.ext.repository;

import com.ext.model.entity.KbDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KbDocumentRepository extends JpaRepository<KbDocument, Long> {
    
    /**
     * 查询知识库下的文档列表
     */
    Page<KbDocument> findByKbIdAndIsDeleted(Long kbId, Integer isDeleted, Pageable pageable);
    
    /**
     * 按状态筛选
     */
    Page<KbDocument> findByKbIdAndStatusAndIsDeleted(Long kbId, Integer status, Integer isDeleted, Pageable pageable);
    
    /**
     * 根据文件哈希查询
     */
    Optional<KbDocument> findByFileHashAndKbIdAndIsDeleted(String fileHash, Long kbId, Integer isDeleted);
    
    /**
     * 统计知识库的文档数量
     */
    long countByKbIdAndIsDeleted(Long kbId, Integer isDeleted);
    
    /**
     * 批量更新文档状态
     */
    @Modifying
    @Query("UPDATE KbDocument d SET d.status = :status, d.errorMsg = :errorMsg WHERE d.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("errorMsg") String errorMsg);
    
    /**
     * 查询待处理的文档
     */
    List<KbDocument> findByStatusAndIsDeleted(Integer status, Integer isDeleted);
}
