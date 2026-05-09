package io.github.alcq77.cqagent.core.multiagent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgentPlanTest {

    @Test
    void shouldCreateEmptyPlan() {
        AgentPlan plan = AgentPlan.of("empty", List.of());
        assertTrue(plan.isEmpty());
        assertEquals(0, plan.size());
        assertTrue(plan.executionLevels().isEmpty());
    }

    @Test
    void shouldComputeExecutionLevels() {
        SubTask t1 = SubTask.of("t1", "Research", "researcher");
        SubTask t2 = SubTask.of("t2", "Code", "coder", List.of("t1"));
        SubTask t3 = SubTask.of("t3", "Review", "reviewer", List.of("t2"));

        AgentPlan plan = AgentPlan.of("plan", List.of(t1, t2, t3));
        List<List<SubTask>> levels = plan.executionLevels();

        assertEquals(3, levels.size());
        assertEquals("t1", levels.get(0).get(0).id());
        assertEquals("t2", levels.get(1).get(0).id());
        assertEquals("t3", levels.get(2).get(0).id());
    }

    @Test
    void shouldHandleParallelTasks() {
        SubTask t1 = SubTask.of("t1", "Research A", "researcher");
        SubTask t2 = SubTask.of("t2", "Research B", "researcher");
        SubTask t3 = SubTask.of("t3", "Combine", "analyst", List.of("t1", "t2"));

        AgentPlan plan = AgentPlan.of("plan", List.of(t1, t2, t3));
        List<List<SubTask>> levels = plan.executionLevels();

        assertEquals(2, levels.size());
        assertEquals(2, levels.get(0).size()); // t1 and t2 in parallel
        assertEquals(1, levels.get(1).size()); // t3 depends on both
    }

    @Test
    void shouldReturnSize() {
        SubTask t1 = SubTask.of("t1", "A", "agent");
        SubTask t2 = SubTask.of("t2", "B", "agent");
        AgentPlan plan = AgentPlan.of("plan", List.of(t1, t2));
        assertEquals(2, plan.size());
        assertFalse(plan.isEmpty());
    }
}