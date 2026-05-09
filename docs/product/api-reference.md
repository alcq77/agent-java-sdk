# API 鍙傝€冿紙LangChain4j 鍩哄骇锛?
## AgentClient

- `AgentChatResponse chat(AgentChatRequest request)`
- `void stream(AgentChatRequest request, AgentStreamingListener listener)`

璇锋眰 DTO锛歚dto.io.github.alcq77.cqagent.agent.api.AgentChatRequest`

- `traceId`锛堝彲閫夛紝涓嶄紶鍒欒嚜鍔ㄧ敓鎴愬苟鍥炲啓锛?- `promptTemplateId`锛堝彲閫夛級
- `promptVariables`锛堢敤浜?`{{key}}` 妯℃澘娓叉煋锛?- `taskType`锛堝彲閫夛紝鐢ㄤ簬浠诲姟绫诲瀷璺敱锛?- `tags`锛堝彲閫夛紝鐢ㄤ簬鏍囩璺敱锛?
鍝嶅簲 DTO锛歚dto.io.github.alcq77.cqagent.agent.api.AgentChatResponse`

- 杩斿洖 `traceId` / `sessionId` / token 璁℃暟

## AgentClientBuilder

- `create()`
- `logicalModel(String logicalModel)`
- `maxHistoryMessages(int maxHistoryMessages)`
- `maxToolCallIterations(int maxIterations)`
- `promptTemplate(String templateId, String systemPrompt, String userMessage)`
- `defaultPromptTemplate(String templateId)`
- `fallbackToDefaultPromptTemplate(boolean enabled)`
- `endpoint(ProductEndpointConfig endpoint)`
- `route(String logicalModel, String endpointId)`
- `routePolicy(String logicalModel, RoutePolicy policy)`
- `circuitBreaker(boolean enabled, int failureThreshold, int openSeconds)`
- `invokeTimeoutMillis(long timeoutMs)`
- `retry(int maxRetries, long backoffMs)`
- `advisor(AgentRuntimeAdvisor advisor)`
- `modelDispatchPolicy(String policyId, ModelDispatchPolicy policy)`
- `modelProvider(ProductModelProvider provider)`
- `tool(ProductTool tool)`
- `sessionStore(ProductSessionStore store)`
- `build()`

## Tool Calling 鍗忚锛堝綋鍓嶇増鏈級

褰撳墠涓嶅啀渚濊禆鏂囨湰鍗忚锛堝 `tool_call:...`锛夛紱宸ュ叿璋冪敤鐢?LangChain4j 鍘熺敓 `ToolSpecification + ToolExecutionRequest` 椹卞姩銆?
- `ProductTool#parameterSpecs()` 鐢ㄤ簬澹版槑 JSON schema锛坰tring/integer/number/boolean锛?- `ProductTool#execute(Map<String, Object>)` 鎵ц缁撴瀯鍖栧弬鏁?- 杩愯鏃惰嚜鍔ㄥ仛鍙傛暟鏍￠獙涓庡繀濉牎楠岋紝骞跺湪宸ュ叿鎵ц缁撴灉鍚庣户缁杞帹鐞?
宸ュ叿寮傚父鍒嗙被锛?
- `cqagent.tool.validation: ...` -> `tool_validation`
- `cqagent.tool.execution: ...` -> `tool_execution`

## 鏍囧噯閿欒妯″瀷

缁熶竴閿欒瀵硅薄浣嶄簬 `agent-api`锛?
- `AgentRuntimeException`
- `AgentError`
- `AgentErrorCode`

Starter 榛樿閫氳繃 `ProductAgentExceptionHandler` 鏄犲皠涓虹粺涓€ HTTP 鍝嶅簲銆?

