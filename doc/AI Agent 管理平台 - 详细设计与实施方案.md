# AI Agent 管理平台 - 详细设计与实施方案

## 文档说明

本文档基于《开发阶段规划》制定，针对**阶段一（V1.0-MVP）**的每个功能模块提供：
- 详细的技术设计方案
- 数据库表结构定义
- API 接口规范
- 实施步骤与验证方法
- 测试用例设计

---

# 第一部分：知识库管理模块（Week 1）

## 1.1 数据库设计

### 1.1.1 核心表结构

```sql
-- 1. 知识库主表
CREATE TABLE knowledge_base (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    name VARCHAR(128) NOT NULL COMMENT '知识库名称',
    description VARCHAR(512) COMMENT '描述',
    creator VARCHAR(64) NOT NULL COMMENT '创建人',
    business_line VARCHAR(64) DEFAULT 'default' COMMENT '业务线标识',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记：0-未删除，1-已删除',
    UNIQUE KEY uk_name_business_line (name, business_line) COMMENT '同一业务线下知识库名称唯一',
    INDEX idx_business_line (business_line) COMMENT '业务线查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库主表';

-- 2. 文档表
CREATE TABLE kb_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    kb_id BIGINT NOT NULL COMMENT '所属知识库 ID',
    file_name VARCHAR(256) NOT NULL COMMENT '原始文件名',
    file_type VARCHAR(32) NOT NULL COMMENT '文件类型：PDF/DOCX/MD/TXT',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    file_path VARCHAR(512) NOT NULL COMMENT '对象存储路径',
    file_hash VARCHAR(64) COMMENT '文件 MD5 哈希值（用于去重）',
    version INT DEFAULT 1 COMMENT '版本号',
    status TINYINT DEFAULT 0 COMMENT '处理状态：0-待处理，1-处理中，2-已完成，3-失败',
    error_msg VARCHAR(1024) COMMENT '错误信息',
    chunk_count INT DEFAULT 0 COMMENT '分块数量',
    creator VARCHAR(64) NOT NULL COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    FOREIGN KEY (kb_id) REFERENCES knowledge_base(id) ON DELETE CASCADE,
    INDEX idx_kb_id_status (kb_id, status) COMMENT '按知识库和状态查询',
    INDEX idx_file_hash (file_hash) COMMENT '去重查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

-- 3. 文档分块与向量表（使用 PGVector）
CREATE TABLE kb_document_segment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    doc_id BIGINT NOT NULL COMMENT '所属文档 ID',
    kb_id BIGINT NOT NULL COMMENT '冗余知识库 ID 字段（便于查询）',
    content TEXT NOT NULL COMMENT '分块文本内容',
    embedding VECTOR(1536) COMMENT '向量表示（维度由嵌入模型决定）',
    segment_no INT NOT NULL COMMENT '分块序号',
    metadata JSON COMMENT '元数据：{page: 1, title: xxx}',
    token_count INT DEFAULT 0 COMMENT '分块的 token 数量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (doc_id) REFERENCES kb_document(id) ON DELETE CASCADE,
    INDEX idx_kb_id (kb_id) COMMENT '知识库检索索引',
    INDEX idx_doc_id (doc_id) COMMENT '文档分块查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档分块与向量表';

-- 4. 启用 PGVector 扩展（PostgreSQL）
-- CREATE EXTENSION IF NOT EXISTS vector;

-- 5. 创建 HNSW 索引（向量检索加速）
CREATE INDEX idx_embedding_hnsw ON kb_document_segment USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);
```

### 1.1.2 表关系图

```
knowledge_base (1) ──< (N) kb_document (1) ──< (N) kb_document_segment
     │                        │                          │
     │                        │                          │
 知识库基本信息            文档元数据                分块内容 + 向量
```

---

## 1.2 API 接口设计

### 1.2.1 知识库管理接口

#### **POST /api/v1/knowledge-base**
**功能**：创建知识库

**请求参数**：
```json
{
  "name": "产品文档库",
  "description": "存储产品相关的所有文档",
  "businessLine": "product"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "产品文档库",
    "description": "存储产品相关的所有文档",
    "creator": "zhangsan",
    "businessLine": "product",
    "createTime": "2026-03-15T10:00:00Z"
  }
}
```

**验证步骤**：
1. ✅ 调用接口创建知识库
2. ✅ 检查数据库 `knowledge_base` 表是否插入记录
3. ✅ 验证同一业务线下知识库名称唯一性约束
4. ✅ 验证必填字段校验（name 不能为空）

---

#### **GET /api/v1/knowledge-base**
**功能**：获取知识库列表（支持分页和筛选）

**请求参数**：
```
?page=1&size=10&businessLine=product&keyword=产品
```

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "total": 25,
    "list": [
      {
        "id": 1,
        "name": "产品文档库",
        "description": "存储产品相关的所有文档",
        "documentCount": 150,
        "createTime": "2026-03-15T10:00:00Z"
      }
    ]
  }
}
```

**验证步骤**：
1. ✅ 不带参数调用，返回所有知识库
2. ✅ 带 `businessLine` 参数，验证筛选结果
3. ✅ 带 `keyword` 参数，验证模糊匹配
4. ✅ 验证分页参数生效（总条数、当前页数据量）

---

#### **PUT /api/v1/knowledge-base/{id}**
**功能**：更新知识库信息

**请求参数**：
```json
{
  "name": "更新后的产品名称",
  "description": "更新后的描述"
}
```

**验证步骤**：
1. ✅ 调用接口更新知识库
2. ✅ 检查数据库记录是否更新
3. ✅ 验证不存在 ID 返回 404
4. ✅ 验证并发更新场景（乐观锁机制）

---

#### **DELETE /api/v1/knowledge-base/{id}**
**功能**：删除知识库（逻辑删除）

**验证步骤**：
1. ✅ 调用删除接口
2. ✅ 验证 `is_deleted` 字段变为 1
3. ✅ 验证关联文档也被级联删除或标记删除
4. ✅ 验证删除后无法查询到该知识库

---

### 1.2.2 文档管理接口

#### **POST /api/v1/knowledge-base/{kbId}/documents/upload**
**功能**：上传单个文档

**请求参数**：`multipart/form-data`
- `file`: 文件对象

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "fileName": "产品手册.pdf",
    "fileType": "PDF",
    "fileSize": 2048000,
    "status": 0,
    "message": "上传成功，正在处理"
  }
}
```

**验证步骤**：
1. ✅ 上传 PDF 文件，验证文件类型识别
2. ✅ 上传 DOCX 文件，验证解析
3. ✅ 上传超大文件（>50MB），验证大小限制
4. ✅ 上传重复文件，验证 MD5 去重机制
5. ✅ 检查 MinIO 存储路径是否正确

---

#### **POST /api/v1/knowledge-base/{kbId}/documents/batch-import**
**功能**：批量导入文档

**请求参数**：
```json
{
  "filePaths": [
    "/tmp/doc1.pdf",
    "/tmp/doc2.docx"
  ]
}
```

**验证步骤**：
1. ✅ 批量上传 10 个文件
2. ✅ 验证所有文件都进入处理队列
3. ✅ 检查部分文件失败时的错误处理
4. ✅ 验证批量导入进度查询接口

---

#### **GET /api/v1/knowledge-base/{kbId}/documents**
**功能**：获取文档列表

**请求参数**：
```
?status=2&page=1&size=20
```

**验证步骤**：
1. ✅ 按状态筛选文档
2. ✅ 验证文档列表包含分块数量统计
3. ✅ 验证文件预览接口（返回文件 URL）

---

#### **DELETE /api/v1/knowledge-base/documents/{id}**
**功能**：删除文档

**验证步骤**：
1. ✅ 删除文档后验证关联分块也被删除
2. ✅ 验证软删除机制（可恢复）
3. ✅ 验证删除正在处理的文档（异步任务终止）

---

### 1.2.3 文档处理接口

#### **POST /api/v1/knowledge-base/documents/{id}/vectorize**
**功能**：触发向量化处理

**验证步骤**：
1. ✅ 调用接口触发自定义处理任务
2. ✅ 验证文档状态变化：0→1→2
3. ✅ 检查 `kb_document_segment` 表生成分块记录
4. ✅ 验证向量维度正确性（1536 维）
5. ✅ 验证 HNSW 索引构建成功

---

#### **GET /api/v1/knowledge-base/documents/{id}/processing-status**
**功能**：查询文档处理进度

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "status": 1,
    "progress": 60,
    "currentStep": "正在生成向量",
    "totalChunks": 25,
    "processedChunks": 15
  }
}
```

**验证步骤**：
1. ✅ 轮询接口查看处理进度
2. ✅ 验证进度百分比准确性
3. ✅ 验证处理失败时的错误信息

---

### 1.2.4 检索测试接口

#### **POST /api/v1/knowledge-base/retrieve**
**功能**：知识库检索测试

**请求参数**：
```json
{
  "kbId": 1,
  "query": "如何安装产品？",
  "topK": 10,
  "scoreThreshold": 0.7
}
```

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "query": "如何安装产品？",
    "costTime": 45,
    "results": [
      {
        "segmentId": 101,
        "content": "安装步骤如下：1. 下载安装包...",
        "score": 0.92,
        "source": {
          "fileName": "产品安装手册.pdf",
          "pageNumber": 3
        }
      },
      {
        "segmentId": 102,
        "content": "系统要求：Windows 10 及以上...",
        "score": 0.85,
        "source": {
          "fileName": "产品安装手册.pdf",
          "pageNumber": 1
        }
      }
    ]
  }
}
```

**验证步骤**：
1. ✅ 输入明确问题，验证召回相关性
2. ✅ 调整 `topK` 参数，验证返回数量
3. ✅ 调整 `scoreThreshold`，验证过滤效果
4. ✅ 验证响应耗时 <200ms
5. ✅ 验证空结果处理（无匹配内容）
6. ✅ 验证多知识库混合检索（后续扩展）

---

## 1.3 核心组件设计

### 1.3.1 文档解析引擎

**类图设计**：
```java
public interface DocumentParser {
    /**
     * 解析文档，提取文本内容
     * @param filePath 文件路径
     * @return 解析后的文本
     */
    String parse(String filePath);
    
    /**
     * 支持的文档类型
     */
    List<String> getSupportedTypes();
}

// PDF 解析器实现
@Component
public class PdfDocumentParser implements DocumentParser {
    @Override
    public String parse(String filePath) {
        // 使用 PdfBox 提取文本
    }
    
    @Override
    public List<String> getSupportedTypes() {
        return Arrays.asList("PDF");
    }
}

// Word 解析器实现
@Component
public class WordDocumentParser implements DocumentParser {
    @Override
    public String parse(String filePath) {
        // 使用 Apache POI 提取文本
    }
}
```

**验证测试**：
```java
@Test
public void testPdfParser() {
    // 1. 准备测试 PDF 文件
    // 2. 调用解析器
    // 3. 验证提取的文本非空
    // 4. 验证文本编码正确（无乱码）
}

@Test
public void testWordParser() {
    // 1. 准备测试 DOCX 文件
    // 2. 调用解析器
    // 3. 验证表格内容正确提取
    // 4. 验证标题层级保留
}
```

---

### 1.3.2 文本分块策略

**分块算法实现**：
```java
public interface ChunkingStrategy {
    /**
     * 将文本分割为多个块
     * @param text 原始文本
     * @return 分块列表
     */
    List<TextChunk> chunk(String text);
}

// 固定长度分块
@Component
public class FixedLengthChunking implements ChunkingStrategy {
    private int chunkSize = 500;  // 每块字符数
    private int overlap = 50;     // 重叠字符数
    
    @Override
    public List<TextChunk> chunk(String text) {
        List<TextChunk> chunks = new ArrayList<>();
        int start = 0;
        int chunkNo = 1;
        
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            String content = text.substring(start, end);
            
            chunks.add(new TextChunk(chunkNo++, content));
            start += (chunkSize - overlap);
        }
        
        return chunks;
    }
}

// 语义分块（基于段落）
@Component
public class SemanticChunking implements ChunkingStrategy {
    @Override
    public List<TextChunk> chunk(String text) {
        // 按段落分割
        String[] paragraphs = text.split("\\n\\n");
        List<TextChunk> chunks = new ArrayList<>();
        
        for (int i = 0; i < paragraphs.length; i++) {
            if (!paragraphs[i].trim().isEmpty()) {
                chunks.add(new TextChunk(i + 1, paragraphs[i]));
            }
        }
        
        return chunks;
    }
}
```

**验证测试**：
```java
@Test
public void testFixedLengthChunking() {
    String text = "这是一个长文本..."; // 2000 字符
    List<TextChunk> chunks = strategy.chunk(text);
    
    // 验证点：
    // 1. 每块大小约 500 字符（允许±10%）
    // 2. 相邻块之间有 50 字符重叠
    // 3. 最后一块可以小于 500 字符
    // 4. 所有块拼接后等于原文本（去除重叠）
}

@Test
public void testSemanticChunking() {
    // 验证点：
    // 1. 按自然段落分割
    // 2. 保留段落语义完整性
    // 3. 空段落被过滤
}
```

---

### 1.3.3 向量化服务

**嵌入模型调用**：
```java
@Service
public class EmbeddingService {
    
    @Autowired
    private DashScopeApi dashScopeApi;  // Spring AI 封装
    
    /**
     * 生成文本向量
     */
    public float[] embed(String text) {
        EmbeddingRequest request = new EmbeddingRequest(
            text,
            "text-embedding-v2",  // 通义千问嵌入模型
            1536                   // 向量维度
        );
        
        EmbeddingResponse response = dashScopeApi.call(request);
        return response.getEmbedding();
    }
    
    /**
     * 批量生成向量
     */
    public List<float[]> batchEmbed(List<String> texts) {
        // 批量调用优化（减少 API 次数）
        return texts.stream()
            .map(this::embed)
            .collect(Collectors.toList());
    }
}
```

**验证测试**：
```java
@Test
public void testEmbedding() {
    String text = "人工智能是研究使计算机模拟人类智能的学科";
    float[] vector = embeddingService.embed(text);
    
    // 验证点：
    // 1. 向量维度为 1536
    // 2. 向量已归一化（模长为 1）
    // 3. 相似文本的向量余弦相似度高
    // 4. 不同文本的向量余弦相似度低
}

@Test
public void testEmbeddingSimilarity() {
    String text1 = "如何学习 Java 编程？";
    String text2 = "Java 编程入门方法";
    String text3 = "今天天气不错";
    
    float[] v1 = embeddingService.embed(text1);
    float[] v2 = embeddingService.embed(text2);
    float[] v3 = embeddingService.embed(text3);
    
    double similarity12 = cosineSimilarity(v1, v2);
    double similarity13 = cosineSimilarity(v1, v3);
    
    // 验证：similarity12 > similarity13
    Assertions.assertTrue(similarity12 > similarity13);
}

private double cosineSimilarity(float[] a, float[] b) {
    // 计算余弦相似度
}
```

---

### 1.3.4 异步处理任务

**文档处理流程编排**：
```java
@Service
public class DocumentProcessingService {
    
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;
    
    @Autowired
    private DocumentParser parser;
    
    @Autowired
    private ChunkingStrategy chunkingStrategy;
    
    @Autowired
    private EmbeddingService embeddingService;
    
    @Autowired
    private DocumentSegmentRepository segmentRepository;
    
    /**
     * 异步处理文档
     */
    @Async("documentProcessor")
    public void processDocument(Long documentId) {
        try {
            // 1. 更新状态为处理中
            updateStatus(documentId, ProcessingStatus.PROCESSING);
            
            // 2. 读取文档
            KbDocument doc = documentRepository.findById(documentId);
            String text = parser.parse(doc.getFilePath());
            
            // 3. 文本清洗
            text = cleanText(text);
            
            // 4. 分块
            List<TextChunk> chunks = chunkingStrategy.chunk(text);
            
            // 5. 生成向量并保存
            for (int i = 0; i < chunks.size(); i++) {
                TextChunk chunk = chunks.get(i);
                float[] vector = embeddingService.embed(chunk.getContent());
                
                KbDocumentSegment segment = new KbDocumentSegment();
                segment.setDocId(documentId);
                segment.setKbId(doc.getKbId());
                segment.setSegmentNo(i + 1);
                segment.setContent(chunk.getContent());
                segment.setEmbedding(vector);
                segment.setTokenCount(countTokens(chunk.getContent()));
                
                segmentRepository.save(segment);
                
                // 更新进度
                updateProgress(documentId, (i + 1) * 100 / chunks.size());
            }
            
            // 6. 构建 HNSW 索引
            buildHnswIndex(doc.getKbId());
            
            // 7. 更新状态为完成
            updateStatus(documentId, ProcessingStatus.COMPLETED);
            
        } catch (Exception e) {
            log.error("文档处理失败", e);
            updateStatus(documentId, ProcessingStatus.FAILED, e.getMessage());
        }
    }
    
    private String cleanText(String text) {
        // 去除多余空白、特殊字符
        return text.replaceAll("\\s+", " ").trim();
    }
    
    private int countTokens(String text) {
        // 估算 token 数量（中文按字符数，英文按单词数）
        return text.length();
    }
    
    private void buildHnswIndex(Long kbId) {
        // 调用数据库存储过程或 JDBC 执行索引构建
        jdbcTemplate.execute(
            "CREATE INDEX IF NOT EXISTS idx_embedding_hnsw_" + kbId + 
            " ON kb_document_segment USING hnsw (embedding vector_cosine_ops)"
        );
    }
}
```

**验证测试**：
```java
@Test
public void testDocumentProcessingFlow() throws Exception {
    // 1. 上传测试文档
    Long docId = uploadTestDocument("test.pdf");
    
    // 2. 触发处理
    processingService.processDocument(docId);
    
    // 3. 等待异步任务完成（最多 30 秒）
    await().atMost(30, SECONDS).until(() -> 
        documentRepository.findById(docId).getStatus() == ProcessingStatus.COMPLETED
    );
    
    // 4. 验证分块已生成
    List<KbDocumentSegment> segments = segmentRepository.findByDocId(docId);
    Assertions.assertTrue(segments.size() > 0);
    
    // 5. 验证向量已生成
    segments.forEach(seg -> {
        Assertions.assertNotNull(seg.getEmbedding());
        Assertions.assertEquals(1536, seg.getEmbedding().length);
    });
    
    // 6. 验证 HNSW 索引存在
    verifyHnswIndexExists();
}
```

---

## 1.4 配置文件

### 1.4.1 application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ai_platform
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:your_password}
    driver-class-name: org.postgresql.Driver
    
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
      
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
      
aigc:
  dashscope:
    api-key: ${DASHSCOPE_API_KEY}
    base-url: https://dashscope.aliyuncs.com/api/v1
    
document:
  parser:
    # 文档解析配置
    supported-types: PDF,DOCX,MD,TXT,XLSX
    # 临时文件目录
    temp-dir: /tmp/ai-platform/docs
    # 最大文件大小（MB）
    max-size: 50
    
  chunking:
    # 默认分块策略
    default-strategy: fixed_length
    # 固定长度分块大小
    chunk-size: 500
    # 重叠窗口大小
    overlap: 50
    
  embedding:
    # 嵌入模型配置
    model: text-embedding-v2
    dimension: 1536
    # 批量处理大小
    batch-size: 100
    
  storage:
    # MinIO 对象存储配置
    type: minio
    endpoint: http://localhost:9000
    access-key: ${MINIO_ACCESS_KEY}
    secret-key: ${MINIO_SECRET_KEY}
    bucket: ai-platform-docs
    
async:
  executor:
    # 文档处理线程池
    core-pool-size: 4
    max-pool-size: 8
    queue-capacity: 100
    thread-name-prefix: doc-processor-
```

---

## 1.5 单元测试设计

### 1.5.1 知识库服务测试

```java
@SpringBootTest
@Transactional
class KnowledgeBaseServiceTest {
    
    @Autowired
    private KnowledgeBaseService kbService;
    
    @Autowired
    private KnowledgeBaseRepository kbRepository;
    
    @Test
    @DisplayName("创建知识库 - 正常场景")
    void testCreateKnowledgeBase() {
        // Given
        CreateKbRequest request = new CreateKbRequest();
        request.setName("测试知识库");
        request.setDescription("用于测试");
        request.setBusinessLine("test");
        
        // When
        KnowledgeBase kb = kbService.create(request);
        
        // Then
        Assertions.assertNotNull(kb.getId());
        Assertions.assertEquals("测试知识库", kb.getName());
        Assertions.assertEquals("test", kb.getBusinessLine());
    }
    
    @Test
    @DisplayName("创建知识库 - 名称重复")
    void testCreateDuplicateKnowledgeBase() {
        // Given
        kbService.create(createRequest("重复名称", "test"));
        
        // When & Then
        assertThrows(DuplicateResourceException.class, () -> {
            kbService.create(createRequest("重复名称", "test"));
        });
    }
    
    @Test
    @DisplayName("查询知识库列表 - 分页")
    void testListKnowledgeBases() {
        // Given: 创建 25 个知识库
        for (int i = 0; i < 25; i++) {
            kbService.create(createRequest("知识库" + i, "test"));
        }
        
        // When: 查询第 1 页，每页 10 条
        PageResult<KnowledgeBase> result = kbService.list(1, 10, "test", null);
        
        // Then
        Assertions.assertEquals(25, result.getTotal());
        Assertions.assertEquals(10, result.getList().size());
        Assertions.assertEquals(3, result.getTotalPages());
    }
    
    @Test
    @DisplayName("删除知识库 - 级联删除文档")
    void testDeleteKnowledgeBaseWithCascade() {
        // Given
        KnowledgeBase kb = kbService.create(createRequest("测试库", "test"));
        uploadDocument(kb.getId(), "doc1.pdf");
        uploadDocument(kb.getId(), "doc2.pdf");
        
        // When
        kbService.delete(kb.getId());
        
        // Then
        Assertions.assertTrue(kb.getIsDeleted());
        List<KbDocument> docs = documentRepository.findByKbId(kb.getId());
        Assertions.assertTrue(docs.stream().allMatch(KbDocument::getIsDeleted));
    }
}
```

### 1.5.2 文档解析测试

```java
class DocumentParserTest {
    
    @Test
    void testPdfParserExtractText() throws IOException {
        // Given
        String pdfPath = "src/test/resources/sample.pdf";
        PdfDocumentParser parser = new PdfDocumentParser();
        
        // When
        String text = parser.parse(pdfPath);
        
        // Then
        Assertions.assertNotNull(text);
        Assertions.assertTrue(text.contains("测试内容"));
        Assertions.assertFalse(text.isEmpty());
    }
    
    @Test
    void testWordParserExtractTable() throws IOException {
        // Given
        String docxPath = "src/test/resources/sample.docx";
        WordDocumentParser parser = new WordDocumentParser();
        
        // When
        String text = parser.parse(docxPath);
        
        // Then
        Assertions.assertNotNull(text);
        // 验证表格内容被正确提取
        Assertions.assertTrue(text.contains("表格数据"));
    }
    
    @Test
    void testUnsupportedFileType() {
        // Given
        String pptxPath = "src/test/resources/sample.pptx";
        PdfDocumentParser parser = new PdfDocumentParser();
        
        // When & Then
        assertThrows(UnsupportedFileTypeException.class, () -> {
            parser.parse(pptxPath);
        });
    }
}
```

### 1.5.3 检索性能测试

```java
@SpringBootTest
class RetrievalPerformanceTest {
    
    @Autowired
    private KnowledgeBaseSearchService searchService;
    
    @Test
    @DisplayName("向量检索性能测试 - 1000+ 文档")
    void testRetrievalPerformance() {
        // Given: 准备 1000 个文档的分块
        prepareTestData(1000);
        
        String query = "测试查询问题";
        RetrieveRequest request = new RetrieveRequest();
        request.setQuery(query);
        request.setTopK(10);
        
        // When
        long startTime = System.currentTimeMillis();
        RetrieveResult result = searchService.retrieve(request);
        long endTime = System.currentTimeMillis();
        
        // Then
        long costTime = endTime - startTime;
        Assertions.assertTrue(costTime < 200, "检索耗时应小于 200ms");
        Assertions.assertEquals(10, result.getResults().size());
        
        System.out.println("检索耗时：" + costTime + "ms");
        System.out.println("召回片段数：" + result.getResults().size());
    }
    
    @Test
    @DisplayName("不同 topK 参数对性能的影响")
    void testTopKImpact() {
        int[] topKValues = {5, 10, 20, 50};
        
        for (int k : topKValues) {
            RetrieveRequest request = new RetrieveRequest();
            request.setQuery("测试");
            request.setTopK(k);
            
            long start = System.currentTimeMillis();
            searchService.retrieve(request);
            long cost = System.currentTimeMillis() - start;
            
            System.out.printf("topK=%d, 耗时=%dms%n", k, cost);
            
            // 验证耗时增长在合理范围内
            Assertions.assertTrue(cost < 500);
        }
    }
}
```

---

## 1.6 集成测试场景

### 1.6.1 端到端流程测试

```java
@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeBaseE2ETest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @DisplayName("完整流程：创建知识库→上传文档→处理→检索")
    void testEndToEndFlow() throws Exception {
        // Step 1: 创建知识库
        String kbResponse = mockMvc.perform(post("/api/v1/knowledge-base")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"测试库\",\"businessLine\":\"test\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        
        Long kbId = JsonPath.read(kbResponse, "$.data.id");
        
        // Step 2: 上传文档
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            new FileInputStream("src/test/resources/test.pdf")
        );
        
        mockMvc.perform(multipart("/api/v1/knowledge-base/{kbId}/documents/upload", kbId)
                .file(file))
            .andExpect(status().isOk());
        
        // Step 3: 等待处理完成（轮询）
        await().atMost(30, SECONDS).until(() -> {
            String docStatus = mockMvc.perform(get("/api/v1/knowledge-base/documents/1/processing-status"))
                .andReturn().getResponse().getContentAsString();
            return JsonPath.read(docStatus, "$.data.status").equals(2);
        });
        
        // Step 4: 执行检索
        String searchResponse = mockMvc.perform(post("/api/v1/knowledge-base/retrieve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"kbId\":" + kbId + ",\"query\":\"测试内容\",\"topK\":5}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        
        // 验证检索结果
        List<Map> results = JsonPath.read(searchResponse, "$.data.results");
        Assertions.assertTrue(results.size() > 0);
        Assertions.assertTrue(results.get(0).containsKey("content"));
        Assertions.assertTrue(results.get(0).containsKey("score"));
    }
}
```

---

## 1.7 验收标准检查清单

### 功能完整性
- [ ] 知识库 CRUD 全部实现
- [ ] 文档上传支持 PDF/DOCX/MD/TXT
- [ ] 文档解析准确率 ≥95%
- [ ] 分块策略可配置
- [ ] 向量化处理自动化
- [ ] 检索接口响应 <200ms

### 数据质量
- [ ] 向量维度正确（1536 维）
- [ ] 向量已归一化
- [ ] HNSW 索引构建成功
- [ ] 元数据完整（文件名、页码等）

### 异常处理
- [ ] 不支持的文件类型友好提示
- [ ] 超大文件拒绝并提示
- [ ] 解析失败记录错误日志
- [ ] 处理失败支持重试

### 性能指标
- [ ] 单文档处理时间 <30 秒（100 页内）
- [ ] 检索延迟 <200ms（1000+ 文档）
- [ ] 并发上传不阻塞
- [ ] 内存占用合理（无泄漏）

### 文档完备性
- [ ] API 文档完整（Swagger）
- [ ] 部署手册清晰
- [ ] 用户使用指南完整
- [ ] 常见问题 FAQ

---

# 第二部分：RAG 配置模块（Week 2）

## 2.1 数据库设计

```sql
-- RAG 配置表
CREATE TABLE rag_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    name VARCHAR(128) NOT NULL COMMENT '配置名称',
    description VARCHAR(512) COMMENT '描述',
    kb_ids JSON NOT NULL COMMENT '关联的知识库 ID 列表',
    retrieval_type VARCHAR(32) DEFAULT 'vector' COMMENT '检索类型：vector/keyword/hybrid',
    top_k INT DEFAULT 10 COMMENT '返回最相似的 K 个结果',
    ef_search INT DEFAULT 40 COMMENT 'HNSW 搜索参数',
    score_threshold DECIMAL(5,4) COMMENT '最低相似度阈值',
    use_rerank TINYINT DEFAULT 0 COMMENT '是否启用重排序：0-否，1-是',
    rerank_model VARCHAR(64) COMMENT '重排序模型名称',
    cache_seconds INT DEFAULT 300 COMMENT '缓存时间（秒），0 表示不缓存',
    creator VARCHAR(64) NOT NULL COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_name (name) COMMENT '名称查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG 配置表';

-- GIN 索引加速 JSON 查询（PostgreSQL）
-- CREATE INDEX idx_kb_ids ON rag_config USING GIN (kb_ids);
```

---

## 2.2 API 接口设计

### **POST /api/v1/rag/config**
**功能**：创建 RAG 配置

**请求参数**：
```json
{
  "name": "产品知识检索",
  "description": "用于产品咨询的检索配置",
  "kbIds": [1, 2, 3],
  "retrievalType": "hybrid",
  "topK": 10,
  "efSearch": 40,
  "scoreThreshold": 0.7,
  "useRerank": true,
  "rerankModel": "bge-reranker",
  "cacheSeconds": 300
}
```

**验证步骤**：
1. ✅ 创建基础向量检索配置
2. ✅ 创建混合检索配置
3. ✅ 验证必填字段校验（name、kbIds）
4. ✅ 验证知识库 ID 有效性（至少一个存在）
5. ✅ 验证参数范围（topK: 1-100, efSearch: 10-100）

---

### **POST /api/v1/rag/retrieve**
**功能**：执行 RAG 检索

**请求参数**：
```json
{
  "ragConfigId": 1,
  "query": "产品的价格是多少？",
  "userId": "user123"  // 用于缓存隔离
}
```

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "query": "产品的价格是多少？",
    "ragConfigName": "产品知识检索",
    "retrievalType": "hybrid",
    "costTime": 68,
    "cacheHit": false,
    "results": [
      {
        "content": "产品定价如下：基础版 999 元/年...",
        "score": 0.94,
        "source": {
          "kbId": 1,
          "kbName": "产品文档库",
          "fileName": "产品价格手册.pdf",
          "segmentNo": 5
        },
        "metadata": {
          "retrievalSource": "vector",  // vector/keyword
          "rerankScore": 0.91           // 重排序后的分数
        }
      }
    ],
    "statistics": {
      "vectorRetrieveCount": 10,
      "keywordRetrieveCount": 8,
      "mergedCount": 12,
      "rerankedCount": 10
    }
  }
}
```

**验证步骤**：
1. ✅ 向量检索模式：仅使用向量搜索
2. ✅ 关键词检索模式：仅使用 ES 搜索
3. ✅ 混合检索模式：两者结合，验证合并逻辑
4. ✅ 缓存命中：相同查询第二次请求应命中缓存
5. ✅ 重排序：验证 rerank 前后分数变化
6. ✅ 参数透传：topK、threshold 生效
7. ✅ 多知识库：结果来自多个知识库
8. ✅ 统计信息：各阶段召回数量准确

---

文档太长和ai交流浪费token（文档继续...）