package io.github.alcq77.cqagent.sdk.provider;

import dev.langchain4j.model.chat.ChatLanguageModel;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqagent.spi.model.ProductModelProvider;
import io.github.alcq77.cqagent.spi.model.ProductProviderCapabilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductProviderCapabilitiesTest {

    @Test
    void shouldDefaultCustomProviderToChatOnly() {
        ProductModelProvider provider = new ProductModelProvider() {
            @Override
            public String providerCode() {
                return "custom";
            }

            @Override
            public ChatLanguageModel createChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
                return null;
            }
        };

        ProductProviderCapabilities capabilities = provider.capabilities();

        assertTrue(capabilities.chat());
        assertFalse(capabilities.streaming());
        assertFalse(capabilities.toolCalling());
    }

    @Test
    void shouldExposeBuiltInProviderCapabilities() {
        assertTrue(new OpenAiProductProvider().capabilities().streaming());
        assertTrue(new OpenAiProductProvider().capabilities().toolCalling());
        assertTrue(new OpenAiCompatibleProductProvider().capabilities().structuredOutput());
        assertTrue(new AnthropicProductProvider().capabilities().toolCalling());
        assertTrue(new OllamaProductProvider().capabilities().selfHosted());
        assertTrue(new DashScopeProductProvider().capabilities().streaming());
    }
}
