package io.github.alcq77.cqagent.sdk.provider;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenStreamingChatModel;
import io.github.alcq77.cqagent.model.api.spi.ModelProviderCodes;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqagent.spi.model.ProductModelProvider;
import io.github.alcq77.cqagent.spi.model.ProductProviderCapabilities;

import java.util.Map;

public class DashScopeProductProvider implements ProductModelProvider {

    @Override
    public String providerCode() {
        return ModelProviderCodes.DASHSCOPE;
    }

    @Override
    public ProductProviderCapabilities capabilities() {
        return ProductProviderCapabilities.chatAndStreaming();
    }

    @Override
    public ChatLanguageModel createChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        QwenChatModel.QwenChatModelBuilder builder = QwenChatModel.builder()
            .apiKey(requiredApiKey(endpoint))
            .modelName(resolveModelName(endpoint, logicalModel));
        option(endpoint.getBaseUrl()).ifPresent(builder::baseUrl);
        applyOptions(builder, endpoint.getHeaders());
        return builder.build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        QwenStreamingChatModel.QwenStreamingChatModelBuilder builder = QwenStreamingChatModel.builder()
            .apiKey(requiredApiKey(endpoint))
            .modelName(resolveModelName(endpoint, logicalModel));
        option(endpoint.getBaseUrl()).ifPresent(builder::baseUrl);
        applyOptions(builder, endpoint.getHeaders());
        return builder.build();
    }

    private static void applyOptions(QwenChatModel.QwenChatModelBuilder builder, Map<String, String> options) {
        doubleOption(options, "top-p").ifPresent(builder::topP);
        intOption(options, "top-k").ifPresent(builder::topK);
        intOption(options, "max-tokens").ifPresent(builder::maxTokens);
        intOption(options, "seed").ifPresent(builder::seed);
        floatOption(options, "temperature").ifPresent(builder::temperature);
        floatOption(options, "repetition-penalty").ifPresent(builder::repetitionPenalty);
        boolOption(options, "enable-search").ifPresent(builder::enableSearch);
    }

    private static void applyOptions(QwenStreamingChatModel.QwenStreamingChatModelBuilder builder, Map<String, String> options) {
        doubleOption(options, "top-p").ifPresent(builder::topP);
        intOption(options, "top-k").ifPresent(builder::topK);
        intOption(options, "max-tokens").ifPresent(builder::maxTokens);
        intOption(options, "seed").ifPresent(builder::seed);
        floatOption(options, "temperature").ifPresent(builder::temperature);
        floatOption(options, "repetition-penalty").ifPresent(builder::repetitionPenalty);
        boolOption(options, "enable-search").ifPresent(builder::enableSearch);
    }

    private static String resolveModelName(ProductEndpointConfig endpoint, String logicalModel) {
        return endpoint.getDefaultModel() == null || endpoint.getDefaultModel().isBlank()
            ? logicalModel
            : endpoint.getDefaultModel();
    }

    private static String requiredApiKey(ProductEndpointConfig endpoint) {
        if (endpoint.getApiKey() == null || endpoint.getApiKey().isBlank()) {
            throw new IllegalArgumentException("endpoint.apiKey must not be blank for dashscope provider");
        }
        return endpoint.getApiKey();
    }

    private static java.util.Optional<String> option(String value) {
        if (value == null || value.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(value.trim());
    }

    private static java.util.Optional<String> option(Map<String, String> options, String key) {
        if (options == null || options.isEmpty()) {
            return java.util.Optional.empty();
        }
        return option(options.get(key));
    }

    private static java.util.Optional<Integer> intOption(Map<String, String> options, String key) {
        return option(options, key).map(Integer::parseInt);
    }

    private static java.util.Optional<Double> doubleOption(Map<String, String> options, String key) {
        return option(options, key).map(Double::parseDouble);
    }

    private static java.util.Optional<Float> floatOption(Map<String, String> options, String key) {
        return option(options, key).map(Float::parseFloat);
    }

    private static java.util.Optional<Boolean> boolOption(Map<String, String> options, String key) {
        return option(options, key).map(Boolean::parseBoolean);
    }
}
