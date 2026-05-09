package io.github.alcq77.cqagent.sdk.provider;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import io.github.alcq77.cqagent.model.api.spi.ModelProviderCodes;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DashScopeProductProviderTest {

    @Test
    void shouldExposeDashScopeProviderCodeAndBuildModels() {
        DashScopeProductProvider provider = new DashScopeProductProvider();
        ProductEndpointConfig endpoint = ProductEndpointConfig.builder()
            .id("qwen")
            .provider(ModelProviderCodes.DASHSCOPE)
            .apiKey("test-key")
            .defaultModel("qwen-plus")
            .headers(Map.of("temperature", "0.2", "enable-search", "false"))
            .build();

        ChatLanguageModel chatModel = provider.createChatLanguageModel(endpoint, "primary-llm");
        StreamingChatLanguageModel streamingModel = provider.createStreamingChatLanguageModel(endpoint, "primary-llm");

        assertEquals(ModelProviderCodes.DASHSCOPE, provider.providerCode());
        assertNotNull(chatModel);
        assertNotNull(streamingModel);
    }

    @Test
    void shouldRequireApiKey() {
        DashScopeProductProvider provider = new DashScopeProductProvider();
        ProductEndpointConfig endpoint = ProductEndpointConfig.builder()
            .id("qwen")
            .provider(ModelProviderCodes.DASHSCOPE)
            .defaultModel("qwen-plus")
            .build();

        assertThrows(IllegalArgumentException.class, () ->
            provider.createChatLanguageModel(endpoint, "primary-llm")
        );
    }
}
