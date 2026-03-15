-- 启用 PGVector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 验证扩展
SELECT * FROM pg_extension WHERE extname = 'vector';

-- 1. 知识库主表
CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    creator VARCHAR(64) NOT NULL,
    business_line VARCHAR(64) DEFAULT 'default',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER DEFAULT 0,
    CONSTRAINT uk_name_business_line UNIQUE (name, business_line)
);

CREATE INDEX IF NOT EXISTS idx_kb_business_line ON knowledge_base(business_line);
COMMENT ON TABLE knowledge_base IS '知识库主表';
COMMENT ON COLUMN knowledge_base.id IS '主键 ID';
COMMENT ON COLUMN knowledge_base.name IS '知识库名称';
COMMENT ON COLUMN knowledge_base.business_line IS '业务线标识';

-- 2. 文档表
CREATE TABLE IF NOT EXISTS kb_document (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    kb_id BIGINT NOT NULL REFERENCES knowledge_base(id) ON DELETE CASCADE,
    file_name VARCHAR(256) NOT NULL,
    file_type VARCHAR(32) NOT NULL,
    file_size BIGINT NOT NULL,
    file_path VARCHAR(512) NOT NULL,
    file_hash VARCHAR(64),
    version INT DEFAULT 1,
    status SMALLINT DEFAULT 0,
    error_msg VARCHAR(1024),
    chunk_count INT DEFAULT 0,
    creator VARCHAR(64) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_doc_kb_id_status ON kb_document(kb_id, status);
CREATE INDEX IF NOT EXISTS idx_doc_file_hash ON kb_document(file_hash);
COMMENT ON TABLE kb_document IS '文档表';
COMMENT ON COLUMN kb_document.status IS '处理状态：0-待处理，1-处理中，2-已完成，3-失败';

-- 3. 文档分块与向量表
CREATE TABLE IF NOT EXISTS kb_document_segment (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    doc_id BIGINT NOT NULL REFERENCES kb_document(id) ON DELETE CASCADE,
    kb_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536),
    segment_no INT NOT NULL,
    metadata JSONB,
    token_count INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_segment_kb_id ON kb_document_segment(kb_id);
CREATE INDEX IF NOT EXISTS idx_segment_doc_id ON kb_document_segment(doc_id);

-- 创建 HNSW 索引（向量检索加速）
CREATE INDEX IF NOT EXISTS idx_embedding_hnsw ON kb_document_segment 
USING hnsw (embedding vector_cosine_ops) 
WITH (m = 16, ef_construction = 64);

COMMENT ON TABLE kb_document_segment IS '文档分块与向量表';
COMMENT ON COLUMN kb_document_segment.embedding IS '向量表示（1536 维）';
