package io.github.alcq77.cqagent.core.runtime.advisor;

import io.github.alcq77.cqagent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqagent.agent.api.dto.AgentChatResponse;

/**
 * 运行时增强拦截器：用于注入、过滤、审计等统一增强链。
 */
public interface AgentRuntimeAdvisor {

    /**
     * 顺序值，越小越先执行。
     */
    default int order() {
        return 0;
    }

    /**
     * 是否启用该增强器。
     */
    default boolean enabled() {
        return true;
    }

    /**
     * 调用前处理。
     */
    default AgentChatRequest before(AgentChatRequest request) {
        return request;
    }

    /**
     * 调用成功后处理。
     */
    default AgentChatResponse after(AgentChatRequest request, AgentChatResponse response) {
        return response;
    }

    /**
     * 调用失败后处理，可替换异常。
     */
    default RuntimeException onError(AgentChatRequest request, RuntimeException error) {
        return error;
    }
}
