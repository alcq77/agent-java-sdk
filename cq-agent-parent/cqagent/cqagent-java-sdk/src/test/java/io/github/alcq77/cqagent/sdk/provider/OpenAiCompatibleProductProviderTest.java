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

class OpenAiCompatibleProductProviderTest {

    @Test
    void shouldExposeOpenAiCompatibleProviderCodeAndBuildModels() {
        OpenAiCompatibleProductProvider provider = new OpenAiCompatibleProductProvider();
        ProductEndpointConfig endpoint = ProductEndpointConfig.builder()
            .id("compatible-primary")
            .provider(ModelProviderCodes.OPENAI_COMPAT)
            .baseUrl("https://example.com/v1/")
            .apiKey("test-key")
            .defaultModel("compatible-model")
            .readTimeout(Duration.ofSeconds(30))
            .headers(Map.of("X-Provider", "demo"))
            .build();

        ChatLanguageModel chatModel = provider.createChatLanguageModel(endpoint, "primary-llm");
        StreamingChatLanguageModel streamingModel = provider.createStreamingChatLanguageModel(endpoint, "primary-llm");

        assertEquals(ModelProviderCodes.OPENAI_COMPAT, provider.providerCode());
        assertNotNull(chatModel);
        assertNotNull(streamingModel);
    }
}
