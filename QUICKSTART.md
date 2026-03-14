# 通义千问 AI 智能体 - 快速开始指南

## 项目概述

这是一个基于 Spring AI 和通义千问模型的简单对话智能体，具备以下特点：

- ✅ 使用 Redis 存储对话上下文
- ✅ 自动管理对话窗口长度（默认 5 轮）
- ✅ 超过窗口长度后自动总结并存储到文件
- ✅ 文件存储路径：`context/{conversationId}.json`

## 快速启动

### 1. 启动 Redis

```bash
# 使用 Docker
docker run -d --name redis -p 6379:6379 redis:7-alpine

# 或使用 docker-compose
docker-compose up -d
```

### 2. 配置 API Key

设置环境变量：

```bash
export DASHSCOPE_API_KEY=your-api-key-here
```

或在 `src/main/resources/application.yml` 中配置。

### 3. 运行应用

```bash
# 编译项目
mvn clean package

# 运行项目
mvn spring-boot:run
```

应用启动后，控制台会显示：

```
=== 通义千问 AI 助手 ===
提示：请使用 API 进行测试，例如：

curl -X POST http://localhost:8080/api/chat/send \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"test-123","message":"你好，请介绍一下你自己"}'

服务已启动，请访问 http://localhost:8080/api/chat/health 检查状态
```

## API 使用示例

### 发送消息

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
  "response": "我是通义千问，阿里巴巴达摩院研发的超大规模语言模型..."
}
```

### 继续对话（多轮对话）

```bash
curl -X POST http://localhost:8080/api/chat/send \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "test-123",
    "message": "你能帮我写一首诗吗？"
  }'
```

### 清空会话

```bash
curl -X DELETE http://localhost:8080/api/chat/conversation/test-123
```

### 健康检查

```bash
curl http://localhost:8080/api/chat/health
```

响应：

```json
{
  "status": "UP",
  "service": "Qianwen AI Agent"
}
```

## 上下文管理说明

### 工作原理

1. **Redis 缓存**：每轮对话都会保存到 Redis，key 为 `conversation:{conversationId}`
2. **窗口管理**：默认保留最近 5 轮对话（10 条消息：5 条用户消息 + 5 条助手回复）
3. **文件持久化**：当对话超过 10 条时，会自动将完整历史总结后存储到 JSON 文件

### 文件存储

- **目录**：`context/`（相对于项目根目录）
- **文件名**：`{conversationId}.json`
- **内容**：包含完整的对话历史、时间戳、消息数量等

示例文件内容：

```json
{
  "id": "test-123",
  "messages": [
    {
      "role": "USER",
      "content": "你好",
      "timestamp": 1710403200000
    },
    {
      "role": "ASSISTANT",
      "content": "你好！有什么我可以帮助你的吗？",
      "timestamp": 1710403205000
    }
  ],
  "createdAt": 1710403200000,
  "updatedAt": 1710403205000,
  "messageCount": 2
}
```

### 查看存储的文件

```bash
# 查看某个会话的历史记录
cat context/test-123.json

# 查看所有会话文件
ls -la context/
```

## 配置说明

### 修改对话窗口大小

编辑 `src/main/resources/application.yml`：

```yaml
conversation:
  max-history-rounds: 10  # 改为 10 轮对话
```

### 修改文件存储路径

```yaml
context:
  storage:
    path: /data/conversations  # 改为其他目录
```

### 修改千问模型

```yaml
spring:
  ai:
    dashscope:
      chat:
        options:
          model: qwen-plus  # 或 qwen-turbo, qwen-max 等
```

## 测试工具推荐

### 1. cURL（命令行）

适合快速测试，如上所示。

### 2. Postman（图形界面）

创建 POST 请求：
- URL: `http://localhost:8080/api/chat/send`
- Headers: `Content-Type: application/json`
- Body (raw JSON):
```json
{
  "conversationId": "test-123",
  "message": "你好"
}
```

### 3. HTTPie（现代化 CLI 工具）

```bash
# 安装
brew install httpie  # macOS
pip install httpie   # pip

# 使用
http POST localhost:8080/api/chat/send \
  conversationId=test-123 \
  message="你好，请介绍一下你自己"
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
│   └── service/
│       ├── ChatService.java              # 聊天服务
│       ├── ConversationSummaryService.java  # 摘要服务
│       └── ContextManagementService.java    # 上下文管理
├── src/main/resources/
│   └── application.yml                   # 配置文件
├── context/                              # 对话历史存储目录
├── pom.xml                              # Maven 配置
└── docker-compose.yml                   # Docker 配置
```

## 核心代码流程

### 聊天流程

```
用户请求 → ChatController → ChatService → ContextManagementService
                                    ↓
                            从 Redis 获取历史对话
                                    ↓
                            构建 Prompt（系统提示 + 历史 + 当前问题）
                                    ↓
                            调用通义千问 API
                                    ↓
                            保存回复到 Redis
                                    ↓
                            检查是否超过窗口长度
                                    ↓
                            如超过，总结并存储到文件
```

## 故障排查

### 1. Redis 连接失败

**错误**: `Cannot get Jedis connection`

**解决**:
```bash
# 检查 Redis 是否运行
docker ps | grep redis

# 测试 Redis 连接
redis-cli ping  # 应返回 PONG

# 重启 Redis
docker restart redis
```

### 2. API Key 无效

**错误**: `API key is invalid`

**解决**:
```bash
# 检查环境变量
echo $DASHSCOPE_API_KEY

# 重新设置
export DASHSCOPE_API_KEY=sk-xxxxxxxxxxxxx
```

### 3. 文件存储失败

**错误**: `IOException: No such file or directory`

**解决**:
```bash
# 创建 context 目录
mkdir -p context

# 检查权限
chmod 755 context
```

## 下一步

- 📚 阅读完整 README.md 了解更多功能
- 🔧 自定义系统提示词（修改 `ChatService.SYSTEM_PROMPT`）
- 🚀 部署到生产环境
- 💡 开发更多智能体功能

## 支持

如有问题，请参考：
- Spring AI 文档：https://spring.io/projects/spring-ai
- 通义千问文档：https://help.aliyun.com/zh/dashscope/
- Redis 文档：https://redis.io/docs/
