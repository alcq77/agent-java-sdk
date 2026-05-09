package io.github.alcq77.cqagent.core.model;

import io.github.alcq77.cqagent.spi.model.ProductProviderCapabilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CapabilityRequirementTest {

    @Test
    void noneShouldSatisfyChatOnlyProvider() {
        ProductProviderCapabilities chatOnly = ProductProviderCapabilities.chatOnly();
        assertTrue(CapabilityRequirement.NONE.satisfiedBy(chatOnly));
    }

    @Test
    void streamingShouldRejectChatOnlyProvider() {
        ProductProviderCapabilities chatOnly = ProductProviderCapabilities.chatOnly();
        assertFalse(CapabilityRequirement.STREAMING.satisfiedBy(chatOnly));
    }

    @Test
    void streamingShouldAcceptChatAndStreamingProvider() {
        ProductProviderCapabilities chatAndStream = ProductProviderCapabilities.chatAndStreaming();
        assertTrue(CapabilityRequirement.STREAMING.satisfiedBy(chatAndStream));
    }

    @Test
    void toolsShouldRejectChatAndStreamingWithoutTools() {
        ProductProviderCapabilities chatAndStream = ProductProviderCapabilities.chatAndStreaming();
        assertFalse(CapabilityRequirement.TOOLS.satisfiedBy(chatAndStream));
    }

    @Test
    void toolsShouldAcceptProviderWithToolCalling() {
        ProductProviderCapabilities caps = ProductProviderCapabilities.chatAndStreaming().withToolCalling(true);
        assertTrue(CapabilityRequirement.TOOLS.satisfiedBy(caps));
    }

    @Test
    void streamingWithToolsShouldRequireBoth() {
        ProductProviderCapabilities chatStreamTools = ProductProviderCapabilities.chatAndStreaming().withToolCalling(true);
        assertTrue(CapabilityRequirement.STREAMING_WITH_TOOLS.satisfiedBy(chatStreamTools));

        ProductProviderCapabilities chatStreamOnly = ProductProviderCapabilities.chatAndStreaming();
        assertFalse(CapabilityRequirement.STREAMING_WITH_TOOLS.satisfiedBy(chatStreamOnly));
    }

    @Test
    void nullCapabilitiesShouldReject() {
        assertFalse(CapabilityRequirement.NONE.satisfiedBy(null));
    }

    @Test
    void isDefaultShouldReturnTrueForNone() {
        assertTrue(CapabilityRequirement.NONE.isDefault());
        assertFalse(CapabilityRequirement.STREAMING.isDefault());
        assertFalse(CapabilityRequirement.TOOLS.isDefault());
    }

    @Test
    void customRequirementShouldWork() {
        CapabilityRequirement req = CapabilityRequirement.of(true, true, true, true, true);
        ProductProviderCapabilities fullCaps = ProductProviderCapabilities.chatAndStreaming()
            .withToolCalling(true)
            .withMultimodal(true)
            .withStructuredOutput(true);
        assertTrue(req.satisfiedBy(fullCaps));

        ProductProviderCapabilities noMultimodal = ProductProviderCapabilities.chatAndStreaming().withToolCalling(true);
        assertFalse(req.satisfiedBy(noMultimodal));
    }
}