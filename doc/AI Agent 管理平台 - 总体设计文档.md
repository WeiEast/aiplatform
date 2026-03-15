# AI Agent 管理平台 - 总体设计文档（优化版）

## 一、文档概述

### 1.1 文档目的

本文档定义 AI Agent 管理平台的整体架构、模块划分、功能设计、数据模型、核心流程与接口规范，旨在实现以下目标：

- 统一产品、开发、测试团队对系统设计的理解；
- 指导后端、前端及数据模块的实际开发工作；
- 作为后续迭代、扩展与运维工作的基准依据。

### 1.2 系统定位

本平台为企业内部提供的 AI Agent 低代码研发与运营一体化平台，具备以下特征：

- 提供从知识库构建 → RAG 配置 → 流程编排 → Agent 发布 → 运行监控的全生命周期能力；
- 支持多业务线复用、权限隔离、灰度发布与可观测性；
- 通过配置化方式替代硬编码逻辑，提升敏捷性与可维护性。

### 1.3 核心边界

- 不自研大语言模型，仅负责主流模型厂商的统一接入与管理；
- 业务逻辑完全通过 Prompt Flow + RAG + 工具调用进行配置化实现；
- 对外提供标准化 OpenAPI 接口，供第三方业务系统集成调用。

---

## 二、整体架构设计

### 2.1 系统分层架构

平台采用四层架构模式，职责清晰、松耦合：

| 层级 | 组件 | 职责 |
|------|------|------|
| 接入层 | 管理控制台、OpenAPI、日志/监控看板 | 用户交互与外部系统对接 |
| 应用层 | 知识库管理、RAG 配置、Prompt Flow 编排、Agent 管理、权限中心 | 可视化操作与配置管理 |
| 核心服务层 | Agent 执行引擎、Flow 调度引擎、Prompt 模板引擎、RAG 检索引擎、文档处理引擎、记忆中心、工具调用中心 | 核心运行时逻辑 |
| 数据层 | PostgreSQL（元数据）、PGVector（向量）、MinIO（原始文件）、Redis（缓存）、ES（日志可选） | 数据存储与访问 |

### 2.2 模块依赖关系

各模块呈链式依赖关系，确保构建顺序合理：

```text
知识库管理 → RAG 配置 → Prompt Flow → Agent 管理 → 发布上线
```

- **知识库**：提供可信知识源；
- **RAG 配置**：封装知识检索能力；
- **Prompt Flow**：定义执行逻辑；
- **Agent**：整合所有资源，形成对外服务能力。

### 2.3 技术栈与核心组件选型

| 类别 | 技术选型 | 说明 |
|------|--------|------|
| 后端框架 | Spring Boot 3.x + Spring Security + SpringDoc OpenAPI | Java 企业级标准栈 |
| 向量数据库 | PostgreSQL + PGVector（HNSW 索引） | 统一关系与向量存储，降低运维复杂度 |
| 缓存与限流 | Redis | 支持热点缓存、会话保持、QPS 控制 |
| 对象存储 | MinIO | 自建 S3 兼容对象存储，保障文档安全性 |
| AI 开发框架 | LangChain4j + Spring AI | 支持多模型适配与流程抽象 |
| 文档解析 | Apache Tika / PdfBox | 支持 PDF、Word、Excel、Markdown 等格式 |
| 部署方式 | Docker + Kubernetes | 支持弹性伸缩与高可用部署 |
| 监控体系 | Prometheus + Grafana | 实现指标采集与可视化告警 |

### 2.4 部署架构与高可用设计

平台采用分布式微服务架构，结合容器化与云原生技术保障高可用性。关键设计如下：

- **多副本部署**：核心服务（如 Agent 执行引擎、RAG 检索引擎）以 Deployment 形式部署，副本数 ≥3，避免单点故障。
- **节点反亲和性（Pod Anti-Affinity）**：确保相同服务的多个副本分布在不同物理节点上，防止单机宕机导致服务中断。
- **健康检查机制**：
  - Liveness Probe：检测服务是否存活，异常时自动重启 Pod；
  - Readiness Probe：判断实例是否就绪，未就绪前不接入流量。
- **负载均衡**：
  - 服务内采用 Kubernetes Service 实现集群内负载均衡；
  - 外部请求通过 Ingress Controller 或 API 网关统一接入，支持动态路由与流量分发。
- **弹性扩缩容**：
  - 基于 HPA（Horizontal Pod Autoscaler），根据 CPU/GPU 利用率、内存使用率或自定义指标（如 QPS）实现自动扩缩容；
  - 结合 KEDA 支持基于业务指标（如推理请求数、队列长度）的精细化弹性调度。
- **多区域容灾（高级选项）**：
  - 在多可用区或跨地域部署独立集群；
  - 使用全局负载均衡器（Global Load Balancer）实现故障转移；
  - 模型与配置信息通过镜像同步或中心化存储保证一致性。
- **无中断发布**：支持蓝绿发布与金丝雀发布策略，结合 Istio 等服务网格实现灰度流量控制。

---

## 三、核心模块详细设计

### 3.1 知识库管理模块

#### 3.1.1 模块定位

统一管理企业内部文档、知识与规则，为 RAG 提供可信数据源。支持文档上传、解析、分块、向量化、版本控制、权限分配与检索测试。

#### 3.1.2 核心功能

1. **知识库管理**：创建、编辑、删除、权限分配；
2. **文档管理**：上传、解析、预览、版本管理、软删除；
3. **文本处理**：支持 PDF/Word/Excel/Markdown 解析与清洗；
4. **分块策略**：支持固定长度与语义分块，配置重叠窗口；
5. **向量化处理**：调用嵌入模型生成向量，写入 PGVector 并构建 HNSW 索引；
6. **检索测试**：支持在线输入问题，查看召回片段、相似度与耗时统计。

#### 3.1.3 核心表结构

```sql
-- 知识库
CREATE TABLE knowledge_base (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    creator VARCHAR(64) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0
);

-- 文档
CREATE TABLE kb_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    kb_id BIGINT NOT NULL,
    file_name VARCHAR(256) NOT NULL,
    file_type VARCHAR(32) NOT NULL,
    file_size BIGINT,
    file_path VARCHAR(512) NOT NULL,
    version INT DEFAULT 1,
    status TINYINT DEFAULT 0, -- 0:待处理, 1:处理中, 2:已完成, 3:失败
    error_msg VARCHAR(1024),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (kb_id) REFERENCES knowledge_base(id)
);

-- 文档分块与向量
CREATE TABLE kb_document_segment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    embedding vector, -- 使用 pgvector 扩展类型
    segment_no INT,
    metadata JSON,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (doc_id) REFERENCES kb_document(id)
);
```

> **说明**：`embedding` 字段使用 `pgvector` 扩展的 `vector` 类型，实际维度由嵌入模型决定（如 1536）。需在数据库初始化时启用 `vector` 扩展。

#### 3.1.4 核心流程

1. 用户上传文档；
2. 系统异步触发文档解析任务；
3. 使用 Apache Tika/PdfBox 提取文本；
4. 进行文本清洗（去除空白、特殊字符）；
5. 按照配置策略进行分块（支持固定长度或语义分割）；
6. 调用嵌入模型生成向量；
7. 写入 `kb_document_segment` 表；
8. 在 PGVector 上构建 HNSW 索引；
9. 更新文档状态为“已完成”。

> 异常处理：任一环节失败均记录 `error_msg`，状态置为“失败”，支持手动重试。

---

### 3.2 RAG 配置模块

#### 3.2.1 模块定位

将一个或多个知识库封装为可复用的检索能力，提供可视化配置界面，支持多种召回策略与后处理机制。

#### 3.2.2 核心功能

1. 支持绑定单个或多个知识库；
2. 检索方式：向量检索（HNSW）、关键词检索（基于 ES）、混合检索；
3. 参数配置：topK、ef_search、score_threshold；
4. 后处理：去重、重排序（rerank）、长度截断；
5. 结果缓存：基于 Redis 实现，支持自定义过期时间；
6. 在线调试：输入问题即可查看召回片段、相似度与响应耗时。

#### 3.2.3 核心表结构

```sql
CREATE TABLE rag_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    kb_ids JSON NOT NULL,                -- 关联的知识库 ID 列表
    top_k INT DEFAULT 10,
    ef_search INT DEFAULT 40,
    score_threshold DECIMAL(5,4),
    use_rerank TINYINT DEFAULT 0,        -- 是否启用重排序
    rerank_model VARCHAR(64),            -- 重排序模型名称
    cache_seconds INT DEFAULT 0,         -- 缓存时间（秒），0 表示不缓存
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

> **建议**：对 `kb_ids` 建立 GIN 索引以加速 JSON 查询。

#### 3.2.4 执行流程

1. 接收用户查询请求；
2. 调用嵌入模型生成查询向量；
3. 在 PGVector 中执行 HNSW 近似最近邻搜索；
4. （可选）结合 ES 执行关键词召回，合并结果；
5. 对召回结果进行去重与重排序；
6. 拼接 TopK 片段作为上下文输入；
7. 若启用缓存且命中，则直接返回；
8. 否则将结果写入 Redis 缓存；
9. 输出至 Prompt Flow 引擎。

---

### 3.3 Prompt Flow 模块

#### 3.3.1 模块定位

作为 Agent 的“大脑”，通过可视化编排定义复杂的执行逻辑，包括多轮对话、条件分支、循环、工具调用等，支持运营人员自主调整。

#### 3.3.2 核心概念

| 概念 | 说明 |
|------|------|
| Flow | 一个完整的执行流程，由多个节点组成 |
| Node 节点 | 包括开始、Prompt、RAG 检索、工具调用、条件分支、循环、赋值、结束等 |
| 变量 | 支持引用上下文变量（如 `{{user_query}}`, `{{rag_result}}`） |
| 版本管理 | 支持草稿、发布、回滚机制 |

#### 3.3.3 核心功能

1. 拖拽式可视化编排界面；
2. 支持变量引用语法 `{{variable}}`；
3. 条件判断：基于变量值动态路由；
4. 单步调试与日志追踪；
5. 发布、灰度发布、回滚机制；
6. 支持导入/导出 Flow 配置。

#### 3.3.4 核心表结构

```sql
CREATE TABLE prompt_flow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    flow_json JSON NOT NULL,             -- 节点、连线、配置的完整结构
    version INT DEFAULT 1,
    status TINYINT DEFAULT 0,            -- 0:草稿, 1:已发布, 2:禁用
    creator VARCHAR(64),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE prompt_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(32),                    -- system/user/assistant
    flow_id BIGINT,
    FOREIGN KEY (flow_id) REFERENCES prompt_flow(id)
);
```

---

### 3.4 Agent 管理模块

#### 3.4.1 模块定位

Agent 是平台最终交付的服务单元，整合 Prompt Flow、RAG 配置、模型、工具集、权限与限流策略，对外提供智能服务能力。

#### 3.4.2 核心功能

1. 创建、编辑、复制、删除 Agent；
2. 绑定 Flow、RAG 配置、模型、工具集；
3. 配置生成参数：temperature、top_p、max_tokens；
4. 设置限流策略：QPS、每日调用量；
5. 在线测试与日志追踪；
6. 支持 API 发布、灰度发布、版本回滚；
7. 提供调用量、成功率、响应耗时、错误率等监控指标。

#### 3.4.3 核心表结构

```sql
CREATE TABLE ai_agent (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    flow_id BIGINT NOT NULL,
    rag_config_id BIGINT,
    model_config JSON NOT NULL,         -- 模型类型、参数、key 管理
    tool_ids JSON,                      -- 工具 ID 列表
    status TINYINT DEFAULT 0,           -- 0:草稿, 1:发布, 2:禁用
    qps_limit INT DEFAULT 100,
    daily_quota INT DEFAULT 10000,      -- 每日调用限额
    temperature DECIMAL(3,2) DEFAULT 0.7,
    top_p DECIMAL(3,2) DEFAULT 0.9,
    max_tokens INT DEFAULT 512,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (flow_id) REFERENCES prompt_flow(id),
    FOREIGN KEY (rag_config_id) REFERENCES rag_config(id)
);

CREATE TABLE ai_agent_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL,
    user_id VARCHAR(64),
    input TEXT,
    output TEXT,
    cost_time INT,                      -- 响应耗时（ms）
    status TINYINT,                     -- 0:成功, 1:失败
    error_msg VARCHAR(1024),
    call_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (agent_id) REFERENCES ai_agent(id)
);
```

#### 3.4.4 执行流程

1. 外部系统发起 `/api/agent/invoke` 请求；
2. 系统进行身份鉴权与限流检查；
3. 加载对应 Agent 配置；
4. 启动 Prompt Flow 执行引擎；
5. 按流程调用 RAG 检索、工具执行、模型生成；
6. 返回最终输出结果；
7. 记录调用日志至 `ai_agent_call_log`。

---

### 3.5 公共支撑模块

#### 3.5.1 模型管理

- 支持接入 OpenAI、文心、通义千问、豆包等主流模型；
- 统一封装 API 接口，屏蔽厂商差异；
- 支持 Key 管理、负载均衡、熔断降级、重试机制；
- 提供模型调用成功率、延迟监控。

#### 3.5.2 工具管理

- 支持 HTTP 工具、SQL 查询工具、自定义函数插件；
- 插件化注册机制，支持热加载；
- 提供工具测试沙箱环境；
- 执行过程记录日志，支持追溯。

#### 3.5.3 权限系统

采用 RBAC 模型，支持多租户与多业务线隔离：

| 角色 | 权限说明 |
|------|----------|
| 1. 管理员 | 全部模块的增删改查、权限分配、系统配置 |
| 2. 开发者 | 可创建/修改 Flow、Agent、RAG，不可发布生产 |
| 3. 运营人员 | 可配置知识库、RAG、测试 Agent，无代码修改权限 |
| 4. 只读用户 | 仅可查看配置与监控数据 |

> **建议**：权限粒度细化至“按业务线”或“按知识库”级别。

#### 3.5.4 监控与告警

- 指标采集：调用量、成功率、P95/P99 耗时、错误类型分布；
- 日志检索：基于 ES 实现调用链追踪；
- 告警机制：异常陡增、超时率上升、模型调用失败时触发通知；
- 可视化大盘：Grafana 展示核心 KPI。

---

## 四、非功能设计

### 4.1 性能

| 指标 | 目标 | 说明 |
|------|------|------|
| 向量检索延迟 | < 100ms | 在 100w+ 向量规模下，HNSW 索引性能保证 |
| 端到端响应时间 | < 3s | 包含 RAG、工具调用、模型生成全过程 |
| 系统吞吐量 | ≥ 100 QPS | 支持水平扩展应对高峰流量 |

> **备注**：性能指标基于标准硬件环境（如配备 NVIDIA A100 GPU 的服务器集群，结合 Kubernetes 容器化部署与负载均衡）

### 4.2 稳定性

- 模型调用支持熔断、降级、指数退避重试；
- 文档处理任务异步化，失败自动重试（最多 3 次）；
- 全链路日志追踪，支持基于 trace_id 的问题定位；
- 支持灰度发布与快速回滚机制。

### 4.3 扩展性

- **模型扩展**：通过适配器模式快速接入新厂商；
- **工具扩展**：插件化注册，支持自定义脚本；
- **Flow 节点扩展**：开放 SDK 支持自定义节点开发；
- **知识库扩展**：支持新增文档类型与分块算法。

### 4.4 安全性

| 措施 | 说明 |
|------|------|
| 鉴权机制 | API 调用采用 Token 鉴权（JWT），管理后台使用 OAuth2 或 Session 认证 |
| 敏感信息处理 | 输出内容自动脱敏（如手机号、身份证） |
| 审计日志 | 所有关键操作记录操作人、时间、IP 地址 |
| 权限隔离 | 知识库、Agent 支持按组织/业务线隔离访问 |

---

## 五、实施 roadmap

| 版本 | 目标 | 主要功能 |
|------|------|----------|
| V1.0 基础版 | 快速验证 MVP | 知识库管理、基础 RAG、线性 Flow、Agent 创建与 API 发布、基础日志 |
| V2.0 企业版 | 满足生产需求 | 完整 Prompt Flow（分支/循环）、混合检索、权限隔离、灰度发布、监控大盘 |
| V3.0 高级版 | 智能进化能力 | Agent 自省机制、多 Agent 协同、自动 RAG 调优、插件市场 |

---

## 六、接口设计概要

| 模块 | 接口路径 | 方法 | 功能 |
|------|--------|------|------|
| 1. 知识库 | `/api/kb` | GET/POST | 获取/创建知识库 |
| 2. | `/api/kb/document` | GET/POST | 文档列表与上传 |
| 3. | `/api/kb/document/import` | POST | 批量导入文档 |
| 4. | `/api/kb/document/vectorize` | POST | 触发向量化任务 |
| 5. RAG | `/api/rag/config` | GET/POST | RAG 配置管理 |
| 6. | `/api/rag/retrieve` | POST | 执行检索测试 |
| 7. Flow | `/api/flow` | GET/POST | Flow 增删改查 |
| 8. | `/api/flow/debug` | POST | 单步调试 |
| 9. | `/api/flow/publish` | POST | 发布 Flow |
| 10. Agent | `/api/agent` | GET/POST | Agent 管理 |
| 11. | `/api/agent/test` | POST | 在线对话测试 |
| 12. | `/api/agent/invoke` | POST | 对外调用入口 |
| 13. | `/api/agent/log` | GET | 调用日志查询 |

---

## 七、附录

### 7.1 词汇表

| 术语 | 说明 |
|------|------|
| Agent | 智能体，整合流程、知识、工具的可调用服务 |
| Prompt Flow | 可视化流程编排，控制 Agent 执行逻辑 |
| RAG | 检索增强生成，提升回答准确性与可溯源性 |
| PGVector | PostgreSQL 的向量扩展，支持向量相似度搜索 |
| HNSW | 分层可导航小世界图算法，用于高效近似最近邻检索 |
| Embedding | 将文本转换为向量表示的过程 |
| TopK | 召回最相似的 K 个知识片段 |

### 7.2 状态码定义表

| 模块 | 字段 | 值 | 含义 |
|------|------|-----|------|
| 知识库文档 | `status` | 0 | 待处理 |
| | | 1 | 处理中 |
| | | 2 | 已完成 |
| | | 3 | 失败 |
| Prompt Flow | `status` | 0 | 草稿 |
| | | 1 | 已发布 |
| | | 2 | 禁用 |
| Agent | `status` | 0 | 草稿 |
| | | 1 | 发布 |
| | | 2 | 禁用 |
| 调用日志 | `status` | 0 | 成功 |
| | | 1 | 失败 |

### 7.3 错误码与异常处理策略

- **400 Bad Request**：请求参数缺失或格式错误，返回具体字段校验信息；
- **401 Unauthorized**：Token 无效或过期，要求重新认证；
- **403 Forbidden**：用户无权限访问指定资源；
- **404 Not Found**：请求的资源不存在；
- **429 Too Many Requests**：超出调用频率或总量限制；
- **500 Internal Server Error**：服务端内部异常，记录详细堆栈并告警；
- **503 Service Unavailable**：依赖服务不可用，提示上游服务状态。

异常处理遵循统一日志规范，包含 trace_id、时间戳、模块名、错误详情，并通过 Prometheus + Alertmanager 触发分级告警。