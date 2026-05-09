package io.github.alcq77.cqagent.core.model;

import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqagent.spi.model.ProductModelProvider;
import io.github.alcq77.cqagent.spi.model.ProductProviderCapabilities;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProductModelRouterCapabilityTest {

    private static final ProductModelRouter ROUTER = new ProductModelRouter(ep -> true);

    @Test
    void shouldFilterOutProviderWithoutStreaming() {
        ProductEndpointConfig ep1 = endpoint("ep1", "chat-only-provider");
        ProductEndpointConfig ep2 = endpoint("ep2", "streaming-provider");

        ProductModelProvider chatOnly = provider("chat-only-provider", ProductProviderCapabilities.chatOnly());
        ProductModelProvider streaming = provider("streaming-provider", ProductProviderCapabilities.chatAndStreaming());

        Map<String, ProductEndpointConfig> endpoints = Map.of("ep1", ep1, "ep2", ep2);
        Map<String, ProductModelProvider> providers = Map.of("chat-only-provider", chatOnly, "streaming-provider", streaming);
        RoutePolicy routePolicy = new RoutePolicy();
        routePolicy.setWeightedEndpoints(new java.util.LinkedHashMap<>(Map.of("ep1", 1, "ep2", 1)));

        List<ProductEndpointConfig> candidates = ROUTER.resolveCandidates(
            "model", endpoints, Map.of(), Map.of("model", routePolicy),
            CapabilityRequirement.STREAMING, providers);

        assertEquals(1, candidates.size());
        assertEquals("ep2", candidates.get(0).getId());
    }

    @Test
    void shouldFilterOutProviderWithoutToolCalling() {
        ProductEndpointConfig ep1 = endpoint("ep1", "no-tools");
        ProductEndpointConfig ep2 = endpoint("ep2", "has-tools");

        ProductModelProvider noTools = provider("no-tools", ProductProviderCapabilities.chatAndStreaming());
        ProductModelProvider hasTools = provider("has-tools", ProductProviderCapabilities.chatAndStreaming().withToolCalling(true));

        Map<String, ProductEndpointConfig> endpoints = Map.of("ep1", ep1, "ep2", ep2);
        Map<String, ProductModelProvider> providers = Map.of("no-tools", noTools, "has-tools", hasTools);
        RoutePolicy routePolicy = new RoutePolicy();
        routePolicy.setWeightedEndpoints(new java.util.LinkedHashMap<>(Map.of("ep1", 1, "ep2", 1)));

        List<ProductEndpointConfig> candidates = ROUTER.resolveCandidates(
            "model", endpoints, Map.of(), Map.of("model", routePolicy),
            CapabilityRequirement.TOOLS, providers);

        assertEquals(1, candidates.size());
        assertEquals("ep2", candidates.get(0).getId());
    }

    @Test
    void shouldKeepAllWhenNoRequirements() {
        ProductEndpointConfig ep1 = endpoint("ep1", "p1");
        ProductEndpointConfig ep2 = endpoint("ep2", "p2");

        ProductModelProvider p1 = provider("p1", ProductProviderCapabilities.chatOnly());
        ProductModelProvider p2 = provider("p2", ProductProviderCapabilities.chatAndStreaming());

        Map<String, ProductEndpointConfig> endpoints = Map.of("ep1", ep1, "ep2", ep2);
        Map<String, ProductModelProvider> providers = Map.of("p1", p1, "p2", p2);
        RoutePolicy routePolicy = new RoutePolicy();
        routePolicy.setWeightedEndpoints(new java.util.LinkedHashMap<>(Map.of("ep1", 1, "ep2", 1)));

        List<ProductEndpointConfig> candidates = ROUTER.resolveCandidates(
            "model", endpoints, Map.of(), Map.of("model", routePolicy),
            CapabilityRequirement.NONE, providers);

        assertEquals(2, candidates.size());
    }

    @Test
    void shouldKeepAllWhenProviderMapIsEmpty() {
        ProductEndpointConfig ep1 = endpoint("ep1", "unknown");
        RoutePolicy routePolicy = new RoutePolicy();
        routePolicy.setPrimaryEndpoint("ep1");

        List<ProductEndpointConfig> candidates = ROUTER.resolveCandidates(
            "model", Map.of("ep1", ep1), Map.of(), Map.of("model", routePolicy),
            CapabilityRequirement.STREAMING, Map.of());

        assertEquals(1, candidates.size());
    }

    @Test
    void backwardCompatibleResolveCandidatesShouldNotFilter() {
        ProductEndpointConfig ep1 = endpoint("ep1", "chat-only");
        ProductModelProvider chatOnly = provider("chat-only", ProductProviderCapabilities.chatOnly());
        RoutePolicy routePolicy = new RoutePolicy();
        routePolicy.setPrimaryEndpoint("ep1");

        List<ProductEndpointConfig> candidates = ROUTER.resolveCandidates(
            "model", Map.of("ep1", ep1), Map.of(), Map.of("model", routePolicy));

        assertEquals(1, candidates.size());
    }

    private static ProductEndpointConfig endpoint(String id, String providerCode) {
        return ProductEndpointConfig.builder()
            .id(id)
            .provider(providerCode)
            .baseUrl("http://localhost:8000")
            .apiKey("key")
            .readTimeout(Duration.ofSeconds(30))
            .build();
    }

    private static ProductModelProvider provider(String code, ProductProviderCapabilities caps) {
        return new ProductModelProvider() {
            @Override
            public String providerCode() { return code; }
            @Override
            public ProductProviderCapabilities capabilities() { return caps; }
            @Override
            public dev.langchain4j.model.chat.ChatLanguageModel createChatLanguageModel(
                io.github.alcq77.cqagent.spi.model.ProductEndpointConfig endpoint, String logicalModel) {
                return null;
            }
        };
    }
}