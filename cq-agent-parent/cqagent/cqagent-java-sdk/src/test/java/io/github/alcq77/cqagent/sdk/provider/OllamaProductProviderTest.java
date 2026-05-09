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

class OllamaProductProviderTest {

    @Test
    void shouldExposeOllamaProviderCodeAndBuildModels() {
        OllamaProductProvider provider = new OllamaProductProvider();
        ProductEndpointConfig endpoint = ProductEndpointConfig.builder()
            .id("ollama-local")
            .provider(ModelProviderCodes.OLLAMA)
            .baseUrl("http://127.0.0.1:11434/")
            .defaultModel("llama3.2")
            .readTimeout(Duration.ofSeconds(30))
            .headers(Map.of("temperature", "0.2", "num-ctx", "4096"))
            .build();

        ChatLanguageModel chatModel = provider.createChatLanguageModel(endpoint, "primary-llm");
        StreamingChatLanguageModel streamingModel = provider.createStreamingChatLanguageModel(endpoint, "primary-llm");

        assertEquals(ModelProviderCodes.OLLAMA, provider.providerCode());
        assertNotNull(chatModel);
        assertNotNull(streamingModel);
    }
}
