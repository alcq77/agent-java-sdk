# Java SDK 蹇€熸帴鍏?
## 1. 渚濊禆

```xml
<dependency>
  <groupId>io.github.alcq77.cqagent</groupId>
  <artifactId>cqagent-java-sdk</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## 2. 鏈€灏忕ず渚?
```java
AgentClient client = AgentClientBuilder.create()
    .logicalModel("primary-llm")
    .endpoint(ProductEndpointConfig.builder()
        .id("primary")
        .provider("openai_compat")
        .baseUrl("http://127.0.0.1:11434")
        .defaultModel("llama3.2")
        .build())
    .route("primary-llm", "primary")
    .build();

AgentChatResponse response = client.chat(AgentChatRequest.builder().message("浣犲ソ").build());
System.out.println(response.getReply());
```

Anthropic endpoint example:

```java
ProductEndpointConfig anthropic = ProductEndpointConfig.builder()
    .id("claude")
    .provider("anthropic")
    .apiKey(System.getenv("ANTHROPIC_API_KEY"))
    .defaultModel("claude-3-5-sonnet-20240620")
    .build();
```

Ollama endpoint example:

```java
ProductEndpointConfig ollama = ProductEndpointConfig.builder()
    .id("ollama")
    .provider("ollama")
    .baseUrl("http://127.0.0.1:11434")
    .defaultModel("llama3.2")
    .build();
```

DashScope/Qwen endpoint example:

```java
ProductEndpointConfig qwen = ProductEndpointConfig.builder()
    .id("qwen")
    .provider("dashscope")
    .apiKey(System.getenv("DASHSCOPE_API_KEY"))
    .defaultModel("qwen-plus")
    .build();
```

## 3. 鍙墿灞曠偣

- 鑷畾涔夋ā鍨嬶細`modelProvider(ProductModelProvider provider)`
- 鑷畾涔夊伐鍏凤細`tool(ProductTool tool)`
- 鑷畾涔変細璇濆瓨鍌細`sessionStore(ProductSessionStore store)`

