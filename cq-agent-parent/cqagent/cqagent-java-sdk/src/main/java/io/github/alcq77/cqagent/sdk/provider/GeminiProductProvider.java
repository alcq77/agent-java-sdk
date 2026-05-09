package io.github.alcq77.cqagent.sdk.provider;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import io.github.alcq77.cqagent.model.api.spi.ModelProviderCodes;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqagent.spi.model.ProductModelProvider;
import io.github.alcq77.cqagent.spi.model.ProductProviderCapabilities;

public class GeminiProductProvider implements ProductModelProvider {

    @Override
    public String providerCode() {
        return ModelProviderCodes.GEMINI;
    }

    @Override
    public ProductProviderCapabilities capabilities() {
        return ProductProviderCapabilities.chatAndStreaming()
            .withToolCalling(true)
            .withMultimodal(true)
            .withStructuredOutput(true);
    }

    @Override
    public ChatLanguageModel createChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        return GoogleAiGeminiChatModel.builder()
            .apiKey(endpoint.getApiKey())
            .modelName(resolveModelName(endpoint, logicalModel))
            .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        return GoogleAiGeminiStreamingChatModel.builder()
            .apiKey(endpoint.getApiKey())
            .modelName(resolveModelName(endpoint, logicalModel))
            .build();
    }

    private static String resolveModelName(ProductEndpointConfig endpoint, String logicalModel) {
        return endpoint.getDefaultModel() == null || endpoint.getDefaultModel().isBlank()
            ? logicalModel
            : endpoint.getDefaultModel();
    }
}