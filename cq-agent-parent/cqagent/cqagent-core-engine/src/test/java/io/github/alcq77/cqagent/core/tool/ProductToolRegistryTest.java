package io.github.alcq77.cqagent.core.tool;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import io.github.alcq77.cqagent.core.observability.AgentRuntimeCounters;
import io.github.alcq77.cqagent.spi.tool.ProductTool;
import io.github.alcq77.cqagent.spi.tool.ProductToolParameterSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProductToolRegistryTest {

    @Test
    void shouldCountValidationFailureAndPrefixError() {
        AgentRuntimeCounters counters = new AgentRuntimeCounters();
        ProductToolRegistry registry = new ProductToolRegistry(List.of(new EchoTool()), counters);
        ToolExecutionRequest request = ToolExecutionRequest.builder()
            .name("echo")
            .arguments("{}")
            .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> registry.execute(request));
        assertTrue(ex.getMessage().startsWith(ProductToolRegistry.PREFIX_VALIDATION));
        assertEquals(1L, counters.snapshot().get("toolValidationFailures"));
        assertEquals(0L, counters.snapshot().get("toolInvocations"));
        assertEquals(0L, counters.snapshot().get("toolExecutionFailures"));
    }

    @Test
    void shouldCountExecutionFailureAndPrefixError() {
        AgentRuntimeCounters counters = new AgentRuntimeCounters();
        ProductToolRegistry registry = new ProductToolRegistry(List.of(new BrokenTool()), counters);
        ToolExecutionRequest request = ToolExecutionRequest.builder()
            .name("broken")
            .arguments("{\"input\":\"x\"}")
            .build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> registry.execute(request));
        assertTrue(ex.getMessage().startsWith(ProductToolRegistry.PREFIX_EXECUTION));
        assertEquals(1L, counters.snapshot().get("toolInvocations"));
        assertEquals(1L, counters.snapshot().get("toolExecutionFailures"));
    }

    @Test
    void shouldCountInvocationOnSuccess() {
        AgentRuntimeCounters counters = new AgentRuntimeCounters();
        ProductToolRegistry registry = new ProductToolRegistry(List.of(new EchoTool()), counters);
        ToolExecutionRequest request = ToolExecutionRequest.builder()
            .name("echo")
            .arguments("{\"input\":\"ok\"}")
            .build();

        String result = registry.execute(request);

        assertEquals("echo:ok", result);
        assertEquals(1L, counters.snapshot().get("toolInvocations"));
        assertEquals(0L, counters.snapshot().get("toolValidationFailures"));
        assertEquals(0L, counters.snapshot().get("toolExecutionFailures"));
    }

    private static class EchoTool implements ProductTool {

        @Override
        public String name() {
            return "echo";
        }

        @Override
        public boolean supports(String userInput) {
            return true;
        }

        @Override
        public String execute(String userInput) {
            return "echo:" + userInput;
        }

        @Override
        public Map<String, ProductToolParameterSpec> parameterSpecs() {
            return Map.of("input", ProductToolParameterSpec.string(true, "input"));
        }
    }

    private static final class BrokenTool extends EchoTool {
        @Override
        public String name() {
            return "broken";
        }

        @Override
        public String execute(Map<String, Object> arguments) {
            throw new IllegalStateException("boom");
        }
    }
}
