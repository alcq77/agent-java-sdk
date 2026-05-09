package io.github.alcq77.cqagent.core.observability;

import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * 运行时轻量计数器（无外部依赖），供健康检查与排障观测使用。
 * <p>
 * 线程安全：基于 {@link LongAdder}。
 */
public final class AgentRuntimeCounters {

    private final LongAdder syncChatInvocations = new LongAdder();
    private final LongAdder streamingInvocations = new LongAdder();
    private final LongAdder toolInvocations = new LongAdder();
    private final LongAdder toolValidationFailures = new LongAdder();
    private final LongAdder toolExecutionFailures = new LongAdder();

    public void incrementSyncChatInvocation() {
        syncChatInvocations.increment();
    }

    public void incrementStreamingInvocation() {
        streamingInvocations.increment();
    }

    public void incrementToolInvocation() {
        toolInvocations.increment();
    }

    public void incrementToolValidationFailure() {
        toolValidationFailures.increment();
    }

    public void incrementToolExecutionFailure() {
        toolExecutionFailures.increment();
    }

    public Map<String, Object> snapshot() {
        return Map.of(
            "syncChatInvocations", syncChatInvocations.longValue(),
            "streamingInvocations", streamingInvocations.longValue(),
            "toolInvocations", toolInvocations.longValue(),
            "toolValidationFailures", toolValidationFailures.longValue(),
            "toolExecutionFailures", toolExecutionFailures.longValue()
        );
    }
}
