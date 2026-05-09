# 閰嶇疆鍙傝€?
## Starter 閰嶇疆锛坄agent.product.*`锛?

| 閰嶇疆椤?                                                                        | 榛樿鍊?                   | 璇存槑                                                 |
| --------------------------------------------------------------------------- | ---------------------- | -------------------------------------------------- |
| `agent.product.workspace`                                                   | `./workspace`          | 宸ヤ綔鐩綍绾﹀畾                                             |
| `agent.product.plugin.enabled`                                              | `true`                 | 鏄惁鍚敤鐩綍鎻掍欢鎵弿                                         |
| `agent.product.plugin.directory`                                            | `./workspace/plugins`  | 鎻掍欢 Jar 鐩綍                                          |
| `agent.product.plugin.reload-interval-seconds`                              | `5`                    | 鎻掍欢閲嶈浇闂撮殧锛堢锛?                                         |
| `agent.product.skills.enabled`                                              | `true`                 | 鏄惁鍚敤鎶€鑳藉伐鍏?                                          |
| `agent.product.skills.directory`                                            | `./workspace/skills`   | 鎶€鑳界洰褰?                                              |
| `agent.product.session.store`                                               | `in_memory`            | 浼氳瘽瀛樺偍锛歚in_memory` / `filesystem` / `redis` / `jdbc` |
| `agent.product.session.filesystem-directory`                                | `./workspace/sessions` | 鏂囦欢浼氳瘽瀛樺偍鐩綍锛坰tore=filesystem锛?                        |
| `agent.product.session.redis-key-prefix`                                    | `cqagent:session:`     | Redis key 鍓嶇紑锛坰tore=redis锛?                         |
| `agent.product.session.redis-ttl`                                           | `7d`                   | Redis key TTL锛坰tore=redis锛?                        |
| `agent.product.session.jdbc-table`                                          | `cqagent_session_store` | JDBC 琛ㄥ悕锛坰tore=jdbc锛?                               |
| `agent.product.session.jdbc-auto-init`                                      | `true`                 | 鍚姩鏃惰嚜鍔ㄥ缓琛紙store=jdbc锛?                               |
| `agent.product.prompts.default-template-id`                                 | 绌?                     | 榛樿 Prompt 妯℃澘 ID                                    |
| `agent.product.prompts.fallback-to-default`                                 | `true`                 | 璇锋眰妯℃澘缂哄け鏃舵槸鍚﹀洖閫€榛樿妯℃澘                                    |
| `agent.product.prompts.templates.<id>.version`                              | 绌?                     | 妯℃澘鐗堟湰鏍囪瘑                                             |
| `agent.product.prompts.templates.<id>.system-prompt`                        | 绌?                     | 妯℃澘绯荤粺鎻愮ず                                             |
| `agent.product.prompts.templates.<id>.user-message`                         | 绌?                     | 妯℃澘鐢ㄦ埛娑堟伅锛堟敮鎸?`{{message}}`锛?                          |
| `logging.level.io.github.alcq77.cqagent.starter.WorkspaceSkillsTool` | `INFO`                 | 璁句负 `DEBUG` 鍙煡鐪嬫妧鑳藉懡涓棩蹇?                              |
| `agent.product.logical-model`                                               | `primary-llm`          | 涓氬姟璋冪敤閫昏緫妯″瀷鍚?                                         |
| `agent.product.max-history-messages`                                        | `40`                   | 浼氳瘽鍘嗗彶鏈€澶ф秷鎭暟                                          |
| `agent.product.invoke-timeout-ms`                                           | `60000`                | 鍗曟璋冪敤瓒呮椂鏃堕棿锛堟绉掞紝0 琛ㄧず涓嶉檺鍒讹級                               |
| `agent.product.max-retries`                                                 | `1`                    | 鍗曠鐐瑰け璐ュ悗鐨勬渶澶ч噸璇曟鏁?                                     |
| `agent.product.retry-backoff-ms`                                            | `300`                  | 閲嶈瘯閫€閬挎椂闂达紙姣锛?                                        |
| `agent.product.endpoints.<id>.provider`                                     | `openai_compat`        | 绔偣鍗忚閫傞厤鍣ㄧ紪鐮?                                         |
| `agent.product.endpoints.<id>.base-url`                                     | -                      | 妯″瀷鍦板潃                                               |
| `agent.product.endpoints.<id>.api-key`                                      | 绌?                     | API Key                                            |
| `agent.product.endpoints.<id>.default-model`                                | 绌?                     | 涓婃父鐪熷疄妯″瀷鍚?                                           |
| `agent.product.routing.<logicalModel>`                                      | -                      | 閫昏緫妯″瀷璺敱绔偣                                           |
| `agent.product.model-dispatch-policies.<id>.target-logical-model`           | 绌?                     | 鍛戒腑鍚庡垏鎹㈠埌鐨勯€昏緫妯″瀷                                      |
| `agent.product.model-dispatch-policies.<id>.task-types`                     | 绌?                     | 鍖归厤鐨勪换鍔＄被鍨嬪垪琛?                                         |
| `agent.product.model-dispatch-policies.<id>.required-tags`                  | 绌?                     | 蹇呴』鍛戒腑鐨勮姹傛爣绛惧垪琛?                                      |
| `agent.product.rag.enabled`                                                  | `false`                 | 鏄惁鍚敤 RAG Advisor                                        |
| `agent.product.rag.knowledge-directory`                                      | `./workspace/knowledge` | 鏈湴鐭ヨ瘑搴撶洰褰曪紙md/txt锛?                                    |
| `agent.product.rag.manifest-path`                                            | `./workspace/rag/index-manifest.json` | 绱㈠紩娓呭崟璺緞                                      |
| `agent.product.rag.chunk-size`                                               | `400`                   | 鏂囨。鍒囧垎鍧楀ぇ灏?                                              |
| `agent.product.rag.overlap`                                                  | `80`                    | 鍒囧垎閲嶅彔澶у皬                                                 |
| `agent.product.rag.top-k`                                                    | `4`                     | 妫€绱㈣繑鍥炵墖娈垫暟                                               |
| `agent.product.rag.advisor-order`                                            | `-100`                  | RAG Advisor 鎵ц椤哄簭                                         |
| `agent.product.rag.refresh-interval-seconds`                                 | `60`                    | 澧為噺绱㈠紩瀹氭椂鍒锋柊闂撮殧锛堢锛?=0 浠呭惎鍔ㄥ埛鏂颁竴娆★級                |
| `agent.product.rag.health-strict`                                            | `true`                  | 鍒锋柊澶辫触鏄惁灏嗗仴搴风姸鎬侀檷绾т负 `DOWN`                            |
| `agent.product.rag.allowed-sources`                                          | 绌?                     | source 鍖呭惈杩囨护锛堝鍊硷級                                      |
| `agent.product.rag.metadata-equals.<key>`                                    | 绌?                     | metadata 绛夊€艰繃婊?                                           |


> 鑻ラ渶瑕佹煡鐪嬩細璇濆瓨鍌ㄨ娴嬬鐐癸紝璇峰湪搴旂敤涓毚闇诧細
> `management.endpoints.web.exposure.include=health,cqagentSessionStore`

## SDK 閰嶇疆锛堜唬鐮佸紡锛?
- `AgentClientBuilder.logicalModel(...)`
- `AgentClientBuilder.maxHistoryMessages(...)`
- `AgentClientBuilder.maxToolCallIterations(...)`
- `AgentClientBuilder.circuitBreaker(enabled, failureThreshold, openSeconds)`
- `AgentClientBuilder.invokeTimeoutMillis(...)`
- `AgentClientBuilder.retry(maxRetries, backoffMs)`
- `AgentClientBuilder.endpoint(...)`
- `AgentClientBuilder.route(...)`
- `AgentClientBuilder.routePolicy(...)`

## 杩愯瑙傛祴琛ュ厖

- `AgentChatRequest.traceId`锛氬彲閫変紶鍏ワ紱涓嶄紶鏃惰繍琛屾椂鑷姩鐢熸垚骞跺洖鍐欏埌鍝嶅簲銆?- `AgentChatResponse.traceId`锛氱敤浜庢棩蹇椾笌鏁呴殰瀹氫綅涓茶仈銆?- `actuator/health` 涓柊澧?`runtimeMetrics`锛氬寘鍚?`totalRequests`銆乣totalFailures`銆乣endpointFailures`銆乣failureByType`銆乣circuitSkipped`銆?- `failureByType` 褰撳墠鍒嗙被锛歚timeout` / `upstream` / `route` / `provider` / `tool_validation` / `tool_execution`锛堝吋瀹逛繚鐣?`tool`锛夈€?- `runtimeMetrics.promptTemplates`锛氬寘鍚?`templateHits`銆乣templateFallbackCount`銆乣templateMissingCount`銆?- `runtimeMetrics` 杩樺寘鍚交閲忚鏁板櫒锛?  - `syncChatInvocations`
  - `streamingInvocations`
  - `toolInvocations`
  - `toolValidationFailures`
  - `toolExecutionFailures`

## Advisor 閾捐矾

- SDK锛氶€氳繃 `AgentClientBuilder.advisor(...)` 娉ㄥ唽澧炲己鍣?- Starter锛氬０鏄?`AgentRuntimeAdvisor` Bean 鑷姩鎺ュ叆
- 鎵ц椤哄簭锛歚before` 姝ｅ簭锛宍after/onError` 閫嗗簭


