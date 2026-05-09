# Spring Boot Starter 蹇€熸帴鍏?
## 1. 渚濊禆

```xml
<dependency>
  <groupId>io.github.alcq77.cqagent</groupId>
  <artifactId>cqagent-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## 2. 閰嶇疆

```yaml
agent:
  product:
    workspace: ./workspace
    logical-model: primary-llm
    endpoints:
      primary:
        provider: openai_compat
        base-url: http://127.0.0.1:11434
        default-model: llama3.2
    routing:
      primary-llm: primary
```

Anthropic endpoint example:

```yaml
agent:
  product:
    endpoints:
      claude:
        provider: anthropic
        api-key: ${ANTHROPIC_API_KEY}
        default-model: claude-3-5-sonnet-20240620
```

Ollama endpoint example:

```yaml
agent:
  product:
    endpoints:
      ollama:
        provider: ollama
        base-url: http://127.0.0.1:11434
        default-model: llama3.2
```

DashScope/Qwen endpoint example:

```yaml
agent:
  product:
    endpoints:
      qwen:
        provider: dashscope
        api-key: ${DASHSCOPE_API_KEY}
        default-model: qwen-plus
```

## 3. 浣跨敤

```java
@RestController
@RequiredArgsConstructor
public class DemoController {
    private final AgentClient agentClient;

    @GetMapping("/demo/chat")
    public String chat() {
        return agentClient.chat(AgentChatRequest.builder().message("浣犲ソ").build()).getReply();
    }
}
```

## 4. 璇存槑

- `AgentClient` 鐢?Starter 鑷姩瑁呴厤銆?- 鑻ヤ笟鍔℃柟鑷畾涔?`ProductModelProvider` / `ProductTool` Bean锛孲tarter 浼氳嚜鍔ㄦ帴鍏ャ€?- 鍚姩鏃朵細鎵ц閰嶇疆鑷锛氭湭閰嶇疆鍙矾鐢遍€昏緫妯″瀷浼氱洿鎺ュけ璐ュ苟缁欏嚭鍙閿欒銆?- workspace 鐩綍寤鸿鍙傝€?`[workspace-layout.md](workspace-layout.md)`銆?- 鍙€夊紑鍚?RAG 鑷姩澧炲己锛氶厤缃?`agent.product.rag.enabled=true` 骞跺噯澶?`agent.product.rag.knowledge-directory`銆?
