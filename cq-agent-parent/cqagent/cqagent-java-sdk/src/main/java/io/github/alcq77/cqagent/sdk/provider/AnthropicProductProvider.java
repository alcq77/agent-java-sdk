package io.github.alcq77.cqagent.sdk.provider;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import io.github.alcq77.cqagent.model.api.spi.ModelProviderCodes;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqagent.spi.model.ProductModelProvider;
import io.github.alcq77.cqagent.spi.model.ProductProviderCapabilities;

import java.util.Locale;
import java.util.Map;

public class AnthropicProductProvider implements ProductModelProvider {

    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com/v1";
    private static final String DEFAULT_VERSION = "2023-06-01";
    private static final int DEFAULT_MAX_TOKENS = 1024;

    @Override
    public String providerCode() {
        return ModelProviderCodes.ANTHROPIC;
    }

    @Override
    public ProductProviderCapabilities capabilities() {
        return ProductProviderCapabilities.chatAndStreaming()
            .withToolCalling(true);
    }

    @Override
    public ChatLanguageModel createChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        AnthropicChatModel.AnthropicChatModelBuilder builder = AnthropicChatModel.builder()
            .baseUrl(resolveBaseUrl(endpoint.getBaseUrl()))
            .apiKey(requiredApiKey(endpoint))
            .modelName(resolveModelName(endpoint, logicalModel))
            .version(option(endpoint.getHeaders(), "anthropic-version", DEFAULT_VERSION))
            .maxTokens(intOption(endpoint.getHeaders(), "anthropic-max-tokens", DEFAULT_MAX_TOKENS))
            .timeout(endpoint.getReadTimeout());
        option(endpoint.getHeaders(), "anthropic-beta").ifPresent(builder::beta);
        return builder.build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        AnthropicStreamingChatModel.AnthropicStreamingChatModelBuilder builder = AnthropicStreamingChatModel.builder()
            .baseUrl(resolveBaseUrl(endpoint.getBaseUrl()))
            .apiKey(requiredApiKey(endpoint))
            .modelName(resolveModelName(endpoint, logicalModel))
            .version(option(endpoint.getHeaders(), "anthropic-version", DEFAULT_VERSION))
            .maxTokens(intOption(endpoint.getHeaders(), "anthropic-max-tokens", DEFAULT_MAX_TOKENS))
            .timeout(endpoint.getReadTimeout());
        option(endpoint.getHeaders(), "anthropic-beta").ifPresent(builder::beta);
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

    private static String requiredApiKey(ProductEndpointConfig endpoint) {
        if (endpoint.getApiKey() == null || endpoint.getApiKey().isBlank()) {
            throw new IllegalArgumentException("endpoint.apiKey must not be blank for anthropic provider");
        }
        return endpoint.getApiKey();
    }

    private static String option(Map<String, String> headers, String key, String fallback) {
        return option(headers, key).orElse(fallback);
    }

    private static java.util.Optional<String> option(Map<String, String> headers, String key) {
        if (headers == null || headers.isEmpty()) {
            return java.util.Optional.empty();
        }
        String value = headers.get(key);
        if (value == null) {
            value = headers.get(key.toLowerCase(Locale.ROOT));
        }
        if (value == null || value.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(value.trim());
    }

    private static int intOption(Map<String, String> headers, String key, int fallback) {
        return option(headers, key)
            .map(Integer::parseInt)
            .orElse(fallback);
    }
}
