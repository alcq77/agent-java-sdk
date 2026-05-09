package io.github.alcq77.cqagent.core.multiagent;

import java.util.List;
import java.util.Map;

/**
 * A single sub-task within a multi-agent plan.
 *
 * <p>Each sub-task has an id, description, assigned agent role,
 * input context, and dependency on other sub-tasks.</p>
 */
public record SubTask(
    String id,
    String description,
    String agentRole,
    String inputContext,
    List<String> dependsOn,
    Map<String, String> metadata
) {

    public SubTask {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("SubTask.id must not be blank");
        }
        dependsOn = dependsOn == null ? List.of() : List.copyOf(dependsOn);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static SubTask of(String id, String description, String agentRole) {
        return new SubTask(id, description, agentRole, null, List.of(), Map.of());
    }

    public static SubTask of(String id, String description, String agentRole, List<String> dependsOn) {
        return new SubTask(id, description, agentRole, null, dependsOn, Map.of());
    }
}