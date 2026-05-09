package io.github.alcq77.cqagent.core.multiagent;

import io.github.alcq77.cqagent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqagent.agent.api.dto.AgentChatResponse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orchestrates multiple specialized agents to handle complex tasks.
 *
 * <p>The orchestrator follows a plan-execute-aggregate pattern:
 * <ol>
 *   <li>The {@link Planner} decomposes the user's request into sub-tasks</li>
 *   <li>The {@link Coordinator} assigns sub-tasks to {@link SubAgent}s and executes them</li>
 *   <li>Results are aggregated into a single coherent response</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>{@code
 * MultiAgentOrchestrator orchestrator = MultiAgentOrchestrator.builder()
 *     .planner(new LlmPlanner(plannerModel))
 *     .coordinator(new DefaultCoordinator())
 *     .agent("researcher", new SubAgent("researcher", "You research topics.", researchModel, tools, store))
 *     .agent("coder", new SubAgent("coder", "You write code.", coderModel, tools, store))
 *     .build();
 *
 * AgentChatResponse response = orchestrator.chat(request);
 * }</pre>
 */
public class MultiAgentOrchestrator {

    private final Planner planner;
    private final Coordinator coordinator;
    private final Map<String, SubAgent> agents;

    private MultiAgentOrchestrator(Builder builder) {
        this.planner = builder.planner;
        this.coordinator = builder.coordinator;
        this.agents = Map.copyOf(builder.agents);
    }

    /**
     * Decomposes the user's message into a plan, executes sub-tasks, and returns aggregated results.
     */
    public AgentChatResponse chat(AgentChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("request and message must not be null or blank");
        }
        String userMessage = request.getMessage();
        Set<String> availableRoles = agents.keySet();
        AgentPlan plan = planner.plan(userMessage, availableRoles);
        return coordinator.execute(plan, agents);
    }

    /**
     * Returns the available agent roles.
     */
    public Set<String> availableRoles() {
        return agents.keySet();
    }

    /**
     * Returns the agent for a given role, or null if not found.
     */
    public SubAgent agent(String role) {
        return agents.get(role);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Planner planner;
        private Coordinator coordinator = new DefaultCoordinator();
        private final Map<String, SubAgent> agents = new LinkedHashMap<>();

        public Builder planner(Planner planner) {
            this.planner = planner;
            return this;
        }

        public Builder coordinator(Coordinator coordinator) {
            this.coordinator = coordinator;
            return this;
        }

        public Builder agent(String role, SubAgent agent) {
            this.agents.put(role, agent);
            return this;
        }

        public MultiAgentOrchestrator build() {
            if (planner == null) {
                throw new IllegalStateException("planner must be set");
            }
            if (agents.isEmpty()) {
                throw new IllegalStateException("at least one agent must be registered");
            }
            return new MultiAgentOrchestrator(this);
        }
    }
}