package io.github.alcq77.cqagent.sdk.provider;

import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import io.github.alcq77.cqagent.model.api.spi.ModelProviderCodes;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqagent.spi.model.ProductModelProvider;
import io.github.alcq77.cqagent.spi.model.ProductProviderCapabilities;

public class AzureOpenAiProductProvider implements ProductModelProvider {

    @Override
    public String providerCode() {
        return ModelProviderCodes.AZURE_OPENAI;
    }

    @Override
    public ProductProviderCapabilities capabilities() {
        return ProductProviderCapabilities.chatAndStreaming();
    }

    @Override
    public ChatLanguageModel createChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        return AzureOpenAiChatModel.builder()
            .endpoint(requiredEndpoint(endpoint))
            .apiKey(endpoint.getApiKey())
            .deploymentName(resolveModelName(endpoint, logicalModel))
            .build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        return AzureOpenAiStreamingChatModel.builder()
            .endpoint(requiredEndpoint(endpoint))
            .apiKey(endpoint.getApiKey())
            .deploymentName(resolveModelName(endpoint, logicalModel))
            .build();
    }

    private static String requiredEndpoint(ProductEndpointConfig endpoint) {
        if (endpoint.getBaseUrl() == null || endpoint.getBaseUrl().isBlank()) {
            throw new IllegalArgumentException("endpoint.baseUrl must not be blank for azure-openai provider");
        }
        return endpoint.getBaseUrl().trim();
    }

    private static String resolveModelName(ProductEndpointConfig endpoint, String logicalModel) {
        return endpoint.getDefaultModel() == null || endpoint.getDefaultModel().isBlank()
            ? logicalModel
            : endpoint.getDefaultModel();
    }
}