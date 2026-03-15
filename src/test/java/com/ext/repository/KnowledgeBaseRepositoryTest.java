package com.ext.repository;

import com.ext.model.entity.KnowledgeBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class KnowledgeBaseRepositoryTest {
    
    @Autowired
    private KnowledgeBaseRepository kbRepository;
    
    @BeforeEach
    void setUp() {
        kbRepository.deleteAll();
    }
    
    @Test
    public void testSaveAndFind() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName("测试知识库");
        kb.setBusinessLine("test");
        kb.setCreator("test-user");
        
        KnowledgeBase saved = kbRepository.save(kb);
        
        Optional<KnowledgeBase> found = kbRepository.findById(saved.getId());
        
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("测试知识库");
        assertThat(found.get().getCreator()).isEqualTo("test-user");
    }
    
    @Test
    public void testFindByNameAndBusinessLine() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName("唯一名称");
        kb.setBusinessLine("test");
        kb.setCreator("test-user");
        kbRepository.save(kb);
        
        Optional<KnowledgeBase> found = kbRepository.findByNameAndBusinessLineAndIsDeleted(
            "唯一名称", "test", 0
        );
        
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("唯一名称");
    }
    
    @Test
    public void testFindByIsDeleted() {
        for (int i = 0; i < 5; i++) {
            KnowledgeBase kb = new KnowledgeBase();
            kb.setName("知识库" + i);
            kb.setBusinessLine("test");
            kb.setCreator("test-user");
            kb.setIsDeleted(0);
            kbRepository.save(kb);
        }
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<KnowledgeBase> result = kbRepository.findByIsDeletedOrderByCreateTimeDesc(0, pageable);
        
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getContent()).hasSize(5);
    }
    
    @Test
    public void testSearchByKeyword() {
        KnowledgeBase kb1 = new KnowledgeBase();
        kb1.setName("产品文档");
        kb1.setDescription("产品相关");
        kb1.setBusinessLine("product");
        kbRepository.save(kb1);
        
        KnowledgeBase kb2 = new KnowledgeBase();
        kb2.setName("技术文档");
        kb2.setDescription("技术相关");
        kb2.setBusinessLine("tech");
        kbRepository.save(kb2);
        
        Pageable pageable = PageRequest.of(0, 10);
        Page<KnowledgeBase> result = kbRepository.searchByKeyword("产品", pageable);
        
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("产品文档");
    }
    
    @Test
    public void testCountByIsDeleted() {
        for (int i = 0; i < 3; i++) {
            KnowledgeBase kb = new KnowledgeBase();
            kb.setName("知识库" + i);
            kb.setBusinessLine("test");
            kb.setCreator("test-user");
            kb.setIsDeleted(0);
            kbRepository.save(kb);
        }
        
        long count = kbRepository.countByIsDeleted(0);
        
        assertThat(count).isEqualTo(3);
    }
}
