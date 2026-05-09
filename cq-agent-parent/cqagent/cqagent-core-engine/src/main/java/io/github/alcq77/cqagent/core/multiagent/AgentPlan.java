package io.github.alcq77.cqagent.core.multiagent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The decomposition of a user task into ordered sub-tasks.
 *
 * <p>An {@code AgentPlan} is produced by the {@link Planner} and consumed
 * by the {@link Coordinator}. It contains the sub-tasks, their execution
 * order (respecting dependencies), and a summary of the overall plan.</p>
 */
public record AgentPlan(
    String summary,
    List<SubTask> subTasks,
    Map<String, String> metadata
) {

    public AgentPlan {
        subTasks = subTasks == null ? List.of() : List.copyOf(subTasks);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Returns sub-tasks grouped by execution level (respecting dependencies).
     * Level 0 has no dependencies, level 1 depends on level 0, etc.
     */
    public List<List<SubTask>> executionLevels() {
        Map<String, Integer> levels = new LinkedHashMap<>();
        for (SubTask task : subTasks) {
            int maxDepLevel = -1;
            for (String depId : task.dependsOn()) {
                Integer depLevel = levels.get(depId);
                if (depLevel != null) {
                    maxDepLevel = Math.max(maxDepLevel, depLevel);
                }
            }
            levels.put(task.id(), maxDepLevel + 1);
        }
        int maxLevel = levels.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<List<SubTask>> result = new ArrayList<>();
        for (int i = 0; i <= maxLevel; i++) {
            List<SubTask> level = new ArrayList<>();
            for (SubTask task : subTasks) {
                if (levels.getOrDefault(task.id(), 0) == i) {
                    level.add(task);
                }
            }
            if (!level.isEmpty()) {
                result.add(level);
            }
        }
        return result;
    }

    /**
     * Returns the total number of sub-tasks.
     */
    public int size() {
        return subTasks.size();
    }

    /**
     * Returns true if there are no sub-tasks.
     */
    public boolean isEmpty() {
        return subTasks.isEmpty();
    }

    public static AgentPlan of(String summary, List<SubTask> subTasks) {
        return new AgentPlan(summary, subTasks, Map.of());
    }
}