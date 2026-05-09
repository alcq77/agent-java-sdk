package io.github.alcq77.cqagent.sdk.provider;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import io.github.alcq77.cqagent.model.api.spi.ModelProviderCodes;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqagent.spi.model.ProductModelProvider;
import io.github.alcq77.cqagent.spi.model.ProductProviderCapabilities;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OpenAI-compatible provider based on LangChain4j OpenAI models.
 *
 * <p>The provider code stays {@code openai_compat} for existing configurations,
 * while the model is created through LangChain4j's native OpenAI builders.</p>
 */
public class OpenAiCompatibleProductProvider implements ProductModelProvider {

    @Override
    public String providerCode() {
        return ModelProviderCodes.OPENAI_COMPAT;
    }

    @Override
    public ProductProviderCapabilities capabilities() {
        return ProductProviderCapabilities.chatAndStreaming()
            .withToolCalling(true)
            .withStructuredOutput(true);
    }

    @Override
    public ChatLanguageModel createChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        String modelName = endpoint.getDefaultModel() == null || endpoint.getDefaultModel().isBlank()
            ? logicalModel
            : endpoint.getDefaultModel();
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
            .baseUrl(normalizedBaseUrl(endpoint.getBaseUrl()))
            .modelName(modelName)
            .timeout(endpoint.getReadTimeout())
            .customHeaders(safeHeaders(endpoint.getHeaders()));
        if (endpoint.getApiKey() != null && !endpoint.getApiKey().isBlank()) {
            builder.apiKey(endpoint.getApiKey());
        }
        return builder.build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        String modelName = endpoint.getDefaultModel() == null || endpoint.getDefaultModel().isBlank()
            ? logicalModel
            : endpoint.getDefaultModel();
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
            .baseUrl(normalizedBaseUrl(endpoint.getBaseUrl()))
            .modelName(modelName)
            .timeout(endpoint.getReadTimeout())
            .customHeaders(safeHeaders(endpoint.getHeaders()));
        if (endpoint.getApiKey() != null && !endpoint.getApiKey().isBlank()) {
            builder.apiKey(endpoint.getApiKey());
        }
        return builder.build();
    }

    private static String normalizedBaseUrl(String rawBaseUrl) {
        if (rawBaseUrl == null || rawBaseUrl.isBlank()) {
            throw new IllegalArgumentException("endpoint.baseUrl must not be blank");
        }
        String value = rawBaseUrl.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static Map<String, String> safeHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        return new LinkedHashMap<>(headers);
    }
}
