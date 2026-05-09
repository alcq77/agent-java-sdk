package io.github.alcq77.cqagent.sdk.internal;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于连续失败次数的轻量熔断器。
 */
public class EndpointCircuitBreaker {

    private final int failureThreshold;
    private final long openMillis;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong openUntilMs = new AtomicLong(0);
    private final AtomicLong openedCount = new AtomicLong(0);
    private final AtomicLong rejectedCount = new AtomicLong(0);
    private volatile String lastErrorMessage = "";
    private final AtomicLong lastFailureAtMs = new AtomicLong(0);

    public EndpointCircuitBreaker(int failureThreshold, int openSeconds) {
        this.failureThreshold = Math.max(1, failureThreshold);
        this.openMillis = Math.max(1, openSeconds) * 1000L;
    }

    /**
     * 熔断打开时返回 false，冷却期结束后自动半开重试。
     */
    public boolean allowRequest() {
        long now = System.currentTimeMillis();
        long until = openUntilMs.get();
        if (now < until) {
            rejectedCount.incrementAndGet();
            return false;
        }
        return true;
    }

    /**
     * 成功后清空失败计数并关闭熔断状态。
     */
    public void onSuccess() {
        consecutiveFailures.set(0);
        openUntilMs.set(0);
    }

    /**
     * 失败时累计计数，达到阈值后打开熔断。
     */
    public void onFailure(String message) {
        int failures = consecutiveFailures.incrementAndGet();
        lastErrorMessage = message == null ? "" : message;
        lastFailureAtMs.set(System.currentTimeMillis());
        if (failures >= failureThreshold) {
            openUntilMs.set(System.currentTimeMillis() + openMillis);
            openedCount.incrementAndGet();
        }
    }

    /**
     * 导出当前熔断状态，用于健康检查和观测上报。
     */
    public Snapshot snapshot() {
        long now = System.currentTimeMillis();
        long until = openUntilMs.get();
        return new Snapshot(
                now < until,
                Math.max(0L, until - now),
                consecutiveFailures.get(),
                openedCount.get(),
                rejectedCount.get(),
                lastErrorMessage,
                lastFailureAtMs.get()
        );
    }

    public record Snapshot(boolean open,
                           long openRemainingMs,
                           int consecutiveFailures,
                           long openedCount,
                           long rejectedCount,
                           String lastErrorMessage,
                           long lastFailureAtMs) {
    }
}
