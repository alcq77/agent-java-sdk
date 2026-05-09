package io.github.alcq77.cqagent.starter;

import io.github.alcq77.cqagent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqagent.agent.api.dto.AgentChatResponse;
import io.github.alcq77.cqagent.sdk.AgentClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.support.StaticApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductHealthIndicatorRagPolicyTest {

    @Test
    void shouldBeDownWhenRagUnhealthyAndStrictEnabled() {
        ProductStarterProperties properties = baseProps();
        properties.getRag().setHealthStrict(true);

        RagIndexRefresher refresher = new RagIndexRefresher(null, null, null, java.nio.file.Path.of("."), 0) {
            @Override
            public boolean healthy() {
                return false;
            }

            @Override
            public java.util.Map<String, Object> snapshot() {
                return java.util.Map.of("healthy", false, "lastError", "boom");
            }
        };

        StaticApplicationContext ctx = new StaticApplicationContext();
        ctx.getBeanFactory().registerSingleton("ragRefresher", refresher);
        ProductAgentAutoConfiguration auto = new ProductAgentAutoConfiguration();
        HealthIndicator indicator = auto.productStarterHealthIndicator(
                properties, new NoopClient(), ctx.getBeanProvider(io.github.alcq77.cqagent.spi.session.ProductSessionStore.class),
                ctx.getBeanProvider(RagIndexRefresher.class)
        );

        Health health = indicator.health();
        assertEquals("DOWN", health.getStatus().getCode());
    }

    @Test
    void shouldStayUpWhenRagUnhealthyAndStrictDisabled() {
        ProductStarterProperties properties = baseProps();
        properties.getRag().setHealthStrict(false);

        RagIndexRefresher refresher = new RagIndexRefresher(null, null, null, java.nio.file.Path.of("."), 0) {
            @Override
            public boolean healthy() {
                return false;
            }

            @Override
            public java.util.Map<String, Object> snapshot() {
                return java.util.Map.of("healthy", false, "lastError", "boom");
            }
        };

        StaticApplicationContext ctx = new StaticApplicationContext();
        ctx.getBeanFactory().registerSingleton("ragRefresher", refresher);
        ProductAgentAutoConfiguration auto = new ProductAgentAutoConfiguration();
        HealthIndicator indicator = auto.productStarterHealthIndicator(
                properties, new NoopClient(), ctx.getBeanProvider(io.github.alcq77.cqagent.spi.session.ProductSessionStore.class),
                ctx.getBeanProvider(RagIndexRefresher.class)
        );

        Health health = indicator.health();
        assertEquals("UP", health.getStatus().getCode());
    }

    private static ProductStarterProperties baseProps() {
        ProductStarterProperties properties = new ProductStarterProperties();
        ProductStarterProperties.Endpoint endpoint = new ProductStarterProperties.Endpoint();
        endpoint.setBaseUrl("http://127.0.0.1");
        properties.getEndpoints().put("primary", endpoint);
        return properties;
    }

    private static class NoopClient implements AgentClient {
        @Override
        public AgentChatResponse chat(AgentChatRequest request) {
            return AgentChatResponse.builder().reply("ok").build();
        }
    }
}
