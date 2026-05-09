package io.github.alcq77.cqagent.sdk.provider;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import io.github.alcq77.cqagent.model.api.spi.ModelProviderCodes;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnthropicProductProviderTest {

    @Test
    void shouldExposeAnthropicProviderCodeAndBuildModels() {
        AnthropicProductProvider provider = new AnthropicProductProvider();
        ProductEndpointConfig endpoint = ProductEndpointConfig.builder()
            .id("anthropic-primary")
            .provider(ModelProviderCodes.ANTHROPIC)
            .apiKey("test-key")
            .defaultModel("claude-3-5-sonnet-20240620")
            .readTimeout(Duration.ofSeconds(30))
            .headers(Map.of("anthropic-max-tokens", "256"))
            .build();

        ChatLanguageModel chatModel = provider.createChatLanguageModel(endpoint, "primary-llm");
        StreamingChatLanguageModel streamingModel = provider.createStreamingChatLanguageModel(endpoint, "primary-llm");

        assertEquals(ModelProviderCodes.ANTHROPIC, provider.providerCode());
        assertNotNull(chatModel);
        assertNotNull(streamingModel);
    }

    @Test
    void shouldRequireApiKey() {
        AnthropicProductProvider provider = new AnthropicProductProvider();
        ProductEndpointConfig endpoint = ProductEndpointConfig.builder()
            .id("anthropic-primary")
            .provider(ModelProviderCodes.ANTHROPIC)
            .defaultModel("claude-3-5-sonnet-20240620")
            .build();

        assertThrows(IllegalArgumentException.class, () ->
            provider.createChatLanguageModel(endpoint, "primary-llm")
        );
    }
}
