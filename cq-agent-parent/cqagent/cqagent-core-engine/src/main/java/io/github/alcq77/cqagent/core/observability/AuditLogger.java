package io.github.alcq77.cqagent.core.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Structured audit event logger.
 *
 * <p>Logs audit events as structured JSON lines via SLF4J, and optionally
 * stores recent events in an in-memory ring buffer for actuator exposure.</p>
 *
 * <p>Usage:
 * <pre>{@code
 * AuditLogger logger = new AuditLogger(1000); // keep last 1000 events
 * logger.log(AuditEvent.builder()
 *     .type("CHAT_COMPLETE")
 *     .traceId(traceId)
 *     .provider("openai")
 *     .durationMs(1234)
 *     .success(true)
 *     .build());
 * }</pre>
 */
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger("io.github.alcq77.cqagent.audit");

    private final int maxBufferSize;
    private final ConcurrentLinkedQueue<AuditEvent> recentEvents;

    /**
     * @param maxBufferSize max number of recent events to retain in memory (0 to disable buffering)
     */
    public AuditLogger(int maxBufferSize) {
        this.maxBufferSize = Math.max(0, maxBufferSize);
        this.recentEvents = new ConcurrentLinkedQueue<>();
    }

    /**
     * Logs an audit event. The event is always written to SLF4J at INFO level.
     * If buffering is enabled, it is also stored in the in-memory ring buffer.
     */
    public void log(AuditEvent event) {
        if (event == null) return;
        if (log.isInfoEnabled()) {
            log.info("type={} traceId={} sessionId={} provider={} endpoint={} model={} durationMs={} success={} error={}",
                event.type(),
                event.traceId(),
                event.sessionId(),
                event.provider(),
                event.endpointId(),
                event.logicalModel(),
                event.durationMs(),
                event.success(),
                event.errorMessage());
        }
        if (maxBufferSize > 0) {
            recentEvents.add(event);
            while (recentEvents.size() > maxBufferSize) {
                recentEvents.poll();
            }
        }
    }

    /**
     * Returns a snapshot of recent audit events (newest first).
     */
    public java.util.List<AuditEvent> recentEvents() {
        return new java.util.ArrayList<>(recentEvents);
    }

    /**
     * Returns the number of buffered events.
     */
    public int bufferedCount() {
        return recentEvents.size();
    }
}
