package io.github.alcq77.cqagent.sdk;

import io.github.alcq77.cqagent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqagent.agent.api.dto.AgentChatResponse;

/**
 * SDK 对外最小调用接口。
 * <p>
 * 同步 {@link #chat} 与流式 {@link #stream} 在嵌入式实现 {@code EmbeddedAgentClient} 中均已接入 LangChain4j；
 * 若业务自行实现本接口且未覆盖 {@link #stream}，默认实现会抛出 {@link UnsupportedOperationException}。
 */
public interface AgentClient {

    /**
     * 同步问答：阻塞直至模型返回完整回复（含内部工具多轮与会话落盘）。
     *
     * @param request 会话与提示词上下文；不可为 {@code null}
     * @return 聚合后的完整回复与用量统计
     */
    AgentChatResponse chat(AgentChatRequest request);

    /**
     * 流式对话：通过 {@link AgentStreamingListener} 逐 token 回调；工具多轮时在底层运行时内展开，
     * 仅在最终文本确定后触发 {@link AgentStreamingListener#onComplete}。
     * <p>
     * 注意：需要对应 {@link io.github.alcq77.cqagent.spi.model.ProductModelProvider}
     * 声明支持 streaming 并成功构建 {@link dev.langchain4j.model.chat.StreamingChatLanguageModel}。
     *
     * @param request 同 {@link #chat}
     * @param listener  token / 完成 / 错误回调；不可为 {@code null}
     */
    default void stream(AgentChatRequest request, AgentStreamingListener listener) {
        throw new UnsupportedOperationException("stream is not supported by " + getClass().getName());
    }
}
