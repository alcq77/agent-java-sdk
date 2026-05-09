package io.github.alcq77.cqagent.starter;

import io.github.alcq77.cqagent.sdk.AgentClient;
import io.github.alcq77.cqagent.sdk.internal.AgentRuntimeMetricsProvider;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import java.util.Map;

/**
 * 将 {@link AgentRuntimeMetricsProvider} 中的运行时指标暴露为 Micrometer {@link Gauge}，
 * 便于通过 Prometheus 等后端抓取（需引入 {@code micrometer-registry-prometheus} 并暴露端点）。
 */
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
public class CqagentMetricsAutoConfiguration {

    @Bean
    @ConditionalOnBean(AgentClient.class)
    public MeterBinder cqagentRuntimeMeterBinder(AgentClient agentClient) {
        return registry -> bindRuntimeGauges(registry, agentClient);
    }

    private static void bindRuntimeGauges(MeterRegistry registry, AgentClient agentClient) {
        if (!(agentClient instanceof AgentRuntimeMetricsProvider provider)) {
            return;
        }
        // Core counters
        bind(registry, provider, "cqagent.runtime.requests.total", "totalRequests");
        bind(registry, provider, "cqagent.runtime.failures.total", "totalFailures");
        bind(registry, provider, "cqagent.runtime.circuit.skipped", "circuitSkipped");
        bind(registry, provider, "cqagent.runtime.audit.events.buffered", "auditEventsBuffered");
        // Invocation counters
        bind(registry, provider, "cqagent.runtime.chat.sync.invocations", "syncChatInvocations");
        bind(registry, provider, "cqagent.runtime.chat.streaming.invocations", "streamingInvocations");
        bind(registry, provider, "cqagent.runtime.tools.invocations", "toolInvocations");
        bind(registry, provider, "cqagent.runtime.tools.validation.failures", "toolValidationFailures");
        bind(registry, provider, "cqagent.runtime.tools.execution.failures", "toolExecutionFailures");
        // Derived gauge: failure rate
        Gauge.builder("cqagent.runtime.failure.rate", provider, p -> {
            Map<String, Object> m = p.runtimeMetrics();
            long requests = longValue(m.get("totalRequests"));
            long failures = longValue(m.get("totalFailures"));
            return requests > 0 ? (double) failures / requests : 0d;
        }).register(registry);
    }

    private static void bind(MeterRegistry registry, AgentRuntimeMetricsProvider provider, String metric, String key) {
        Gauge.builder(metric, provider, p -> doubleValue(p.runtimeMetrics().get(key)))
                .register(registry);
    }

    private static double doubleValue(Object v) {
        return v instanceof Number ? ((Number) v).doubleValue() : 0d;
    }

    private static long longValue(Object v) {
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }
}
