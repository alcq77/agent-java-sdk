package io.github.alcq77.cqagent.agent.api.error;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一运行时异常载体，贯穿 SDK / Starter。
 */
public class AgentRuntimeException extends RuntimeException {

    private final AgentErrorCode code;
    private final boolean retryable;
    private final String userMessage;
    private final String traceId;
    private final Map<String, Object> details;

    public AgentRuntimeException(AgentErrorCode code,
                                 boolean retryable,
                                 String message,
                                 String userMessage,
                                 String traceId,
                                 Throwable cause) {
        this(code, retryable, message, userMessage, traceId, Map.of(), cause);
    }

    public AgentRuntimeException(AgentErrorCode code,
                                 boolean retryable,
                                 String message,
                                 String userMessage,
                                 String traceId,
                                 Map<String, Object> details,
                                 Throwable cause) {
        super(message, cause);
        this.code = code == null ? AgentErrorCode.UNKNOWN_ERROR : code;
        this.retryable = retryable;
        this.userMessage = userMessage == null ? "请求处理失败，请稍后重试" : userMessage;
        this.traceId = traceId;
        this.details = details == null ? Map.of() : new LinkedHashMap<>(details);
    }

    public AgentErrorCode getCode() {
        return code;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getTraceId() {
        return traceId;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public AgentError toAgentError() {
        return AgentError.builder()
            .code(code)
            .retryable(retryable)
            .message(getMessage())
            .userMessage(userMessage)
            .traceId(traceId)
            .details(details)
            .build();
    }
}
