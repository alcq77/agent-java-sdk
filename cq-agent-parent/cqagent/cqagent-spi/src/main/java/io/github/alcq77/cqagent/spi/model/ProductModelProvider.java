package io.github.alcq77.cqagent.spi.model;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

/**
 * Model provider SPI.
 *
 * <p>A provider converts endpoint configuration into LangChain4j model instances
 * so the runtime can stay independent from vendor protocols.</p>
 */
public interface ProductModelProvider {

    /**
     * Unique provider code, for example {@code openai_compat}.
     */
    String providerCode();

    /**
     * Optional provider capability descriptor.
     */
    default ProductProviderCapabilities capabilities() {
        return ProductProviderCapabilities.chatOnly();
    }

    /**
     * Creates a callable chat model from the selected endpoint and logical model.
     */
    ChatLanguageModel createChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel);

    /**
     * Creates a streaming chat model when supported by the provider.
     */
    default StreamingChatLanguageModel createStreamingChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        throw new UnsupportedOperationException("streaming model is not supported by provider: " + providerCode());
    }
}
