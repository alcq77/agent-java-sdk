package io.github.alcq77.cqagent.core.session;

import io.github.alcq77.cqagent.model.api.dto.ChatMessageDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionStoreMigratorTest {

    @Test
    void copiesSessionsWhenOverwrite() {
        InMemoryProductSessionStore source = new InMemoryProductSessionStore(40);
        InMemoryProductSessionStore target = new InMemoryProductSessionStore(40);
        source.register("s1");
        ChatMessageDto user = ChatMessageDto.builder().role("user").content("hi").build();
        ChatMessageDto assistant = ChatMessageDto.builder().role("assistant").content("hello").build();
        source.append("s1", user, assistant);

        SessionStoreMigrator.MigrationResult result = SessionStoreMigrator.migrate(source, target, true);

        assertEquals(1, result.scanned());
        assertEquals(1, result.copied());
        assertEquals(0, result.skipped());
        assertTrue(result.errors().isEmpty());
        assertEquals(2, target.history("s1").size());
    }

    @Test
    void skipsExistingWhenNoOverwrite() {
        InMemoryProductSessionStore source = new InMemoryProductSessionStore(40);
        InMemoryProductSessionStore target = new InMemoryProductSessionStore(40);
        source.register("s1");
        source.append("s1",
                ChatMessageDto.builder().role("user").content("a").build(),
                ChatMessageDto.builder().role("assistant").content("b").build());
        target.register("s1");
        target.replaceHistory("s1", java.util.List.of(
                ChatMessageDto.builder().role("user").content("old").build()));

        SessionStoreMigrator.MigrationResult result = SessionStoreMigrator.migrate(source, target, false);

        assertEquals(1, result.skipped());
        assertEquals(0, result.copied());
        assertEquals("user", target.history("s1").get(0).getRole());
        assertEquals("old", target.history("s1").get(0).getContent());
    }
}
