package io.github.alcq77.cqagent.core.multiagent;

/**
 * Decomposes a complex user task into an ordered plan of sub-tasks.
 *
 * <p>The planner is responsible for:
 * <ul>
 *   <li>Analyzing the user's request</li>
 *   <li>Deciding which sub-tasks are needed</li>
 *   <li>Assigning roles (agent types) to each sub-task</li>
 *   <li>Defining dependencies between sub-tasks</li>
 * </ul>
 */
@FunctionalInterface
public interface Planner {

    /**
     * Creates an execution plan from a user message.
     *
     * @param userMessage the user's original request
     * @param availableRoles the roles available for assignment (e.g. "researcher", "coder")
     * @return an ordered plan of sub-tasks
     */
    AgentPlan plan(String userMessage, java.util.Set<String> availableRoles);
}