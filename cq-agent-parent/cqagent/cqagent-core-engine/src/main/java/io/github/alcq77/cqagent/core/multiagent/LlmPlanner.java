package io.github.alcq77.cqagent.core.multiagent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * LLM-based planner that uses a chat model to decompose tasks into sub-tasks.
 *
 * <p>The planner sends a structured prompt to the LLM asking it to output
 * a JSON object of sub-tasks, each with id, description, role, and dependencies.</p>
 *
 * <p>System prompt and user prompt template are configurable via constructor
 * for internationalization and customization.</p>
 */
public class LlmPlanner implements Planner {

    private static final Logger log = LoggerFactory.getLogger(LlmPlanner.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    static final String DEFAULT_SYSTEM_PROMPT = """
        You are a task planner. Your job is to decompose complex tasks into smaller sub-tasks
        that can be assigned to specialized agents. Always respond with valid JSON only.
        """;

    static final String DEFAULT_USER_PROMPT_TEMPLATE = """
        Decompose the following task into sub-tasks.

        Available agent roles: %s

        Task: %s

        Respond with a JSON object with two fields:
          - "summary": a brief summary of the overall plan
          - "subTasks": an array of objects, each with:
            - "id": unique string identifier
            - "description": what this sub-task should accomplish
            - "agentRole": one of the available roles
            - "dependsOn": array of sub-task ids this depends on (empty if none)

        Output ONLY valid JSON, no explanation.
        """;

    private final ChatLanguageModel model;
    private final String systemPrompt;
    private final String userPromptTemplate;

    public LlmPlanner(ChatLanguageModel model) {
        this(model, DEFAULT_SYSTEM_PROMPT, DEFAULT_USER_PROMPT_TEMPLATE);
    }

    public LlmPlanner(ChatLanguageModel model, String systemPrompt, String userPromptTemplate) {
        if (model == null) {
            throw new IllegalArgumentException("model must not be null");
        }
        this.model = model;
        this.systemPrompt = systemPrompt != null ? systemPrompt : DEFAULT_SYSTEM_PROMPT;
        this.userPromptTemplate = userPromptTemplate != null ? userPromptTemplate : DEFAULT_USER_PROMPT_TEMPLATE;
    }

    @Override
    public AgentPlan plan(String userMessage, Set<String> availableRoles) {
        String roles = String.join(", ", availableRoles);
        String prompt = String.format(userPromptTemplate, roles, userMessage);
        SystemMessage sys = SystemMessage.from(systemPrompt);
        UserMessage user = UserMessage.from(prompt);
        Response<AiMessage> response = model.generate(List.of(sys, user));
        String json = response.content().text();
        return parsePlan(json, userMessage);
    }

    private AgentPlan parsePlan(String json, String originalMessage) {
        try {
            String cleaned = json.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```(?:json)?\\s*", "").replaceAll("```\\s*$", "");
            }
            JsonNode root = JSON.readTree(cleaned);
            String summary = root.has("summary") ? root.get("summary").asText() : "Multi-agent plan";
            List<SubTask> tasks = new ArrayList<>();
            JsonNode tasksNode = root.get("subTasks");
            if (tasksNode != null && tasksNode.isArray()) {
                for (JsonNode taskNode : tasksNode) {
                    String id = taskNode.has("id") ? taskNode.get("id").asText() : "task-" + tasks.size();
                    String desc = taskNode.has("description") ? taskNode.get("description").asText() : "";
                    String role = taskNode.has("agentRole") ? taskNode.get("agentRole").asText() : "default";
                    List<String> deps = new ArrayList<>();
                    JsonNode depsNode = taskNode.get("dependsOn");
                    if (depsNode != null && depsNode.isArray()) {
                        for (JsonNode dep : depsNode) {
                            deps.add(dep.asText());
                        }
                    }
                    tasks.add(new SubTask(id, desc, role, null, deps, new LinkedHashMap<>()));
                }
            }
            if (tasks.isEmpty()) {
                tasks.add(SubTask.of("task-0", originalMessage, "default"));
            }
            return new AgentPlan(summary, tasks, new LinkedHashMap<>());
        } catch (Exception e) {
            log.warn("Failed to parse planner JSON, falling back to single task: {}", e.getMessage());
            List<SubTask> fallback = List.of(SubTask.of("task-0", originalMessage, "default"));
            return new AgentPlan("Single-task fallback (planner parse failed)", fallback, new LinkedHashMap<>());
        }
    }
}
