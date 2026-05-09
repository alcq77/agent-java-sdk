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

public class DeepSeekProductProvider implements ProductModelProvider {

    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com/v1";

    @Override
    public String providerCode() {
        return ModelProviderCodes.DEEPSEEK;
    }

    @Override
    public ProductProviderCapabilities capabilities() {
        return ProductProviderCapabilities.chatAndStreaming()
            .withToolCalling(true);
    }

    @Override
    public ChatLanguageModel createChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        String modelName = resolveModelName(endpoint, logicalModel);
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
            .baseUrl(resolveBaseUrl(endpoint.getBaseUrl()))
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
        String modelName = resolveModelName(endpoint, logicalModel);
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
            .baseUrl(resolveBaseUrl(endpoint.getBaseUrl()))
            .modelName(modelName)
            .timeout(endpoint.getReadTimeout())
            .customHeaders(safeHeaders(endpoint.getHeaders()));
        if (endpoint.getApiKey() != null && !endpoint.getApiKey().isBlank()) {
            builder.apiKey(endpoint.getApiKey());
        }
        return builder.build();
    }

    private static String resolveModelName(ProductEndpointConfig endpoint, String logicalModel) {
        return endpoint.getDefaultModel() == null || endpoint.getDefaultModel().isBlank()
            ? logicalModel
            : endpoint.getDefaultModel();
    }

    private static String resolveBaseUrl(String rawBaseUrl) {
        if (rawBaseUrl == null || rawBaseUrl.isBlank()) {
            return DEFAULT_BASE_URL;
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