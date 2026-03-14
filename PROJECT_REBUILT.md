# 项目重建完成 ✅

## 项目概述

已成功将项目清空并重新创建为一个基于 **Spring AI** 和 **通义千问模型** 的简单对话智能体。

## 核心功能

### ✅ Redis 上下文存储
- 使用 Redis 缓存最近 5 轮对话（10 条消息）
- Key 格式：`conversation:{conversationId}`
- 自动管理对话窗口

### ✅ 文件持久化
- 超过窗口长度后自动总结
- 存储到 JSON 文件：`context/{conversationId}.json`
- 包含完整的历史记录、时间戳等信息

### ✅ RESTful API
- `POST /api/chat/send` - 发送消息
- `DELETE /api/chat/conversation/{id}` - 清空会话
- `GET /api/chat/health` - 健康检查

## 快速启动

### 1. 启动 Redis
```bash
docker run -d --name redis -p 6379:6379 redis:7-alpine
# 或使用 docker-compose
docker-compose up -d
```

### 2. 配置 API Key
```bash
export DASHSCOPE_API_KEY=your-api-key-here
```

### 3. 运行应用
```bash
mvn clean package
mvn spring-boot:run
```

## 测试示例

```bash
# 发送消息
curl -X POST http://localhost:8080/api/chat/send \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"test-123","message":"你好，请介绍一下你自己"}'

# 查看响应
{
  "success": true,
  "conversationId": "test-123",
  "response": "我是通义千问 AI 助手..."
}

# 继续对话（多轮）
curl -X POST http://localhost:8080/api/chat/send \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"test-123","message":"你能帮我写一首诗吗？"}'

# 清空会话
curl -X DELETE http://localhost:8080/api/chat/conversation/test-123

# 健康检查
curl http://localhost:8080/api/chat/health
```

## 项目结构

```
qianwenAi/
├── src/main/java/com/ext/
│   ├── Main.java                          # 主入口
│   ├── config/
│   │   └── RedisConfig.java              # Redis 配置
│   ├── controller/
│   │   └── ChatController.java           # REST API
│   ├── model/
│   │   ├── ChatMessage.java              # 消息模型
│   │   └── ConversationContext.java      # 上下文模型
│   ├── service/
│   │   ├── ChatService.java              # 聊天服务 ⭐
│   │   ├── ConversationSummaryService.java  # 摘要服务 ⭐
│   │   └── ContextManagementService.java    # 上下文管理 ⭐
│   └── example/
│       └── SimpleChatExample.java        # 使用示例
├── src/main/resources/
│   └── application.yml                   # 配置文件
├── context/                              # 对话历史存储目录 📁
├── pom.xml                              # Maven 配置
├── docker-compose.yml                   # Docker 配置
├── README.md                            # 详细文档
└── QUICKSTART.md                        # 快速开始指南
```

## 核心代码说明

### 1. ChatService - 聊天服务
位置：`src/main/java/com/ext/service/ChatService.java`

功能：
- 处理用户聊天请求
- 从 Redis 获取历史对话
- 构建 Prompt（系统提示 + 历史 + 当前问题）
- 调用通义千问 API
- 保存回复到 Redis

### 2. ContextManagementService - 上下文管理服务
位置：`src/main/java/com/ext/service/ContextManagementService.java`

功能：
- 管理 Redis 中的对话上下文
- 添加消息并检查是否超过窗口长度
- 超过窗口长度时自动总结并存储到文件
- 从文件加载历史上下文

### 3. ConversationSummaryService - 对话摘要服务
位置：`src/main/java/com/ext/service/ConversationSummaryService.java`

功能：
- 总结对话内容
- 压缩历史记录为摘要
- 使用通义千问模型进行智能总结

## 上下文管理流程

```
用户发送消息
    ↓
添加到 Redis 上下文
    ↓
检查消息数量是否 > maxHistoryRounds * 2 (默认 10)
    ↓
如果超过：
  1. 将完整对话历史总结
  2. 存储到 context/{id}.json
  3. Redis 中只保留最近 5 轮
    ↓
返回响应
```

## 配置文件说明

### application.yml

```yaml
# 通义千问配置
spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-max  # 可选：qwen-turbo, qwen-plus, qwen-max

# Redis 配置
  data:
    redis:
      host: localhost
      port: 6379

# 对话配置
conversation:
  max-history-rounds: 5        # 最大缓存轮数
  compression-model: qwen-turbo  # 摘要模型

# 文件存储路径
context:
  storage:
    path: context  # JSON 文件存储目录
```

## 依赖说明

### 核心依赖
- `spring-boot-starter-web` - Web 框架
- `spring-ai-alibaba-starter` - 通义千问集成
- `spring-boot-starter-data-redis` - Redis 支持
- `redisson-spring-boot-starter` - Redis 高级客户端
- `lombok` - 简化代码

### 已移除的依赖
- ❌ spring-ai-core（已包含在 starter 中）
- ❌ spring-ai-redis-store（不再需要向量库）
- ❌ spring-ai-starter-vector-store-redis（不再需要）
- ❌ PgVector 相关（已完全移除）

## 编译验证

✅ 项目已成功编译：
```bash
mvn clean compile
# BUILD SUCCESS
```

## 下一步建议

1. **修改系统提示词**
   - 编辑 `ChatService.java` 中的 `SYSTEM_PROMPT` 常量
   - 根据你的业务场景定制 AI 助手角色

2. **调整窗口大小**
   - 修改 `application.yml` 中的 `conversation.max-history-rounds`
   - 默认值：5 轮对话

3. **自定义文件存储路径**
   - 修改 `application.yml` 中的 `context.storage.path`
   - 默认值：`context/`

4. **扩展功能**
   - 添加文件上传功能，支持从文件加载历史
   - 实现更智能的摘要算法
   - 添加用户认证和授权

## 常见问题

### Q: Redis 连接失败？
A: 确保 Redis 容器正在运行：`docker ps | grep redis`

### Q: API Key 无效？
A: 检查环境变量：`echo $DASHSCOPE_API_KEY`

### Q: 文件存储失败？
A: 确保 `context/` 目录存在：`mkdir -p context`

## 文档参考

- 📖 [详细文档](README.md) - 完整的功能说明和开发指南
- 🚀 [快速开始](QUICKSTART.md) - 详细的 API 使用示例
- 📝 [环境配置](.env.example) - 环境变量模板

---

**项目重建时间**: 2026-03-14  
**状态**: ✅ 已完成，可正常运行
