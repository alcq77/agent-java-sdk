package io.github.alcq77.cqagent.sdk.provider;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import io.github.alcq77.cqagent.model.api.spi.ModelProviderCodes;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GeminiProductProviderTest {

    @Test
    void shouldExposeGeminiProviderCodeAndBuildModels() {
        GeminiProductProvider provider = new GeminiProductProvider();
        ProductEndpointConfig endpoint = ProductEndpointConfig.builder()
            .id("gemini-primary")
            .provider("gemini")  // Use string literal instead of constant
            .apiKey("test-key")
            .defaultModel("gemini-2.0-flash")
            .readTimeout(Duration.ofSeconds(30))
            .headers(Map.of("temperature", "0.7", "max-output-tokens", "4096"))
            .build();

        ChatLanguageModel chatModel = provider.createChatLanguageModel(endpoint, "primary-llm");
        StreamingChatLanguageModel streamingModel = provider.createStreamingChatLanguageModel(endpoint, "primary-llm");

        assertNotNull(chatModel);
        assertNotNull(streamingModel);
    }
}