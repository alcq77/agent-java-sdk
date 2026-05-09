package io.github.alcq77.cqagent.core.multiagent;

import io.github.alcq77.cqagent.agent.api.dto.AgentChatResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Default coordinator that executes sub-tasks level by level (respecting dependencies).
 * <p>
 * Tasks within the same level are executed in parallel using a shared thread pool,
 * then results are collected before moving to the next level.
 */
public class DefaultCoordinator implements Coordinator {

    private static final int MAX_PARALLELISM = 8;

    private final ExecutorService executor;

    public DefaultCoordinator() {
        this(Executors.newFixedThreadPool(Math.min(Runtime.getRuntime().availableProcessors(), MAX_PARALLELISM)));
    }

    /**
     * Custom executor for advanced use cases (e.g., virtual threads, custom pool size).
     */
    public DefaultCoordinator(ExecutorService executor) {
        this.executor = executor != null ? executor : Executors.newFixedThreadPool(MAX_PARALLELISM);
    }

    @Override
    public AgentChatResponse execute(AgentPlan plan, Map<String, SubAgent> agents) {
        if (plan.isEmpty()) {
            return AgentChatResponse.builder()
                .reply("No sub-tasks to execute.")
                .build();
        }

        List<List<SubTask>> levels = plan.executionLevels();
        Map<String, String> results = new LinkedHashMap<>();
        int totalInputTokens = 0;
        int totalOutputTokens = 0;

        for (List<SubTask> level : levels) {
            if (level.size() == 1) {
                // Single task in level — execute directly (no thread overhead)
                SubTask subTask = level.get(0);
                AgentChatResponse response = executeSingle(subTask, agents, results);
                results.put(subTask.id(), response.getReply());
                totalInputTokens += nullSafeTokens(response.getInputTokens());
                totalOutputTokens += nullSafeTokens(response.getOutputTokens());
            } else {
                // Multiple tasks in level — execute in parallel
                List<Future<AgentChatResponse>> futures = new ArrayList<>();
                for (SubTask subTask : level) {
                    futures.add(executor.submit(() -> executeSingle(subTask, agents, results)));
                }
                // Collect results (order preserved by futures list)
                for (int i = 0; i < level.size(); i++) {
                    SubTask subTask = level.get(i);
                    try {
                        AgentChatResponse response = futures.get(i).get();
                        results.put(subTask.id(), response.getReply());
                        totalInputTokens += nullSafeTokens(response.getInputTokens());
                        totalOutputTokens += nullSafeTokens(response.getOutputTokens());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        results.put(subTask.id(), "[Interrupted: " + e.getMessage() + "]");
                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();
                        results.put(subTask.id(), "[Error: " + (cause != null ? cause.getMessage() : e.getMessage()) + "]");
                    }
                }
            }
        }

        String aggregatedReply = aggregateResults(plan, results);
        return AgentChatResponse.builder()
            .reply(aggregatedReply)
            .inputTokens(totalInputTokens)
            .outputTokens(totalOutputTokens)
            .totalTokens(totalInputTokens + totalOutputTokens)
            .build();
    }

    private AgentChatResponse executeSingle(SubTask subTask, Map<String, SubAgent> agents, Map<String, String> results) {
        SubAgent agent = agents.get(subTask.agentRole());
        if (agent == null) {
            return AgentChatResponse.builder()
                .reply("[No agent available for role: " + subTask.agentRole() + "]")
                .build();
        }
        String inputContext = buildInputContext(subTask, results);
        return agent.execute(subTask, inputContext);
    }

    private String buildInputContext(SubTask subTask, Map<String, String> results) {
        if (subTask.dependsOn().isEmpty()) {
            return subTask.inputContext();
        }
        StringBuilder sb = new StringBuilder();
        for (String depId : subTask.dependsOn()) {
            String depResult = results.get(depId);
            if (depResult != null) {
                sb.append("[").append(depId).append("]: ").append(depResult).append("\n\n");
            }
        }
        if (subTask.inputContext() != null && !subTask.inputContext().isBlank()) {
            sb.append(subTask.inputContext());
        }
        return sb.toString();
    }

    private String aggregateResults(AgentPlan plan, Map<String, String> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Plan: ").append(plan.summary()).append("\n\n");
        for (SubTask task : plan.subTasks()) {
            String result = results.getOrDefault(task.id(), "[No result]");
            sb.append("### ").append(task.id()).append(": ").append(task.description()).append("\n");
            sb.append("**Role:** ").append(task.agentRole()).append("\n\n");
            sb.append(result).append("\n\n");
        }
        return sb.toString();
    }

    private static int nullSafeTokens(Integer value) {
        return value != null ? value : 0;
    }
}
