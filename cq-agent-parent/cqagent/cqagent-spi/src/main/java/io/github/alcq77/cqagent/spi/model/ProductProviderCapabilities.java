package io.github.alcq77.cqagent.spi.model;

/**
 * Describes optional capabilities exposed by a model provider.
 */
public record ProductProviderCapabilities(
    boolean chat,
    boolean streaming,
    boolean toolCalling,
    boolean multimodal,
    boolean structuredOutput,
    boolean selfHosted
) {

    public static ProductProviderCapabilities chatOnly() {
        return new ProductProviderCapabilities(true, false, false, false, false, false);
    }

    public static ProductProviderCapabilities chatAndStreaming() {
        return new ProductProviderCapabilities(true, true, false, false, false, false);
    }

    public ProductProviderCapabilities withToolCalling(boolean value) {
        return new ProductProviderCapabilities(chat, streaming, value, multimodal, structuredOutput, selfHosted);
    }

    public ProductProviderCapabilities withMultimodal(boolean value) {
        return new ProductProviderCapabilities(chat, streaming, toolCalling, value, structuredOutput, selfHosted);
    }

    public ProductProviderCapabilities withStructuredOutput(boolean value) {
        return new ProductProviderCapabilities(chat, streaming, toolCalling, multimodal, value, selfHosted);
    }

    public ProductProviderCapabilities withSelfHosted(boolean value) {
        return new ProductProviderCapabilities(chat, streaming, toolCalling, multimodal, structuredOutput, value);
    }
}
