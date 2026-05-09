package io.github.alcq77.cqagent.sdk.internal;

import io.github.alcq77.cqagent.core.agent.LangChain4jProductAgentRuntime;
import io.github.alcq77.cqagent.core.model.ProductModelRouter;
import io.github.alcq77.cqagent.core.observability.AgentRuntimeCounters;
import io.github.alcq77.cqagent.core.session.InMemoryProductSessionStore;
import io.github.alcq77.cqagent.core.tool.ProductToolRegistry;
import io.github.alcq77.cqagent.sdk.ProductSdkOptions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddedAgentClientMetricsTest {

    @Test
    void shouldMergeRuntimeCountersIntoMetricsSnapshot() {
        LangChain4jProductAgentRuntime runtime = new LangChain4jProductAgentRuntime(
            new InMemoryProductSessionStore(20),
            List.of(),
            3,
            Map.of(),
            null,
            true,
            new AgentRuntimeCounters()
        );
        EmbeddedAgentClient client = new EmbeddedAgentClient(
            new ProductSdkOptions(),
            new ProductModelRouter(endpoint -> true),
            runtime,
            Map.of()
        );
        runtime.runtimeCounters().incrementSyncChatInvocation();
        runtime.runtimeCounters().incrementStreamingInvocation();
        runtime.runtimeCounters().incrementToolInvocation();
        runtime.runtimeCounters().incrementToolValidationFailure();
        runtime.runtimeCounters().incrementToolExecutionFailure();

        Map<String, Object> metrics = client.runtimeMetrics();

        assertEquals(1L, metrics.get("syncChatInvocations"));
        assertEquals(1L, metrics.get("streamingInvocations"));
        assertEquals(1L, metrics.get("toolInvocations"));
        assertEquals(1L, metrics.get("toolValidationFailures"));
        assertEquals(1L, metrics.get("toolExecutionFailures"));
    }

    @Test
    void shouldClassifyToolFailuresByPrefix() throws Exception {
        Method method = EmbeddedAgentClient.class.getDeclaredMethod("classifyFailureType", RuntimeException.class);
        method.setAccessible(true);

        String validation = (String) method.invoke(null, new IllegalStateException(
            ProductToolRegistry.PREFIX_VALIDATION + "bad args"));
        String execution = (String) method.invoke(null, new IllegalStateException(
            ProductToolRegistry.PREFIX_EXECUTION + "boom"));

        assertEquals("tool_validation", validation);
        assertEquals("tool_execution", execution);
        assertTrue(!validation.equals(execution));
    }
}
