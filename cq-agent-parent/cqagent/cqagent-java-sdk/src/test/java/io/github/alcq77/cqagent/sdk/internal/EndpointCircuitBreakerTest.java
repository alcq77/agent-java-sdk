package io.github.alcq77.cqagent.sdk.internal;

import io.github.alcq77.cqagent.sdk.internal.EndpointCircuitBreaker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EndpointCircuitBreakerTest {

    @Test
    void shouldOpenAfterThresholdAndKeepLastErrorSummary() {
        EndpointCircuitBreaker breaker = new EndpointCircuitBreaker(2, 30);
        assertTrue(breaker.allowRequest());
        breaker.onFailure("upstream timeout");
        assertTrue(breaker.allowRequest());
        breaker.onFailure("upstream 500");
        assertFalse(breaker.allowRequest());
        EndpointCircuitBreaker.Snapshot snapshot = breaker.snapshot();
        assertTrue(snapshot.open());
        assertEquals("upstream 500", snapshot.lastErrorMessage());
        assertTrue(snapshot.openedCount() >= 1);
        assertTrue(snapshot.rejectedCount() >= 1);
    }
}
