package io.github.alcq77.cqagent.core.observability;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuditEventTest {

    @Test
    void shouldBuildAuditEvent() {
        AuditEvent event = AuditEvent.builder()
            .type("CHAT_COMPLETE")
            .traceId("abc-123")
            .sessionId("sess-1")
            .provider("openai")
            .endpointId("ep-1")
            .logicalModel("gpt-4o")
            .durationMs(1234)
            .success(true)
            .build();

        assertEquals("CHAT_COMPLETE", event.type());
        assertEquals("abc-123", event.traceId());
        assertEquals("sess-1", event.sessionId());
        assertEquals("openai", event.provider());
        assertEquals("ep-1", event.endpointId());
        assertEquals("gpt-4o", event.logicalModel());
        assertEquals(1234, event.durationMs());
        assertTrue(event.success());
        assertNull(event.errorMessage());
    }

    @Test
    void shouldBuildErrorEvent() {
        AuditEvent event = AuditEvent.builder()
            .type("CHAT_ERROR")
            .traceId("err-1")
            .success(false)
            .errorMessage("timeout")
            .metadata(Map.of("retryCount", 3))
            .build();

        assertFalse(event.success());
        assertEquals("timeout", event.errorMessage());
        assertEquals(3, event.metadata().get("retryCount"));
    }
}
