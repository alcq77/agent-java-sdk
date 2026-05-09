package io.github.alcq77.cqagent.sdk.example;

import io.github.alcq77.cqagent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqagent.sdk.AgentClient;
import io.github.alcq77.cqagent.sdk.AgentClientBuilder;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;

/**
 * 纯 SDK 最小接入示例。
 */
public final class SdkQuickStartExample {

    private SdkQuickStartExample() {
    }

    public static void main(String[] args) {
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
        System.out.println(client.chat(AgentChatRequest.builder().message("你好").build()).getReply());
    }
}
