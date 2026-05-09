package io.github.alcq77.cqagent.sdk.provider;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import io.github.alcq77.cqagent.model.api.spi.ModelProviderCodes;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqagent.spi.model.ProductProviderCapabilities;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SubprocessModelBridgeProviderTest {

    @Test
    void shouldExposeProviderCodeAndCapabilities() {
        SubprocessModelBridgeProvider provider = new SubprocessModelBridgeProvider();
        assertEquals(ModelProviderCodes.PYTHON_BRIDGE, provider.providerCode());
        ProductProviderCapabilities caps = provider.capabilities();
        assertTrue(caps.chat());
        assertTrue(caps.streaming());
        assertFalse(caps.selfHosted());
    }

    @Test
    void shouldCreateModelsForExternalServer() {
        SubprocessModelBridgeProvider provider = new SubprocessModelBridgeProvider();
        ProductEndpointConfig endpoint = ProductEndpointConfig.builder()
            .id("vllm-remote")
            .provider(ModelProviderCodes.PYTHON_BRIDGE)
            .baseUrl("http://remote-server:8000/v1")
            .defaultModel("Qwen/Qwen2-7B")
            .apiKey("not-a-real-key")
            .readTimeout(Duration.ofSeconds(30))
            .build();

        ChatLanguageModel chatModel = provider.createChatLanguageModel(endpoint, "default-model");
        StreamingChatLanguageModel streamingModel = provider.createStreamingChatLanguageModel(endpoint, "default-model");

        assertNotNull(chatModel);
        assertNotNull(streamingModel);
    }

    @Test
    void shouldUseLogicalModelWhenDefaultModelNotSet() {
        SubprocessModelBridgeProvider provider = new SubprocessModelBridgeProvider();
        ProductEndpointConfig endpoint = ProductEndpointConfig.builder()
            .id("vllm-no-model")
            .provider(ModelProviderCodes.PYTHON_BRIDGE)
            .baseUrl("http://127.0.0.1:8000/v1")
            .apiKey("dummy")
            .readTimeout(Duration.ofSeconds(30))
            .build();

        ChatLanguageModel chatModel = provider.createChatLanguageModel(endpoint, "my-custom-model");
        assertNotNull(chatModel);
    }

    @Test
    void shouldRequireBaseUrl() {
        SubprocessModelBridgeProvider provider = new SubprocessModelBridgeProvider();
        ProductEndpointConfig endpoint = ProductEndpointConfig.builder()
            .id("no-url")
            .provider(ModelProviderCodes.PYTHON_BRIDGE)
            .build();

        assertThrows(IllegalArgumentException.class, () ->
            provider.createChatLanguageModel(endpoint, "default-model"));
    }

    @Test
    void shouldStripTrailingSlashFromBaseUrl() {
        SubprocessModelBridgeProvider provider = new SubprocessModelBridgeProvider();
        ProductEndpointConfig endpoint = ProductEndpointConfig.builder()
            .id("trailing-slash")
            .provider(ModelProviderCodes.PYTHON_BRIDGE)
            .baseUrl("http://127.0.0.1:8000/v1/")
            .defaultModel("test-model")
            .apiKey("dummy")
            .readTimeout(Duration.ofSeconds(30))
            .build();

        ChatLanguageModel chatModel = provider.createChatLanguageModel(endpoint, "default-model");
        assertNotNull(chatModel);
    }

    @Test
    void shouldFilterBridgeHeadersFromUpstream() {
        SubprocessModelBridgeProvider provider = new SubprocessModelBridgeProvider();
        ProductEndpointConfig endpoint = ProductEndpointConfig.builder()
            .id("filter-headers")
            .provider(ModelProviderCodes.PYTHON_BRIDGE)
            .baseUrl("http://127.0.0.1:8000/v1")
            .defaultModel("test-model")
            .apiKey("dummy")
            .readTimeout(Duration.ofSeconds(30))
            .headers(Map.of(
                "bridge.port", "8000",
                "X-Custom-Header", "keep-this"
            ))
            .build();

        // bridge.port is not a bridge.command so no subprocess is spawned;
        // X-Custom-Header should pass through to the upstream
        ChatLanguageModel chatModel = provider.createChatLanguageModel(endpoint, "default-model");
        assertNotNull(chatModel);
    }

    @Test
    void closeShouldNotThrow() {
        SubprocessModelBridgeProvider provider = new SubprocessModelBridgeProvider();
        assertDoesNotThrow(provider::close);
    }

    @Test
    void pythonServerProcessFromHeaders() {
        PythonServerProcess proc = PythonServerProcess.fromHeaders(
            Map.of("bridge.command", "vllm", "bridge.port", "9000", "bridge.args", "--model test"),
            8000
        );
        assertEquals(9000, proc.getPort());
        assertFalse(proc.isAlive());
    }

    @Test
    void pythonServerProcessFromHeadersDefaults() {
        PythonServerProcess proc = PythonServerProcess.fromHeaders(Map.of(), 8080);
        assertEquals(8080, proc.getPort());
        assertFalse(proc.isAlive());
        assertEquals("http://127.0.0.1:8080/v1", proc.getBaseUrl());
    }
}
