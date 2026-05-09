# 统一智能体 Jar 产品 - 开发规范

## 1. 模块规范

- `cqagent-spi` 只放接口，不放业务实现
- `cqagent-core-engine` 不依赖 Spring
- `cqagent-java-sdk` 负责对外易用 API
- `cqagent-spring-boot-starter` 只做自动装配，不侵入核心逻辑

## 2. API 设计规范

- 对外 DTO 优先复用 `agent-api` / `model-api`
- Builder 方法命名语义清晰
- 错误信息可定位（包含 endpoint / provider 等关键信息）

## 3. 扩展规范

- 新模型协议：实现 `ProductModelProvider`
- 新工具：实现 `ProductTool`
- 新会话存储：实现 `ProductSessionStore`

禁止直接修改核心类实现业务特例，必须走扩展点。

## 4. 配置规范

- Starter 配置统一放在 `agent.product.*`
- 敏感配置使用环境变量注入
- 默认值应支持本地快速跑通

## 5. 质量规范

- 每次改动至少通过 `mvn clean compile -DskipTests`
- 文档与代码同步更新
- 发布前执行 CVE 检查（见 `release-governance.md`）
