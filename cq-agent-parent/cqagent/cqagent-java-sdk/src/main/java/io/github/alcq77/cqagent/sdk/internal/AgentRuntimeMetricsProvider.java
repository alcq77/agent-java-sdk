package io.github.alcq77.cqagent.sdk.internal;

import java.util.Map;

/**
 * 运行时指标快照提供者。
 */
public interface AgentRuntimeMetricsProvider {

    /**
     * 返回当前运行态指标（用于健康检查与观测上报）。
     */
    Map<String, Object> runtimeMetrics();
}
