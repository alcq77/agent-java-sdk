# AI Agent Java SDK (Preview)

[CI Build and Test](https://github.com/alcq77/agent-platform-java-sdk/actions/workflows/ci-compile.yml)

面向业务应用的 Java 嵌入式 Agent 能力：**LangChain4j 运行时 + SDK / Spring Boot Starter**，支持会话、路由、工具调用、可观测性与 RAG 等面向生产的组装方式。

> 文档与配置请以 UTF-8 打开；快速上手优先阅读 `[docs/product/quick-start-10min.md](docs/product/quick-start-10min.md)`。

## Why This Project

- 用统一 SPI（模型、会话存储、工具）把可变部分收口，减少业务侧重复胶水代码。
- 默认集成 LangChain4j，兼容多种模型接入形态（OpenAI 兼容、Anthropic、Ollama、通义、Gemini、Azure、Bedrock 等）。

## Features

- 多模型路由：`taskType` / `tags` 动态映射逻辑模型；路由策略（静态、主备、加权、健康探测）。
- 可观测：`EmbeddedAgentClient.runtimeMetrics()`、运行时计数器、`promptTemplates` 命中与回退统计；工具调用/失败分类。
- Agent 主链基于 LangChain4j 的 tool-calling；Advisor/Interceptor 链用于提示词与上下文增强（如 RAG 注入）。
- RAG：本地 `md/txt` 知识库、分块、索引清单、定时增量刷新；Starter 下可通过 `agent.product.rag.`* 配置；支持按 metadata/source 过滤检索。
- 会话：内存 / 文件 / Redis / JDBC 等 `ProductSessionStore` 实现；与 LangChain4j `ChatMemory` 适配。
- 工作区：可加载 `workspace` 下插件与 skills，供工具与提示词侧使用。

## Quick Start

- 10 分钟：`[docs/product/quick-start-10min.md](docs/product/quick-start-10min.md)`

### Spring Boot Starter（详细）

见 `[docs/product/quick-start-starter.md](docs/product/quick-start-starter.md)`

### Java SDK（详细）

见 `[docs/product/quick-start-sdk.md](docs/product/quick-start-sdk.md)`

## Examples

- `examples/java-sdk-demo`：纯 Java SDK 示例
- `examples/spring-starter-demo`：Spring Boot Starter 示例

## Project Layout

> 主要模块位于 `cq-agent-parent/cq-agent`：

- `cqagent-java-sdk`：`cq-agent-parent/cq-agent/cqagent-java-sdk`
- `cqagent-spring-boot-starter`：`cq-agent-parent/cq-agent/cqagent-spring-boot-starter`
- `cqagent-core-engine`（LangChain4j 运行时）：`cq-agent-parent/cq-agent/cqagent-core-engine`
- `cqagent-spi`：`cq-agent-parent/cq-agent/cqagent-spi`
- `cqagent-plugins`：`cq-agent-parent/cq-agent/cqagent-plugins`

## Build

Requirements:

- JDK 21+
- Maven 3.9+

```bash
cd cq-agent-parent
mvn -q clean compile -DskipTests
```

与 CI 一致的完整校验（含单测）：

```bash
cd cq-agent-parent
mvn -q clean test
```

## Code Conventions

- 优先保持 SPI 边界清晰：模型接入实现 `ProductModelProvider`，工具实现 `ProductTool`，存储实现 `ProductSessionStore`。
- 配置敏感信息（API Key、云凭证）勿写入可被日志打印的通用 headers；云平台推荐使用环境变量与实例角色等默认凭证链。

## Docs

设计与仓库根目录：

- [架构设计](01-architecture-design.md)
- [技术栈评估](02-tech-stack-evaluation.md)
- [任务拆解与阶段](03-task-breakdown.md)
- [数据与事件说明](04-database-events.md)
- [开发规范](05-development-standards.md)

产品文档：

- [配置参考](docs/product/config-reference.md)
- [10 分钟上手](docs/product/quick-start-10min.md)
- [能力与路线图要点](docs/product/core-capabilities.md)
- [Current Roadmap](docs/product/current-roadmap.md)
- [API 参考](docs/product/api-reference.md)
- [SPI 扩展](docs/product/spi-extension.md)
- [Workspace 布局](docs/product/workspace-layout.md)
- [迁移说明](docs/product/migration-guide.md)
- [会话存储迁移](docs/product/session-migration.md)
- [版本与发布治理](docs/product/release-governance.md)

## Roadmap

- 持续完善文档与示例；扩展更多模型提供商开箱实现。
- Redis / DB 会话存储在生产环境的运维与压测建议。
- 按场景拆分 Starter 可选模块（按需引入依赖）。

## Contributing

欢迎通过 Issue / PR 反馈问题或提交改进；提交前请在 `cq-agent-parent` 下执行 `mvn clean test`（或与 CI 一致的校验）。