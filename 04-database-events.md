# 统一智能体 Jar 产品 - 数据与事件说明

## 1. 当前形态

Jar 内嵌模式不强制依赖外部数据库或消息系统。

- 默认会话存储：内存实现（`InMemoryProductSessionStore`）
- 事件总线：无强制实现

## 2. 扩展建议

当业务需要持久化与异步处理时，可通过 SPI 扩展：

- 会话持久化：实现 `ProductSessionStore`（Redis / JDBC 等）
- 事件通知：在业务侧包装 `AgentClient` 调用并投递 MQ

## 3. 建议事件模型（可选）

- `AgentConversationStarted`
- `AgentConversationCompleted`
- `AgentToolExecuted`
- `ModelEndpointFallbackTriggered`

以上事件建议由接入方在应用层定义并治理。
