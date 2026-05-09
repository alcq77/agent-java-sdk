package io.github.alcq77.cqagent.core.model;

import io.github.alcq77.cqagent.spi.model.ProductProviderCapabilities;

/**
 * Declares which capabilities a request requires from a model provider.
 *
 * <p>Used by {@link ProductModelRouter} to filter out endpoints whose provider
 * does not satisfy the required capabilities, avoiding wasted retries on
 * providers that can never fulfill the request.</p>
 *
 * <p>Example: a streaming request with tool calling would set
 * {@code streaming=true, toolCalling=true}, and the router will skip any
 * provider that lacks either capability.</p>
 */
public record CapabilityRequirement(
    boolean chat,
    boolean streaming,
    boolean toolCalling,
    boolean multimodal,
    boolean structuredOutput
) {

    /**
     * No special requirements — any provider with basic chat is acceptable.
     */
    public static final CapabilityRequirement NONE = new CapabilityRequirement(true, false, false, false, false);

    /**
     * Requires streaming support.
     */
    public static final CapabilityRequirement STREAMING = new CapabilityRequirement(true, true, false, false, false);

    /**
     * Requires streaming + tool calling.
     */
    public static final CapabilityRequirement STREAMING_WITH_TOOLS = new CapabilityRequirement(true, true, true, false, false);

    /**
     * Requires chat + tool calling (synchronous).
     */
    public static final CapabilityRequirement TOOLS = new CapabilityRequirement(true, false, true, false, false);

    /**
     * Requires multimodal input support.
     */
    public static final CapabilityRequirement MULTIMODAL = new CapabilityRequirement(true, false, false, true, false);

    /**
     * Returns true if the given provider capabilities satisfy this requirement.
     */
    public boolean satisfiedBy(ProductProviderCapabilities caps) {
        if (caps == null) {
            return false;
        }
        if (chat && !caps.chat()) return false;
        if (streaming && !caps.streaming()) return false;
        if (toolCalling && !caps.toolCalling()) return false;
        if (multimodal && !caps.multimodal()) return false;
        if (structuredOutput && !caps.structuredOutput()) return false;
        return true;
    }

    /**
     * Returns true if this requirement has any non-default flags set.
     */
    public boolean isDefault() {
        return chat && !streaming && !toolCalling && !multimodal && !structuredOutput;
    }

    public static CapabilityRequirement of(boolean chat, boolean streaming, boolean toolCalling) {
        return new CapabilityRequirement(chat, streaming, toolCalling, false, false);
    }

    public static CapabilityRequirement of(boolean chat, boolean streaming, boolean toolCalling,
                                           boolean multimodal, boolean structuredOutput) {
        return new CapabilityRequirement(chat, streaming, toolCalling, multimodal, structuredOutput);
    }
}