package io.github.alcq77.cqagent.testkit;

import io.github.alcq77.cqagent.sdk.AgentClient;
import io.github.alcq77.cqagent.sdk.AgentClientBuilder;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;

/**
 * 给业务方提供最小化接入模板（测试夹具）。
 */
public final class EmbeddedProductSmoke {

    private EmbeddedProductSmoke() {
    }

    public static AgentClient quickClient(String baseUrl, String apiKey, String model) {
        return AgentClientBuilder.create()
                .logicalModel("primary-llm")
                .endpoint(ProductEndpointConfig.builder()
                        .id("primary")
                        .provider("openai_compat")
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .defaultModel(model)
                        .build())
                .route("primary-llm", "primary")
                .build();
    }
}
