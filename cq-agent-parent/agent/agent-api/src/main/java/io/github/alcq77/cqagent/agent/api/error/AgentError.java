package io.github.alcq77.cqagent.agent.api.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 标准错误对象，可用于 HTTP 输出与日志审计。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentError {

    private AgentErrorCode code;
    private boolean retryable;
    private String message;
    private String userMessage;
    private String traceId;

    @Builder.Default
    private Map<String, Object> details = new LinkedHashMap<>();
}
