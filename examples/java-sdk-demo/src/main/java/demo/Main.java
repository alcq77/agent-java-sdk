package demo;

import io.github.alcq77.cqagent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqagent.sdk.AgentClient;
import io.github.alcq77.cqagent.sdk.AgentClientBuilder;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;

public class Main {

    /**
     * 纯 Java SDK 示例：
     * 1) 配置 endpoint/provider；
     * 2) 配置 logicalModel 到 endpoint 的路由；
     * 3) 发起一次聊天请求。
     */
    public static void main(String[] args) {
        // AgentClient 是业务侧唯一需要依赖的入口。
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
        // 支持命令行覆盖问题文本，便于快速 smoke test。
        String q = args.length > 0 ? args[0] : "你好，请介绍一下你";
        System.out.println(client.chat(AgentChatRequest.builder().message(q).build()).getReply());
    }
}
