package io.github.alcq77.cqagent.core.multiagent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import io.github.alcq77.cqagent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqagent.agent.api.dto.AgentChatResponse;
import io.github.alcq77.cqagent.core.agent.LangChain4jProductAgentRuntime;
import io.github.alcq77.cqagent.core.observability.AgentRuntimeCounters;
import io.github.alcq77.cqagent.spi.session.ProductSessionStore;
import io.github.alcq77.cqagent.spi.tool.ProductTool;

import java.util.List;
import java.util.Map;

/**
 * A specialized agent that executes a single sub-task within a multi-agent plan.
 *
 * <p>Each SubAgent has its own role, system prompt, model, and optional tools.
 * It delegates to the existing {@link LangChain4jProductAgentRuntime} for
 * tool-calling and session management.</p>
 */
public class SubAgent {

    private final String role;
    private final String systemPrompt;
    private final ChatLanguageModel model;
    private final LangChain4jProductAgentRuntime runtime;

    /**
     * @param role         unique role identifier (e.g. "researcher", "coder", "reviewer")
     * @param systemPrompt system prompt that defines this agent's behavior
     * @param model        the LangChain4j chat model to use
     * @param tools        tools available to this agent (may be empty)
     * @param sessionStore session store for conversation memory
     */
    public SubAgent(String role,
                    String systemPrompt,
                    ChatLanguageModel model,
                    List<ProductTool> tools,
                    ProductSessionStore sessionStore) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("SubAgent.role must not be blank");
        }
        this.role = role;
        this.systemPrompt = systemPrompt;
        this.model = model;
        this.runtime = new LangChain4jProductAgentRuntime(
            sessionStore,
            tools != null ? tools : List.of(),
            5, // max tool iterations per sub-agent
            Map.of(),
            null,
            false,
            new AgentRuntimeCounters()
        );
    }

    /**
     * Executes a sub-task and returns the response.
     *
     * @param subTask the sub-task to execute
     * @param inputContext additional context (e.g. results from dependent sub-tasks)
     * @return the agent's response
     */
    public AgentChatResponse execute(SubTask subTask, String inputContext) {
        String message = buildMessage(subTask, inputContext);
        AgentChatRequest request = AgentChatRequest.builder()
            .systemPrompt(systemPrompt)
            .message(message)
            .build();
        return runtime.chat(request, model, role);
    }

    private String buildMessage(SubTask subTask, String inputContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("Task: ").append(subTask.description());
        if (subTask.inputContext() != null && !subTask.inputContext().isBlank()) {
            sb.append("\n\nContext: ").append(subTask.inputContext());
        }
        if (inputContext != null && !inputContext.isBlank()) {
            sb.append("\n\nPrevious results:\n").append(inputContext);
        }
        return sb.toString();
    }

    public String role() {
        return role;
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public ChatLanguageModel model() {
        return model;
    }
}