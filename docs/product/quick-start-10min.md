# 10 鍒嗛挓鎺ュ叆鎸囧崡锛堝閮ㄩ」鐩級

## 1. 娣诲姞渚濊禆

```xml
<dependency>
  <groupId>io.github.alcq77.cqagent</groupId>
  <artifactId>cqagent-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## 2. 鏈€灏忛厤缃?
```yaml
agent:
  product:
    logical-model: primary-llm
    endpoints:
      primary:
        provider: openai_compat
        base-url: http://127.0.0.1:11434
        default-model: llama3.2
    routing:
      primary-llm: primary
```

## 3. 鍙€夛細鍚敤 RAG

```yaml
agent:
  product:
    rag:
      enabled: true
      knowledge-directory: ./workspace/knowledge
      manifest-path: ./workspace/rag/index-manifest.json
      refresh-interval-seconds: 60
      health-strict: true
```

> 灏?`md/txt` 鏂囨。鏀惧叆 `knowledge-directory`锛屽惎鍔ㄥ悗浼氳嚜鍔ㄥ缓绔嬬储寮曞苟瀹氭椂澧為噺鍒锋柊銆?
## 4. 鏈€灏忚皟鐢?
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

## 5. 楠岃瘉椤?
- `GET /actuator/health` 鏌ョ湅 `runtimeMetrics` 涓?`ragIndex`
- `ragIndex.documentCount/chunkCount` 搴斿ぇ浜?0锛堝惎鐢?RAG 鏃讹級
- 鍏抽棴鐭ヨ瘑搴撶洰褰曞悗锛宍health-strict=true` 灏嗚Е鍙戝仴搴烽檷绾?
## 6. 甯歌闂

- **no route found**锛氱己灏?`routing` 鎴?endpoint 鏈厤缃?- **provider not found**锛歚provider` 鍚嶇О涓庢敞鍐?provider 涓嶄竴鑷?- **rag index refresh failed**锛氱煡璇嗗簱鐩綍涓嶅瓨鍦ㄣ€佹潈闄愪笉瓒虫垨鏂囨。缂栫爜寮傚父

