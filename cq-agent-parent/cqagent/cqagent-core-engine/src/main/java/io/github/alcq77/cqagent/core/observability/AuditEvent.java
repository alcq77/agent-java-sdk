package io.github.alcq77.cqagent.core.observability;

import java.time.Instant;
import java.util.Map;

/**
 * Structured audit event for significant agent operations.
 *
 * <p>Audit events capture the what, when, and outcome of key operations
 * without leaking sensitive data (API keys, full prompts). They are designed
 * for compliance logging, operational dashboards, and post-incident analysis.</p>
 *
 * <p>Event types:
 * <ul>
 *   <li>{@code CHAT_START} / {@code CHAT_COMPLETE} / {@code CHAT_ERROR} — sync request lifecycle</li>
 *   <li>{@code STREAM_START} / {@code STREAM_COMPLETE} / {@code STREAM_ERROR} — streaming lifecycle</li>
 *   <li>{@code TOOL_EXECUTION} — tool call result</li>
 *   <li>{@code CIRCUIT_BREAKER_OPEN} — circuit breaker state change</li>
 *   <li>{@code ROUTE_RESOLVED} — routing decision</li>
 * </ul>
 */
public record AuditEvent(
    String type,
    String traceId,
    String sessionId,
    String provider,
    String endpointId,
    String logicalModel,
    long durationMs,
    boolean success,
    String errorMessage,
    Map<String, Object> metadata
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String type;
        private String traceId;
        private String sessionId;
        private String provider;
        private String endpointId;
        private String logicalModel;
        private long durationMs;
        private boolean success = true;
        private String errorMessage;
        private Map<String, Object> metadata = Map.of();

        public Builder type(String type) { this.type = type; return this; }
        public Builder traceId(String traceId) { this.traceId = traceId; return this; }
        public Builder sessionId(String sessionId) { this.sessionId = sessionId; return this; }
        public Builder provider(String provider) { this.provider = provider; return this; }
        public Builder endpointId(String endpointId) { this.endpointId = endpointId; return this; }
        public Builder logicalModel(String logicalModel) { this.logicalModel = logicalModel; return this; }
        public Builder durationMs(long durationMs) { this.durationMs = durationMs; return this; }
        public Builder success(boolean success) { this.success = success; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }

        public AuditEvent build() {
            return new AuditEvent(type, traceId, sessionId, provider, endpointId,
                logicalModel, durationMs, success, errorMessage, metadata);
        }
    }
}