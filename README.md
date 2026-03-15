# Spring AI 通义千问简单智能体

这是一个基于 Spring AI 框架，集成阿里云通义千问大模型的简单对话智能体。使用 Redis 存储上下文，超过窗口长度的上下文会自动总结并存储到文件。

## 技术栈

- **Spring Boot 3.2.4** - 应用框架
- **Spring AI Alibaba** - 通义千问集成
- **DashScope (通义千问)** - 大语言模型
- **Redis** - 上下文缓存
- **Redisson** - Redis 高级客户端

## 功能特性

### 1. 通义千问大模型集成
- 简单对话
- 多轮对话上下文管理
- 系统提示词定制

### 2. Redis 上下文缓存
- 缓存最近 5 轮对话
- 自动过期管理
- 支持多会话

### 3. 文件存储持久化
- 超过窗口长度自动总结
- 总结内容存储到 JSON 文件
- 文件目录：`context/{conversationId}.json`

### 4. RESTful API
- 发送消息
- 清空会话
- 健康检查

## 环境准备

### 1. Redis

```bash
# 使用 Docker 运行 Redis
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

### 2. 获取 DashScope API Key

访问 [DashScope 官网](https://dashscope.aliyun.com/) 注册并获取 API Key

## 配置说明

编辑 `src/main/resources/application.yml` 或在环境变量中设置：

```yaml
# 千问 API Key
DASHSCOPE_API_KEY=your-api-key-here

# Redis 配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
```

## 运行项目

```bash
# 编译项目
mvn clean package

# 运行项目
mvn spring-boot:run

# 或者直接运行 Main 类
```

## API 接口

### 1. 发送消息

```bash
curl -X POST http://localhost:8080/api/chat/send \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "test-123",
    "message": "你好，请介绍一下你自己"
  }'
```

响应示例：
```json
{
  "success": true,
  "conversationId": "test-123",
  "response": "我是通义千问 AI 助手..."
}
```

### 2. 清空会话

```bash
curl -X DELETE http://localhost:8080/api/chat/conversation/test-123
```

### 3. 健康检查

```bash
curl http://localhost:8080/api/chat/health
```

## 项目结构

```
src/main/java/com/ext/
├── Main.java                          # 应用入口
├── config/
│   └── RedisConfig.java              # Redis 配置
├── controller/
│   └── ChatController.java           # REST API 控制器
├── model/
│   ├── ChatMessage.java              # 对话消息
│   └── ConversationContext.java      # 对话上下文
├── service/
│   ├── ChatService.java              # 聊天服务
│   ├── ConversationSummaryService.java  # 摘要服务
│   └── ContextManagementService.java    # 上下文管理服务
└── example/
    └── SimpleChatExample.java        # 使用示例
```

## 核心功能说明

### 1. 对话工作流程

1. 用户发送消息到 `/api/chat/send`
2. 从 Redis 获取历史对话（最多 5 轮）
3. 构建包含历史上下文的提示
4. 调用通义千问模型生成回答
5. 保存对话到 Redis
6. 如果超过窗口长度，总结后存储到 `context/{conversationId}.json`

### 2. 上下文管理策略

- **Redis 存储**: Key 格式为 `conversation:{conversationId}`
- **窗口长度**: 默认保留最近 5 轮对话（10 条消息）
- **文件持久化**: 超过窗口长度时，完整对话历史存储到 JSON 文件
- **自动清理**: Redis 中只保留最近的消息，历史记录在文件中

### 3. 文件存储格式

文件路径：`context/{conversationId}.json`

文件内容包含完整的对话历史、时间戳、消息数量等信息。

## 自定义配置

### 修改对话配置

```yaml
conversation:
  max-history-rounds: 5       # 最大缓存轮数
  compression-model: qwen-turbo  # 摘要模型

# 上下文文件存储路径
context:
  storage:
    path: context             # 文件存储目录
```

## 开发指南

### 添加自定义系统提示

修改 `ChatService.java` 中的 `SYSTEM_PROMPT` 常量：

```java
private static final String SYSTEM_PROMPT = """
        你是一个专业的客服助手。请友好、专业地回答用户的问题。
        重点介绍产品的功能和优势。
        """;
```

### 扩展上下文管理

```java
@Service
public class CustomContextService {
    
    @Autowired
    private ContextManagementService contextService;
    
    public void loadHistory(String conversationId) {
        // 从文件加载历史对话
        ConversationContext history = contextService.loadContextFromFile(conversationId);
        // 恢复到 Redis
        if (history != null) {
            contextService.saveContext(history);
        }
    }
}
```

## 常见问题

### 1. Redis 连接失败

错误信息：`Cannot get Jedis connection`

解决：确保 Redis 服务已启动，检查配置中的 host 和 port

### 2. DashScope API 调用失败

错误信息：`API key is invalid`

解决：检查环境变量 DASHSCOPE_API_KEY 是否正确设置

### 3. 文件存储失败

错误信息：`IOException: No such file or directory`

解决：确保 `context` 目录存在且有写入权限，或修改配置中的 `context.storage.path`

## 许可证

MIT License

## 联系方式 524392198@qq.com

如有问题，请提交 Issue 或联系开发者。
