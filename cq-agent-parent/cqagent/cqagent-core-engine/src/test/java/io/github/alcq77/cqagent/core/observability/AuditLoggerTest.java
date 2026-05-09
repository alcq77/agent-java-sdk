package io.github.alcq77.cqagent.core.observability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditLoggerTest {

    @Test
    void shouldBufferEventsUpToLimit() {
        AuditLogger logger = new AuditLogger(5);
        for (int i = 0; i < 10; i++) {
            logger.log(AuditEvent.builder().type("EVENT_" + i).build());
        }
        assertEquals(5, logger.bufferedCount());
        assertEquals(5, logger.recentEvents().size());
    }

    @Test
    void shouldHandleNullEvent() {
        AuditLogger logger = new AuditLogger(10);
        assertDoesNotThrow(() -> logger.log(null));
        assertEquals(0, logger.bufferedCount());
    }

    @Test
    void shouldWorkWithZeroBufferSize() {
        AuditLogger logger = new AuditLogger(0);
        logger.log(AuditEvent.builder().type("EVENT").build());
        assertEquals(0, logger.bufferedCount());
    }

    @Test
    void shouldReturnEventsInInsertionOrder() {
        AuditLogger logger = new AuditLogger(10);
        logger.log(AuditEvent.builder().type("FIRST").build());
        logger.log(AuditEvent.builder().type("SECOND").build());
        logger.log(AuditEvent.builder().type("THIRD").build());

        var events = logger.recentEvents();
        assertEquals(3, events.size());
        assertEquals("FIRST", events.get(0).type());
        assertEquals("SECOND", events.get(1).type());
        assertEquals("THIRD", events.get(2).type());
    }
}