package io.github.alcq77.cqagent.core.runtime.advisor;

import io.github.alcq77.cqagent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqagent.core.rag.RagChunk;
import io.github.alcq77.cqagent.core.rag.RagRetrievalFilter;
import io.github.alcq77.cqagent.core.rag.RagRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 检索知识片段并注入到本轮用户消息。
 * <p>
 * 说明：
 * - 这是一个标准 {@link AgentRuntimeAdvisor}，可通过 order 控制执行先后；
 * - 只改写当前轮 request.message，不直接落库会话历史；
 * - 若未召回任何片段，保持原请求不变。
 */
public class RagContextAdvisor implements AgentRuntimeAdvisor {

    private static final Logger log = LoggerFactory.getLogger(RagContextAdvisor.class);

    /** Default prompt header in Chinese. */
    static final String DEFAULT_HEADER_CN = "以下是可参考的知识库片段：\n";
    /** Default user question prefix in Chinese. */
    static final String DEFAULT_USER_PREFIX_CN = "\n用户问题：";

    private final RagRetriever retriever;
    private final int topK;
    private final int order;
    private final RagRetrievalFilter filter;
    private final String headerText;
    private final String userPrefixText;

    public RagContextAdvisor(RagRetriever retriever, int topK, int order) {
        this(retriever, topK, order, null, DEFAULT_HEADER_CN, DEFAULT_USER_PREFIX_CN);
    }

    public RagContextAdvisor(RagRetriever retriever, int topK, int order, RagRetrievalFilter filter) {
        this(retriever, topK, order, filter, DEFAULT_HEADER_CN, DEFAULT_USER_PREFIX_CN);
    }

    public RagContextAdvisor(RagRetriever retriever, int topK, int order, RagRetrievalFilter filter,
                            String headerText, String userPrefixText) {
        this.retriever = retriever;
        this.topK = Math.max(1, topK);
        this.order = order;
        this.filter = filter;
        this.headerText = headerText != null ? headerText : DEFAULT_HEADER_CN;
        this.userPrefixText = userPrefixText != null ? userPrefixText : DEFAULT_USER_PREFIX_CN;
    }

    @Override
    public int order() {
        return order;
    }

    @Override
    public AgentChatRequest before(AgentChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return request;
        }
        List<RagChunk> chunks;
        try {
            chunks = retriever.retrieve(request.getMessage(), topK, filter);
        } catch (Exception ex) {
            log.warn("RAG retrieval failed, proceeding without context: {}", ex.getMessage());
            return request;
        }
        if (chunks.isEmpty()) {
            return request;
        }
        StringBuilder context = new StringBuilder();
        context.append(headerText);
        for (int i = 0; i < chunks.size(); i++) {
            RagChunk chunk = chunks.get(i);
            context.append("[").append(i + 1).append("] ").append(chunk.text()).append("\n");
        }
        AgentChatRequest copied = AgentChatRequest.builder()
            .sessionId(request.getSessionId())
            .message(context + userPrefixText + request.getMessage())
            .systemPrompt(request.getSystemPrompt())
            .traceId(request.getTraceId())
            .promptTemplateId(request.getPromptTemplateId())
            .promptVariables(request.getPromptVariables() == null ? null : new LinkedHashMap<>(request.getPromptVariables()))
            .taskType(request.getTaskType())
            .tags(request.getTags() == null ? null : new ArrayList<>(request.getTags()))
            .build();
        return copied;
    }
}
