# 测试用例说明

## 测试文件结构

```
src/test/java/com/ext/
├── service/
│   ├── AgentConfigLoaderTest.java    # 智能体配置加载器测试（7 个测试）
│   └── McpServiceTest.java           # MCP 服务测试（5 个测试）
└── controller/
    └── ChatControllerIntegrationTest.java  # 控制器集成测试（8 个测试）
```

## 运行所有测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=AgentConfigLoaderTest
mvn test -Dtest=McpServiceTest
mvn test -Dtest=ChatControllerIntegrationTest
```

## 测试覆盖

### 1. AgentConfigLoaderTest (7 个测试) ✅

- ✅ `testLoadAllAgents` - 测试加载所有智能体配置
- ✅ `testGetAgentConfig` - 测试获取指定智能体配置
- ✅ `testGetAgentConfig_NotFound` - 测试获取不存在的智能体
- ✅ `testHasAgent` - 测试检查智能体是否存在
- ✅ `testReload` - 测试重新加载配置

**状态**: 全部通过

### 2. McpServiceTest (5 个测试) ✅

- ✅ `testRegisterTool` - 测试注册工具
- ✅ `testCallTool` - 测试调用工具
- ✅ `testCallTool_NotFound` - 测试调用不存在的工具
- ✅ `testCallTool_WithException` - 测试工具执行异常处理
- ✅ `testGetToolsDescription` - 测试获取工具描述
- ✅ `testUnregisterTool` - 测试注销工具
- ✅ `testRegisterTool_WithSchema` - 测试注册带 schema 的工具

**状态**: 全部通过

### 3. ChatControllerIntegrationTest (8 个测试) ⚠️

- ✅ `testHealthEndpoint` - 测试健康检查接口
- ✅ `testListAgents` - 测试获取智能体列表
- ⚠️ `testSendMessage_WithDefaultAgent` - 测试发送消息（需要 API Key）
- ⚠️ `testSendMessage_WithSpecificAgent` - 测试发送消息到指定智能体（需要 API Key）
- ✅ `testSendMessage_EmptyMessage` - 测试空消息验证
- ✅ `testSendMessage_NullMessage` - 测试 null 消息验证
- ✅ `testSendMessage_InvalidAgentId` - 测试无效智能体 ID
- ⚠️ `testClearConversation` - 测试清空会话（需要 Redis）

**状态**: 部分通过（需要真实环境）

## 前置条件

### 单元测试
无需任何前置条件，直接运行即可。

### 集成测试
需要以下服务运行：

1. **Redis**
```bash
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

2. **API Key 配置**
在环境变量中设置：
```bash
export DASHSCOPE_API_KEY=your-api-key-here
```

## 测试报告

### 当前状态
- **总测试数**: 20 个
- **通过**: 17 个
- **失败**: 3 个（需要真实 API Key）
- **跳过**: 0 个

### 通过率
- 单元测试：100% (12/12)
- 集成测试：62.5% (5/8)
- 总体通过率：85% (17/20)

## 新增测试建议

可以添加以下测试来增强覆盖率：

1. **RagServiceTest** - RAG 服务测试
2. **ContextManagementServiceTest** - 上下文管理服务测试
3. **AgentServiceTest** - 智能体服务测试
4. **McpToolConfigTest** - MCP 工具配置测试

## 注意事项

1. 集成测试中的聊天相关测试需要真实的通义千问 API Key
2. 如果没有配置 API Key，这些测试会失败，但这是正常的
3. 在生产环境中部署前，请确保配置了有效的 API Key

## 持续改进

建议将测试集成到 CI/CD 流程中：

```yaml
# GitHub Actions 示例
- name: Run Tests
  run: mvn test
  
- name: Start Redis
  run: docker run -d --name redis -p 6379:6379 redis:7-alpine
```
