package io.github.alcq77.cqagent.core.multiagent;

import io.github.alcq77.cqagent.agent.api.dto.AgentChatResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultCoordinatorTest {

    @Test
    void shouldReturnEmptyMessageForEmptyPlan() {
        DefaultCoordinator coordinator = new DefaultCoordinator();
        AgentPlan plan = AgentPlan.of("empty", List.of());
        AgentChatResponse response = coordinator.execute(plan, Map.of());
        assertEquals("No sub-tasks to execute.", response.getReply());
    }

    @Test
    void shouldHandleMissingAgent() {
        DefaultCoordinator coordinator = new DefaultCoordinator();
        SubTask task = SubTask.of("t1", "Research", "researcher");
        AgentPlan plan = AgentPlan.of("plan", List.of(task));
        // No agents registered
        AgentChatResponse response = coordinator.execute(plan, Map.of());
        assertTrue(response.getReply().contains("No agent available"));
    }
}