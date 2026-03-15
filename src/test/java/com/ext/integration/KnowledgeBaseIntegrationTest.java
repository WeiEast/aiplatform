package com.ext.integration;

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

/**
 * 知识库管理模块端到端集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class KnowledgeBaseIntegrationTest {
    
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
    
    /**
     * 测试完整的知识库创建流程
     */
    @Test
    public void testFullCreateFlow() throws Exception {
        // 1. 创建知识库
        CreateKbRequest request = new CreateKbRequest();
        request.setName("产品文档库");
        request.setDescription("存储产品相关文档");
        request.setBusinessLine("product");
        
        String createResponse = mockMvc.perform(post("/api/v1/knowledge-base")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", "product-manager")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name").value("产品文档库"))
                .andExpect(jsonPath("$.creator").value("product-manager"))
                .andExpect(jsonPath("$.businessLine").value("product"))
                .andReturn().getResponse().getContentAsString();
        
        Long kbId = objectMapper.readTree(createResponse).get("id").asLong();
        
        // 2. 查询知识库列表
        mockMvc.perform(get("/api/v1/knowledge-base")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(kbId));
        
        // 3. 查询知识库详情
        mockMvc.perform(get("/api/v1/knowledge-base/{id}", kbId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(kbId))
                .andExpect(jsonPath("$.name").value("产品文档库"));
    }
    
    /**
     * 测试知识库的增删改查完整流程
     */
    @Test
    public void testCRUDFlow() throws Exception {
        // CREATE
        CreateKbRequest createRequest = new CreateKbRequest();
        createRequest.setName("临时知识库");
        createRequest.setBusinessLine("temp");
        
        String createResponse = mockMvc.perform(post("/api/v1/knowledge-base")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        
        Long kbId = objectMapper.readTree(createResponse).get("id").asLong();
        
        // READ
        mockMvc.perform(get("/api/v1/knowledge-base/{id}", kbId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("临时知识库"));
        
        // UPDATE
        CreateKbRequest updateRequest = new CreateKbRequest();
        updateRequest.setName("更新后的知识库");
        updateRequest.setDescription("已更新描述");
        
        mockMvc.perform(put("/api/v1/knowledge-base/{id}", kbId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("更新后的知识库"));
        
        // DELETE
        mockMvc.perform(delete("/api/v1/knowledge-base/{id}", kbId))
                .andExpect(status().isOk());
        
        // Verify soft delete
        KnowledgeBase deleted = kbRepository.findById(kbId).orElse(null);
        assert deleted != null;
        assert deleted.getIsDeleted() == 1;
    }
    
    /**
     * 测试分页和搜索功能
     */
    @Test
    public void testPaginationAndSearch() throws Exception {
        // 创建多个知识库
        for (int i = 1; i <= 15; i++) {
            CreateKbRequest request = new CreateKbRequest();
            request.setName("知识库" + i);
            request.setBusinessLine(i % 3 == 0 ? "product" : "tech");
            kbRepository.save(new KnowledgeBase());
        }
        
        // 测试分页
        mockMvc.perform(get("/api/v1/knowledge-base")
                .param("page", "1")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(15));
        
        // 测试关键词搜索
        mockMvc.perform(get("/api/v1/knowledge-base")
                .param("keyword", "知识库 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))));
        
        // 测试业务线筛选
        mockMvc.perform(get("/api/v1/knowledge-base")
                .param("businessLine", "product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)));
    }
}
