package io.github.alcq77.cqagent.sdk;

import io.github.alcq77.cqagent.agent.api.dto.AgentChatResponse;

/**
 * Agent 流式输出监听器。
 */
public interface AgentStreamingListener {

    /**
     * 模型增量 token 回调。
     */
    void onToken(String token);

    /**
     * 流式完成回调（含最终聚合响应）。
     */
    void onComplete(AgentChatResponse response);

    /**
     * 流式异常回调。
     */
    void onError(Throwable throwable);
}
