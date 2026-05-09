package io.github.alcq77.cqagent.core.multiagent;

import io.github.alcq77.cqagent.agent.api.dto.AgentChatResponse;

import java.util.List;
import java.util.Map;

/**
 * Routes sub-tasks to sub-agents and aggregates results.
 *
 * <p>The coordinator is responsible for:
 * <ul>
 *   <li>Executing sub-tasks in dependency order (levels)</li>
 *   <li>Passing results from dependent tasks as context</li>
 *   <li>Aggregating final results into a coherent response</li>
 * </ul>
 */
public interface Coordinator {

    /**
     * Executes a plan by running sub-tasks in order and aggregating results.
     *
     * @param plan the execution plan
     * @param agents map of role -> SubAgent
     * @return the aggregated response
     */
    AgentChatResponse execute(AgentPlan plan, Map<String, SubAgent> agents);
}