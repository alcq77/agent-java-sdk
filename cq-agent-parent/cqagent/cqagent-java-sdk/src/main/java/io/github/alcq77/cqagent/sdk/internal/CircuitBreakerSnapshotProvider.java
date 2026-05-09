package io.github.alcq77.cqagent.sdk.internal;

import java.util.Map;

/**
 * 提供熔断状态快照，供上层暴露健康与监控信息。
 */
public interface CircuitBreakerSnapshotProvider {

    Map<String, EndpointCircuitBreaker.Snapshot> circuitBreakerSnapshot();
}
