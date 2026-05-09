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

class OpenAiProductProviderTest {

    @Test
    void shouldExposeOpenAiProviderCodeAndBuildModels() {
        OpenAiProductProvider provider = new OpenAiProductProvider();
        ProductEndpointConfig endpoint = ProductEndpointConfig.builder()
            .id("openai-primary")
            .provider(ModelProviderCodes.OPENAI)
            .apiKey("test-key")
            .defaultModel("gpt-4o-mini")
            .readTimeout(Duration.ofSeconds(30))
            .headers(Map.of("OpenAI-Organization", "demo-org"))
            .build();

        ChatLanguageModel chatModel = provider.createChatLanguageModel(endpoint, "primary-llm");
        StreamingChatLanguageModel streamingModel = provider.createStreamingChatLanguageModel(endpoint, "primary-llm");

        assertEquals(ModelProviderCodes.OPENAI, provider.providerCode());
        assertNotNull(chatModel);
        assertNotNull(streamingModel);
    }
}
