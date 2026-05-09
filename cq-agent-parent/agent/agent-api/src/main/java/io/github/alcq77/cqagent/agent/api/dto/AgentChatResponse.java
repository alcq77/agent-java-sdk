package io.github.alcq77.cqagent.agent.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 对话响应（同步）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentChatResponse {

    private String sessionId;

    private String reply;

    private Integer inputTokens;

    private Integer outputTokens;

    private Integer totalTokens;

    /**
     * 请求追踪 ID；用于日志与问题排查串联。
     */
    private String traceId;
}
