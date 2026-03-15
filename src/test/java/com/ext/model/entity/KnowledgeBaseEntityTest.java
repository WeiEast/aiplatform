package com.ext.model.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class KnowledgeBaseEntityTest {
    
    @Test
    public void testEntityCreation() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName("测试知识库");
        kb.setDescription("这是一个测试");
        kb.setCreator("test-user");
        kb.setBusinessLine("test");
        
        assertEquals("测试知识库", kb.getName());
        assertEquals("这是一个测试", kb.getDescription());
        assertEquals("test-user", kb.getCreator());
        assertEquals("test", kb.getBusinessLine());
        assertEquals(0, kb.getIsDeleted());
    }
    
    @Test
    public void testPrePersist() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName("测试");
        kb.setCreator("user");
        
        assertNull(kb.getCreateTime());
        assertNull(kb.getUpdateTime());
        
        kb.onCreate();
        
        assertNotNull(kb.getCreateTime());
        assertNotNull(kb.getUpdateTime());
    }
    
    @Test
    public void testPreUpdate() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName("测试");
        kb.setCreator("user");
        
        kb.onUpdate();
        
        assertNotNull(kb.getUpdateTime());
    }
}
