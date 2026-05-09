package io.github.alcq77.cqagent.sdk.internal;

import io.github.alcq77.cqagent.agent.api.error.AgentErrorCode;
import io.github.alcq77.cqagent.agent.api.error.AgentRuntimeException;
import io.github.alcq77.cqagent.core.agent.LangChain4jProductAgentRuntime;
import io.github.alcq77.cqagent.core.model.ProductModelRouter;
import io.github.alcq77.cqagent.core.observability.AgentRuntimeCounters;
import io.github.alcq77.cqagent.core.session.InMemoryProductSessionStore;
import io.github.alcq77.cqagent.sdk.ProductSdkOptions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddedAgentClientStandardErrorTest {

    @Test
    void shouldMapTimeoutToStandardError() throws Exception {
        EmbeddedAgentClient client = buildClient();
        Method map = EmbeddedAgentClient.class.getDeclaredMethod("mapToStandardError", RuntimeException.class, String.class);
        map.setAccessible(true);

        RuntimeException mapped = (RuntimeException) map.invoke(client, new IllegalStateException("invoke timeout after 1000ms"), "trace-x");

        assertTrue(mapped instanceof AgentRuntimeException);
        AgentRuntimeException ex = (AgentRuntimeException) mapped;
        assertEquals(AgentErrorCode.MODEL_INVOKE_TIMEOUT, ex.getCode());
        assertTrue(ex.isRetryable());
        assertEquals("trace-x", ex.getTraceId());
    }

    @Test
    void shouldMapToolValidationToStandardError() throws Exception {
        EmbeddedAgentClient client = buildClient();
        Method map = EmbeddedAgentClient.class.getDeclaredMethod("mapToStandardError", RuntimeException.class, String.class);
        map.setAccessible(true);

        RuntimeException mapped = (RuntimeException) map.invoke(
            client,
            new IllegalStateException("cqagent.tool.validation: missing field"),
            "trace-y"
        );

        AgentRuntimeException ex = (AgentRuntimeException) mapped;
        assertEquals(AgentErrorCode.TOOL_VALIDATION_ERROR, ex.getCode());
        assertTrue(!ex.isRetryable());
    }

    private static EmbeddedAgentClient buildClient() {
        LangChain4jProductAgentRuntime runtime = new LangChain4jProductAgentRuntime(
            new InMemoryProductSessionStore(20),
            List.of(),
            3,
            Map.of(),
            null,
            true,
            new AgentRuntimeCounters()
        );
        return new EmbeddedAgentClient(
            new ProductSdkOptions(),
            new ProductModelRouter(endpoint -> true),
            runtime,
            Map.of()
        );
    }
}
