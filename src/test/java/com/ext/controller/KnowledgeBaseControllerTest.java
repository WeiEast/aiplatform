package com.ext.controller;

import com.ext.model.dto.CreateKbRequest;
import com.ext.model.entity.KnowledgeBase;
import com.ext.repository.KnowledgeBaseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class KnowledgeBaseControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private KnowledgeBaseRepository kbRepository;
    
    @BeforeEach
    void setUp() {
        kbRepository.deleteAll();
    }
    
    @Test
    public void testCreateKnowledgeBase() throws Exception {
        CreateKbRequest request = new CreateKbRequest();
        request.setName("测试知识库");
        request.setDescription("这是一个测试知识库");
        request.setBusinessLine("test");
        
        mockMvc.perform(post("/api/v1/knowledge-base")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", "test-user")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name").value("测试知识库"))
                .andExpect(jsonPath("$.creator").value("test-user"))
                .andExpect(jsonPath("$.businessLine").value("test"));
    }
    
    @Test
    public void testCreateKnowledgeBaseWithDuplicateName() throws Exception {
        // 创建第一个知识库
        KnowledgeBase kb1 = new KnowledgeBase();
        kb1.setName("重复名称");
        kb1.setBusinessLine("test");
        kb1.setCreator("user1");
        kbRepository.save(kb1);
        
        // 尝试创建同名知识库
        CreateKbRequest request = new CreateKbRequest();
        request.setName("重复名称");
        request.setBusinessLine("test");
        
        mockMvc.perform(post("/api/v1/knowledge-base")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", "user2")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }
    
    @Test
    public void testGetKnowledgeBaseList() throws Exception {
        // 创建测试数据
        for (int i = 1; i <= 5; i++) {
            KnowledgeBase kb = new KnowledgeBase();
            kb.setName("知识库" + i);
            kb.setBusinessLine("test");
            kb.setCreator("test-user");
            kbRepository.save(kb);
        }
        
        mockMvc.perform(get("/api/v1/knowledge-base")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(5));
    }
    
    @Test
    public void testGetKnowledgeBaseById() throws Exception {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName("测试知识库");
        kb.setBusinessLine("test");
        kb.setCreator("test-user");
        kb = kbRepository.save(kb);
        
        mockMvc.perform(get("/api/v1/knowledge-base/{id}", kb.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(kb.getId()))
                .andExpect(jsonPath("$.name").value("测试知识库"));
    }
    
    @Test
    public void testUpdateKnowledgeBase() throws Exception {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName("原名");
        kb.setBusinessLine("test");
        kb.setCreator("test-user");
        kb = kbRepository.save(kb);
        
        CreateKbRequest request = new CreateKbRequest();
        request.setName("新名称");
        request.setDescription("新描述");
        request.setBusinessLine("new-line");
        
        mockMvc.perform(put("/api/v1/knowledge-base/{id}", kb.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("新名称"))
                .andExpect(jsonPath("$.description").value("新描述"))
                .andExpect(jsonPath("$.businessLine").value("new-line"));
    }
    
    @Test
    public void testDeleteKnowledgeBase() throws Exception {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName("待删除");
        kb.setBusinessLine("test");
        kb.setCreator("test-user");
        kb = kbRepository.save(kb);
        
        mockMvc.perform(delete("/api/v1/knowledge-base/{id}", kb.getId()))
                .andExpect(status().isOk());
        
        // 验证逻辑删除
        KnowledgeBase deleted = kbRepository.findById(kb.getId()).orElse(null);
        assert deleted != null;
        assert deleted.getIsDeleted() == 1;
    }
    
    @Test
    public void testSearchByKeyword() throws Exception {
        KnowledgeBase kb1 = new KnowledgeBase();
        kb1.setName("产品文档");
        kb1.setDescription("产品相关文档");
        kb1.setBusinessLine("product");
        kbRepository.save(kb1);
        
        KnowledgeBase kb2 = new KnowledgeBase();
        kb2.setName("技术文档");
        kb2.setDescription("技术相关文档");
        kb2.setBusinessLine("tech");
        kbRepository.save(kb2);
        
        mockMvc.perform(get("/api/v1/knowledge-base")
                .param("keyword", "产品"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("产品文档"));
    }
}
