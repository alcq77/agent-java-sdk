package io.github.alcq77.cqagent.core.agent;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import io.github.alcq77.cqagent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqagent.agent.api.dto.AgentChatResponse;
import io.github.alcq77.cqagent.core.observability.AgentRuntimeCounters;
import io.github.alcq77.cqagent.core.session.ProductSessionChatMemory;
import io.github.alcq77.cqagent.core.tool.ProductToolRegistry;
import io.github.alcq77.cqagent.spi.session.ProductSessionStore;
import io.github.alcq77.cqagent.spi.tool.ProductTool;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 * 基于 LangChain4j 抽象的 cqagent 运行时门面。
 */
public class LangChain4jProductAgentRuntime {

    /**
     * 会话历史后端（内存/文件/redis/jdbc）。
     */
    private final ProductSessionStore sessionStore;
    /**
     * ProductTool -> ToolSpecification / ToolExecution 适配层。
     */
    private final ProductToolRegistry toolRegistry;
    /**
     * 工具回路最大轮次，防止模型与工具陷入无限循环。
     */
    private final int maxToolCallIterations;
    private final Map<String, PromptTemplate> promptTemplates;
    private final String defaultPromptTemplateId;
    private final boolean fallbackToDefaultPromptTemplate;
    private final Map<String, LongAdder> templateHits = new ConcurrentHashMap<>();
    private final LongAdder templateFallbackCount = new LongAdder();
    private final LongAdder templateMissingCount = new LongAdder();

    /**
     * 与工具层共用的运行时计数器（同步/流式调用在 SDK 外壳累加）。
     */
    private final AgentRuntimeCounters runtimeCounters;

    public LangChain4jProductAgentRuntime(ProductSessionStore sessionStore,
                                          List<ProductTool> tools,
                                          int maxToolCallIterations,
                                          Map<String, PromptTemplate> promptTemplates,
                                          String defaultPromptTemplateId,
                                          boolean fallbackToDefaultPromptTemplate,
                                          AgentRuntimeCounters runtimeCounters) {
        this.sessionStore = sessionStore;
        this.runtimeCounters = Objects.requireNonNull(runtimeCounters, "runtimeCounters");
        this.toolRegistry = new ProductToolRegistry(tools, runtimeCounters);
        this.maxToolCallIterations = Math.max(1, maxToolCallIterations);
        this.promptTemplates = promptTemplates == null ? Map.of() : promptTemplates;
        this.defaultPromptTemplateId = defaultPromptTemplateId;
        this.fallbackToDefaultPromptTemplate = fallbackToDefaultPromptTemplate;
    }

    public AgentRuntimeCounters runtimeCounters() {
        return runtimeCounters;
    }

    /**
     * 同步对话主链：反复 generate，直到模型不再请求工具或达到 {@link #maxToolCallIterations}。
     *
     * @param logicalModel 逻辑模型名（审计/路由语义）；当前实现主要透传到响应侧
     */
    public AgentChatResponse chat(AgentChatRequest request, ChatLanguageModel model, String logicalModel) {
        SessionContext ctx = prepareSessionContext(request);
        ProductSessionChatMemory memory = ctx.memory();

        TokenUsage totalUsage = new TokenUsage();
        AiMessage finalAiMessage = null;
        for (int i = 0; i < maxToolCallIterations; i++) {
            // 由 LangChain4j 在一次 generate 内携带 messages；有工具时附带 ToolSpecification 列表。
            Response<AiMessage> response = toolRegistry.isEmpty()
                    ? model.generate(memory.messages())
                    : model.generate(memory.messages(), toolRegistry.specifications());
            if (response.tokenUsage() != null) {
                totalUsage = totalUsage.add(response.tokenUsage());
            }
            AiMessage aiMessage = response.content();
            memory.add(aiMessage);
            if (!aiMessage.hasToolExecutionRequests()) {
                // 本轮已是最终用户可见文本，退出工具回路。
                finalAiMessage = aiMessage;
                break;
            }
            // 工具回合：执行结果作为 ToolExecutionResultMessage 追加，下一轮 generate 继续推理。
            for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                String toolResult = toolRegistry.execute(toolRequest);
                memory.add(ToolExecutionResultMessage.from(toolRequest, toolResult));
            }
        }
        if (finalAiMessage == null) {
            throw new IllegalStateException("tool loop exceeded max iterations: " + maxToolCallIterations);
        }
        // 请求结束后一次性持久化会话消息
        memory.syncToStore();
        return AgentChatResponse.builder()
            .sessionId(ctx.sessionId())
                .reply(finalAiMessage.text())
                .inputTokens(totalUsage.inputTokenCount())
                .outputTokens(totalUsage.outputTokenCount())
                .totalTokens(totalUsage.totalTokenCount())
                .traceId(request.getTraceId())
                .build();
    }

    /**
     * 流式对话主链（支持工具多轮：工具执行后继续调用模型直至产出最终文本）。
     * <p>
     * 完成后持久化会话；达到 {@link #maxToolCallIterations} 仍未结束时通过 onError 上报。
     */
    public void stream(AgentChatRequest request,
                       StreamingChatLanguageModel model,
                       String logicalModel,
                       Consumer<String> onToken,
                       Consumer<AgentChatResponse> onComplete,
                       Consumer<Throwable> onError) {
        SessionContext ctx = prepareSessionContext(request);
        ProductSessionChatMemory memory = ctx.memory();
        TokenUsage totalUsage = new TokenUsage();
        streamRound(request, model, memory, ctx.sessionId(), 0, totalUsage, onToken, onComplete, onError);
    }

    /**
     * 流式轮次执行：
     * 1) 调用模型获取本轮输出；
     * 2) 若返回 tool requests，则执行工具并继续下一轮；
     * 3) 若返回最终文本，则结束并持久化会话。
     */
    private void streamRound(AgentChatRequest request,
                             StreamingChatLanguageModel model,
                             ProductSessionChatMemory memory,
                             String sessionId,
                             int round,
                             TokenUsage totalUsage,
                             Consumer<String> onToken,
                             Consumer<AgentChatResponse> onComplete,
                             Consumer<Throwable> onError) {
        if (round >= maxToolCallIterations) {
            onError.accept(new IllegalStateException("streaming tool loop exceeded max iterations: " + maxToolCallIterations));
            return;
        }
        if (toolRegistry.isEmpty()) {
            model.generate(memory.messages(), streamingHandler(request, model, memory, sessionId, round, totalUsage,
                onToken, onComplete, onError));
            return;
        }
        model.generate(memory.messages(), toolRegistry.specifications(), streamingHandler(
            request, model, memory, sessionId, round, totalUsage, onToken, onComplete, onError
        ));
    }

    private StreamingResponseHandler<AiMessage> streamingHandler(AgentChatRequest request,
                                                                 StreamingChatLanguageModel model,
                                                                 ProductSessionChatMemory memory,
                                                                 String sessionId,
                                                                 int round,
                                                                 TokenUsage totalUsage,
                                                                 Consumer<String> onToken,
                                                                 Consumer<AgentChatResponse> onComplete,
                                                                 Consumer<Throwable> onError) {
        return new StreamingResponseHandler<>() {
            @Override
            public void onNext(String token) {
                onToken.accept(token);
            }

            @Override
            public void onComplete(Response<AiMessage> response) {
                TokenUsage usage = response.tokenUsage();
                TokenUsage mergedUsage = usage == null ? totalUsage : totalUsage.add(usage);
                AiMessage aiMessage = response.content();
                if (aiMessage == null) {
                    onError.accept(new IllegalStateException("streaming response is empty"));
                    return;
                }
                memory.add(aiMessage);
                if (aiMessage.hasToolExecutionRequests()) {
                    try {
                        // 流式场景下工具分支不产生面向用户的 token 流，仅在下一轮 generate 继续输出。
                        for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                            String toolResult = toolRegistry.execute(toolRequest);
                            memory.add(ToolExecutionResultMessage.from(toolRequest, toolResult));
                        }
                        streamRound(request, model, memory, sessionId, round + 1, mergedUsage, onToken, onComplete, onError);
                    } catch (Throwable toolError) {
                        onError.accept(toolError);
                    }
                    return;
                }
                if (aiMessage.text() == null) {
                    onError.accept(new IllegalStateException("streaming final response text is empty"));
                    return;
                }
                memory.syncToStore();
                onComplete.accept(AgentChatResponse.builder()
                    .sessionId(sessionId)
                    .reply(aiMessage.text())
                    .inputTokens(mergedUsage.inputTokenCount())
                    .outputTokens(mergedUsage.outputTokenCount())
                    .totalTokens(mergedUsage.totalTokenCount())
                    .traceId(request.getTraceId())
                    .build());
            }

            @Override
            public void onError(Throwable throwable) {
                onError.accept(throwable);
            }
        };
    }

    /**
     * 组装本会话轮次的 ChatMemory：解析模板、渲染占位符、注入 system/user 消息。
     */
    private SessionContext prepareSessionContext(AgentChatRequest request) {
        String sessionId = resolveSessionId(request.getSessionId());
        PromptTemplate selectedTemplate = resolveTemplate(request.getPromptTemplateId());
        String systemPromptSource = firstNonBlank(request.getSystemPrompt(),
            selectedTemplate == null ? null : selectedTemplate.systemPrompt());
        String userPromptSource = firstNonBlank(request.getMessage(),
            selectedTemplate == null ? null : selectedTemplate.userMessage());
        String renderedSystemPrompt = renderTemplate(systemPromptSource, request.getPromptVariables());
        String renderedMessage = renderTemplate(userPromptSource, request.getPromptVariables());
        if (renderedMessage == null || renderedMessage.isBlank()) {
            throw new IllegalStateException("rendered user message is empty");
        }

        ProductSessionChatMemory memory = new ProductSessionChatMemory(sessionId, sessionStore);
        if (renderedSystemPrompt != null && !renderedSystemPrompt.isBlank()) {
            memory.add(SystemMessage.from(renderedSystemPrompt.trim()));
        }
        memory.add(UserMessage.from(renderedMessage));
        return new SessionContext(sessionId, memory);
    }

    private String resolveSessionId(String requested) {
        if (requested != null && !requested.isBlank()) {
            String sid = requested.trim();
            if (!sessionStore.hasSession(sid)) {
                sessionStore.register(sid);
            }
            return sid;
        }
        String sid = UUID.randomUUID().toString();
        sessionStore.register(sid);
        return sid;
    }

    private static String renderTemplate(String template, Map<String, String> variables) {
        if (template == null || template.isBlank() || variables == null || variables.isEmpty()) {
            return template;
        }
        // Single-pass replacement via regex to prevent template injection:
        // If a variable value contains {{otherKey}}, it will NOT be replaced
        // because we replace all placeholders in one pass.
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{\\{(\\w+)}}");
        java.util.regex.Matcher matcher = pattern.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = variables.getOrDefault(key, matcher.group());
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private PromptTemplate resolveTemplate(String requestTemplateId) {
        if (requestTemplateId == null || requestTemplateId.isBlank()) {
            PromptTemplate template = resolveDefaultTemplateOrNull();
            if (template != null) {
                markTemplateHit(defaultPromptTemplateId, false);
            }
            return template;
        }
        String requestId = requestTemplateId.trim();
        PromptTemplate template = promptTemplates.get(requestId);
        if (template != null) {
            markTemplateHit(requestId, false);
            return template;
        }
        templateMissingCount.increment();
        if (fallbackToDefaultPromptTemplate) {
            PromptTemplate fallback = resolveDefaultTemplateOrNull();
            if (fallback != null) {
                markTemplateHit(defaultPromptTemplateId, true);
            }
            return fallback;
        }
        throw new IllegalStateException("prompt template not found: " + requestId);
    }

    private PromptTemplate resolveDefaultTemplateOrNull() {
        if (defaultPromptTemplateId == null || defaultPromptTemplateId.isBlank()) {
            return null;
        }
        PromptTemplate template = promptTemplates.get(defaultPromptTemplateId.trim());
        if (template == null) {
            throw new IllegalStateException("default prompt template not found: " + defaultPromptTemplateId);
        }
        return template;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    public Map<String, Object> promptTemplateMetrics() {
        Map<String, Long> hitByTemplate = new LinkedHashMap<>();
        templateHits.forEach((k, v) -> hitByTemplate.put(k, v.longValue()));
        return Map.of(
                "templateHits", hitByTemplate,
                "templateFallbackCount", templateFallbackCount.longValue(),
                "templateMissingCount", templateMissingCount.longValue()
        );
    }

    private void markTemplateHit(String templateId, boolean fallback) {
        if (templateId != null && !templateId.isBlank()) {
            templateHits.computeIfAbsent(templateId.trim(), k -> new LongAdder()).increment();
        }
        if (fallback) {
            templateFallbackCount.increment();
        }
    }

    public record PromptTemplate(String systemPrompt, String userMessage) {
    }

    private record SessionContext(String sessionId, ProductSessionChatMemory memory) {
    }
}
