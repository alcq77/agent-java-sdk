# 统一智能体 Jar 产品 - 任务拆解与阶段划分

本文档将能力拆为可交付阶段，便于排期与验收；细节状态以 `docs/product/core-capabilities.md` 与 `docs/product/current-roadmap.md` 为准。

## 阶段 P0：可嵌入运行时闭环

| 任务 | 目标 | 验收要点 |
| --- | --- | --- |
| LangChain4j 主链 | 同步对话 + 工具多轮 | 集成测试覆盖工具校验与执行失败分类 |
| `ProductModelProvider` | 至少一种主流兼容路径可用 | `openai_compat` 可打通 |
| `ProductSessionStore` | 内存实现默认可用 | 会话可读写、可裁剪 |
| `AgentClient` / `AgentClientBuilder` | 纯 Java 可构建可调用 | `examples/java-sdk-demo` 可编译运行 |
| `cqagent-spring-boot-starter` | 配置驱动装配 | `examples/spring-starter-demo` 可启动 |
| 统一错误模型 | `AgentErrorCode` + Starter 映射 | 典型失败有稳定错误码与用户可读信息 |

## 阶段 P1：体验增强（与核心能力基线一致）

| 任务 | 目标 | 验收要点 |
| --- | --- | --- |
| Advisor 链 | 可插拔 `before/after/onError` | 顺序与文档一致，业务无侵入 |
| RAG 基础 | 导入、切分、检索、Advisor 注入 | 本地知识库问答示例可跑 |
| 路由增强 | taskType/tags、策略模板 | 同逻辑模型可按策略分流 |
| RAG 热更新 | 定时增量刷新 + 健康统计 | `ragIndex` 与健康策略可配置 |
| Provider 矩阵扩展 | 多厂商开箱或实验实现 | 文档列出 providerCode 与能力位 |

## 阶段 P2：工程化与「生态级 Starter」体验

| 任务 | 目标 | 验收要点 |
| --- | --- | --- |
| 文档单一主线 | 10 分钟 → Starter/SDK → 配置参考 | 链接可用、示例可复制 |
| 版本治理 | JDK / Boot / LangChain4j 范围声明 | `release-governance.md` + CI |
| 依赖与安全 | CVE  profile、BOM 对齐 | 发布前可执行检查任务 |
| 测试门禁 | 核心模块单测 + 关键集成 | CI 不仅编译，逐步加测试任务 |
| 可观测进阶 | 指标导出 / 链路追踪（可选） | 与路线图 Not Started 对齐后再立项 |

## 阶段 P3：路线图中的扩展项（非承诺排期）

以下内容来自 `current-roadmap.md`，**单独立项时再拆验收**：

- 更多 Provider（Gemini / DeepSeek / Azure OpenAI 等）；Bedrock 深化（Converse Stream 等）。
- Python 模型桥（子进程 / gRPC）。
- 多智能体编排（规划器、子 Agent、工作流运行时）。
- 能力感知路由与策略自动选择。

## 跨阶段固定要求

- 配置与 SPI 变更必须同步更新：`config-reference.md`、`spi-extension.md`、`migration-guide.md`（若影响迁移）。
- 新能力标注所属基线模块（见 `core-capabilities.md` 末尾原则）。
