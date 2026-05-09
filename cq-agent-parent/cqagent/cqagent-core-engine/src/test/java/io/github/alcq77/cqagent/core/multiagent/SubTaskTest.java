package io.github.alcq77.cqagent.core.multiagent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SubTaskTest {

    @Test
    void shouldCreateSimpleSubTask() {
        SubTask task = SubTask.of("t1", "Research topic", "researcher");
        assertEquals("t1", task.id());
        assertEquals("Research topic", task.description());
        assertEquals("researcher", task.agentRole());
        assertTrue(task.dependsOn().isEmpty());
    }

    @Test
    void shouldCreateSubTaskWithDependencies() {
        SubTask task = SubTask.of("t2", "Write code", "coder", List.of("t1"));
        assertEquals(List.of("t1"), task.dependsOn());
    }

    @Test
    void shouldRejectBlankId() {
        assertThrows(IllegalArgumentException.class, () ->
            SubTask.of("", "desc", "role"));
    }

    @Test
    void shouldCopyLists() {
        SubTask task = SubTask.of("t1", "desc", "role");
        List<String> deps = task.dependsOn();
        // The returned list should be immutable
        assertThrows(UnsupportedOperationException.class, () -> deps.add("x"));
    }
}