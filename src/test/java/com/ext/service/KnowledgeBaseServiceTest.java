package com.ext.service;

import com.ext.model.dto.CreateKbRequest;
import com.ext.model.dto.KnowledgeBaseResponse;
import com.ext.model.entity.KnowledgeBase;
import com.ext.repository.KnowledgeBaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class KnowledgeBaseServiceTest {
    
    @Autowired
    private KnowledgeBaseService kbService;
    
    @Autowired
    private KnowledgeBaseRepository kbRepository;
    
    @BeforeEach
    void setUp() {
        kbRepository.deleteAll();
    }
    
    @Test
    public void testCreateKnowledgeBase() {
        CreateKbRequest request = new CreateKbRequest();
        request.setName("测试知识库");
        request.setDescription("这是一个测试知识库");
        request.setBusinessLine("test");
        
        KnowledgeBase created = kbService.create(request, "test-user");
        
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("测试知识库", created.getName());
        assertEquals("test-user", created.getCreator());
        assertEquals("test", created.getBusinessLine());
        assertEquals(0, created.getIsDeleted());
    }
    
    @Test
    public void testDuplicateNameShouldThrowException() {
        CreateKbRequest request1 = new CreateKbRequest();
        request1.setName("重复名称");
        request1.setBusinessLine("test");
        kbService.create(request1, "user1");
        
        CreateKbRequest request2 = new CreateKbRequest();
        request2.setName("重复名称");
        request2.setBusinessLine("test");
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> kbService.create(request2, "user2")
        );
        
        assertEquals("该知识库已存在", exception.getMessage());
    }
    
    @Test
    public void testGetKnowledgeBaseList() {
        for (int i = 1; i <= 5; i++) {
            CreateKbRequest request = new CreateKbRequest();
            request.setName("知识库" + i);
            request.setBusinessLine("test");
            kbService.create(request, "test-user");
        }
        
        Page<KnowledgeBaseResponse> result = kbService.list(1, 10, null, null);
        
        assertEquals(5, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals(5, result.getContent().size());
    }
    
    @Test
    public void testGetKnowledgeBaseById() {
        CreateKbRequest request = new CreateKbRequest();
        request.setName("测试知识库");
        request.setBusinessLine("test");
        KnowledgeBase created = kbService.create(request, "test-user");
        
        KnowledgeBaseResponse response = kbService.getById(created.getId());
        
        assertNotNull(response);
        assertEquals(created.getId(), response.getId());
        assertEquals("测试知识库", response.getName());
    }
    
    @Test
    public void testUpdateKnowledgeBase() {
        CreateKbRequest request = new CreateKbRequest();
        request.setName("原名");
        request.setBusinessLine("test");
        KnowledgeBase created = kbService.create(request, "test-user");
        
        CreateKbRequest updateRequest = new CreateKbRequest();
        updateRequest.setName("新名称");
        updateRequest.setDescription("新描述");
        updateRequest.setBusinessLine("new-line");
        
        KnowledgeBase updated = kbService.update(created.getId(), updateRequest);
        
        assertEquals("新名称", updated.getName());
        assertEquals("新描述", updated.getDescription());
        assertEquals("new-line", updated.getBusinessLine());
    }
    
    @Test
    public void testDeleteKnowledgeBase() {
        CreateKbRequest request = new CreateKbRequest();
        request.setName("待删除");
        request.setBusinessLine("test");
        KnowledgeBase created = kbService.create(request, "test-user");
        
        kbService.delete(created.getId());
        
        KnowledgeBase deleted = kbRepository.findById(created.getId()).orElse(null);
        assertNotNull(deleted);
        assertEquals(1, deleted.getIsDeleted());
    }
    
    @Test
    public void testSearchByKeyword() {
        CreateKbRequest request1 = new CreateKbRequest();
        request1.setName("产品文档库");
        request1.setDescription("产品相关文档");
        request1.setBusinessLine("product");
        kbService.create(request1, "test-user");
        
        CreateKbRequest request2 = new CreateKbRequest();
        request2.setName("技术文档库");
        request2.setDescription("技术相关文档");
        request2.setBusinessLine("tech");
        kbService.create(request2, "test-user");
        
        Page<KnowledgeBaseResponse> result = kbService.list(1, 10, null, "产品");
        
        assertEquals(1, result.getTotalElements());
        assertEquals("产品文档库", result.getContent().get(0).getName());
    }
    
    @Test
    public void testFilterByBusinessLine() {
        CreateKbRequest request1 = new CreateKbRequest();
        request1.setName("产品库");
        request1.setBusinessLine("product");
        kbService.create(request1, "test-user");
        
        CreateKbRequest request2 = new CreateKbRequest();
        request2.setName("技术库");
        request2.setBusinessLine("tech");
        kbService.create(request2, "test-user");
        
        Page<KnowledgeBaseResponse> result = kbService.list(1, 10, "product", null);
        
        assertEquals(1, result.getTotalElements());
        assertEquals("产品库", result.getContent().get(0).getName());
    }
}
