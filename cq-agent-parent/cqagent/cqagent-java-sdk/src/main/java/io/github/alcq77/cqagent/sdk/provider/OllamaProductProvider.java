package io.github.alcq77.cqagent.sdk.provider;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import io.github.alcq77.cqagent.model.api.spi.ModelProviderCodes;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqagent.spi.model.ProductModelProvider;
import io.github.alcq77.cqagent.spi.model.ProductProviderCapabilities;

import java.util.LinkedHashMap;
import java.util.Map;

public class OllamaProductProvider implements ProductModelProvider {

    private static final String DEFAULT_BASE_URL = "http://127.0.0.1:11434";

    @Override
    public String providerCode() {
        return ModelProviderCodes.OLLAMA;
    }

    @Override
    public ProductProviderCapabilities capabilities() {
        return ProductProviderCapabilities.chatAndStreaming()
            .withStructuredOutput(true)
            .withSelfHosted(true);
    }

    @Override
    public ChatLanguageModel createChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        OllamaChatModel.OllamaChatModelBuilder builder = OllamaChatModel.builder()
            .baseUrl(resolveBaseUrl(endpoint.getBaseUrl()))
            .modelName(resolveModelName(endpoint, logicalModel))
            .timeout(endpoint.getReadTimeout())
            .customHeaders(safeHeaders(endpoint.getHeaders()));
        applyOptions(builder, endpoint.getHeaders());
        return builder.build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        OllamaStreamingChatModel.OllamaStreamingChatModelBuilder builder = OllamaStreamingChatModel.builder()
            .baseUrl(resolveBaseUrl(endpoint.getBaseUrl()))
            .modelName(resolveModelName(endpoint, logicalModel))
            .timeout(endpoint.getReadTimeout())
            .customHeaders(safeHeaders(endpoint.getHeaders()));
        applyOptions(builder, endpoint.getHeaders());
        return builder.build();
    }

    private static void applyOptions(OllamaChatModel.OllamaChatModelBuilder builder, Map<String, String> options) {
        doubleOption(options, "temperature").ifPresent(builder::temperature);
        doubleOption(options, "top-p").ifPresent(builder::topP);
        intOption(options, "top-k").ifPresent(builder::topK);
        intOption(options, "num-predict").ifPresent(builder::numPredict);
        intOption(options, "num-ctx").ifPresent(builder::numCtx);
        option(options, "format").ifPresent(builder::format);
    }

    private static void applyOptions(OllamaStreamingChatModel.OllamaStreamingChatModelBuilder builder, Map<String, String> options) {
        doubleOption(options, "temperature").ifPresent(builder::temperature);
        doubleOption(options, "top-p").ifPresent(builder::topP);
        intOption(options, "top-k").ifPresent(builder::topK);
        intOption(options, "num-predict").ifPresent(builder::numPredict);
        intOption(options, "num-ctx").ifPresent(builder::numCtx);
        option(options, "format").ifPresent(builder::format);
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

    private static java.util.Optional<String> option(Map<String, String> options, String key) {
        if (options == null || options.isEmpty()) {
            return java.util.Optional.empty();
        }
        String value = options.get(key);
        if (value == null || value.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(value.trim());
    }

    private static java.util.Optional<Integer> intOption(Map<String, String> options, String key) {
        return option(options, key).map(Integer::parseInt);
    }

    private static java.util.Optional<Double> doubleOption(Map<String, String> options, String key) {
        return option(options, key).map(Double::parseDouble);
    }
}
