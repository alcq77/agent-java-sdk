# 统一智能体 Jar 产品 - 技术栈评估与选型

## 1. 评估目标

在「可嵌入业务的 Java 智能体能力」前提下，选择 **运行时内核**、**集成方式** 与 **最低 JDK / 框架版本**，使后续演进专注在路由、会话、工具、RAG、可观测等产品能力上，而不是重复实现模型协议与工具循环。

## 2. 运行时内核：LangChain4j

### 2.1 入选理由

- **模型与工具协议**：Chat / Streaming、ToolSpecification、多轮工具调用等与主流模型能力对齐，减少自研协议与解析逻辑。
- **厂商适配成熟度**：社区已提供多类模型模块（OpenAI、Anthropic、Ollama、DashScope 等），本项目通过 `ProductModelProvider` 集中装配即可扩展。
- **嵌入式友好**：不绑定 Spring Cloud，可在纯 Java SDK 路径中使用。

### 2.2 代价与约束

- 版本升级需跟随 LangChain4j 语义（尤其是 `ChatLanguageModel` 工具相关重载）。
- 个别云厂商若暂无一流模块，需自建 Provider（例如基于 AWS SDK 的 Bedrock InvokeModel 适配）。

### 2.3 未优先选项（简述）

| 方案 | 说明 |
| --- | --- |
| **完全自研对话与 tool 循环** | 协议与边界情况维护成本高，与「产品化交付」目标不符。 |
| **Spring AI 作为唯一抽象** | 生态成熟，但本项目明确选择 **LangChain4j 为执行基座**；若未来要做「双栈」，应以单独适配层评估，而非替代当前主线。 |

结论：**以 LangChain4j 为单一对话与工具执行基座**，在其之上构建 cqagent 的路由、会话 SPI、Advisor、RAG 与 Starter。

## 3. 应用集成：Spring Boot 3.x

- **Starter**：`cqagent-spring-boot-starter` 提供 `agent.product.*` 与 Bean 自动发现，对标「生态级厂商 Starter」的体验目标。
- **版本**：与仓库父 POM 中的 **Spring Boot 3.3.x**、**Java 21** 对齐；具体区间见 `release-governance.md` 与 CI。

## 4. 契约与 API 分层

- **`agent-api`**：对外请求/响应与统一错误模型，便于宿主应用稳定依赖。
- **`model-api`**：模型侧编码常量等共享契约（如 `ModelProviderCodes`）。
- **`cqagent-spi`**：模型、工具、会话存储扩展点，避免业务直接改核心引擎代码。

## 5. 观测与韧性（内置策略）

- **熔断 / 重试 / 超时**：在 `EmbeddedAgentClient` 外壳层实现，与 LangChain4j 单次调用解耦。
- **指标**：运行时计数器 + Actuator 健康信息扩展；Prometheus 导出等可作为后续增强（见路线图）。

## 6. 选型结论（摘要）

- **运行时**：LangChain4j（必选）。
- **宿主集成**：Spring Boot Starter + 纯 Java SDK 双路径。
- **长期维护焦点**：SPI 扩展、Starter 配置契约、文档与版本矩阵，而非重写模型协议栈。

本文档随重大架构变更修订；若引入第二运行时或 Spring AI 门面层，应在本节追加「修订记录」并更新 `01-architecture-design.md`。
