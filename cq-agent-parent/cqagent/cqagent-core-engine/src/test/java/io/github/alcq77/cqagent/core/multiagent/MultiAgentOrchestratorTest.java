package io.github.alcq77.cqagent.core.multiagent;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import io.github.alcq77.cqagent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqagent.agent.api.dto.AgentChatResponse;
import io.github.alcq77.cqagent.core.session.InMemoryProductSessionStore;
import io.github.alcq77.cqagent.spi.session.ProductSessionStore;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MultiAgentOrchestratorTest {

    @Test
    void shouldRejectBuildWithoutPlanner() {
        assertThrows(IllegalStateException.class, () ->
            MultiAgentOrchestrator.builder()
                .agent("test", mockSubAgent("test"))
                .build());
    }

    @Test
    void shouldRejectBuildWithoutAgents() {
        assertThrows(IllegalStateException.class, () ->
            MultiAgentOrchestrator.builder()
                .planner(mockPlanner("task-0", "default"))
                .build());
    }

    @Test
    void shouldBuildWithAllComponents() {
        MultiAgentOrchestrator orchestrator = MultiAgentOrchestrator.builder()
            .planner(mockPlanner("task-0", "default"))
            .agent("default", mockSubAgent("default"))
            .build();

        assertNotNull(orchestrator);
        assertEquals(Set.of("default"), orchestrator.availableRoles());
        assertNotNull(orchestrator.agent("default"));
        assertNull(orchestrator.agent("nonexistent"));
    }

    @Test
    void shouldExecuteChatThroughOrchestrator() {
        ChatLanguageModel plannerModel = message -> Response.from(AiMessage.from(
            """
            {"summary":"Plan","subTasks":[{"id":"t1","description":"Do something","agentRole":"default","dependsOn":[]}]}
            """));
        ChatLanguageModel agentModel = message -> Response.from(AiMessage.from("Result from agent"));

        ProductSessionStore store = new InMemoryProductSessionStore(10);
        SubAgent agent = new SubAgent("default", "You are a helper.", agentModel, java.util.List.of(), store);

        MultiAgentOrchestrator orchestrator = MultiAgentOrchestrator.builder()
            .planner(mockPlanner("t1", "default"))
            .agent("default", agent)
            .build();

        AgentChatRequest request = AgentChatRequest.builder()
            .message("Hello, do something!")
            .build();

        AgentChatResponse response = orchestrator.chat(request);
        assertNotNull(response.getReply());
        assertTrue(response.getReply().contains("Result from agent"));
    }

    private static SubAgent mockSubAgent(String role) {
        ChatLanguageModel model = message -> Response.from(AiMessage.from("mock"));
        ProductSessionStore store = new InMemoryProductSessionStore(10);
        return new SubAgent(role, "Mock agent", model, java.util.List.of(), store);
    }

    private static Planner mockPlanner(String taskId, String role) {
        return (userMessage, availableRoles) -> AgentPlan.of(
            "mock plan",
            java.util.List.of(SubTask.of(taskId, userMessage, role))
        );
    }
}