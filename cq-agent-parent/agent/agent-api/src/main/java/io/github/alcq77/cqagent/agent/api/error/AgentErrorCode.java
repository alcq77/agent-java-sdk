package io.github.alcq77.cqagent.agent.api.error;

/**
 * 统一错误码定义。
 */
public enum AgentErrorCode {
    ROUTE_NOT_FOUND,
    PROVIDER_NOT_FOUND,
    MODEL_INVOKE_TIMEOUT,
    MODEL_UPSTREAM_ERROR,
    TOOL_VALIDATION_ERROR,
    TOOL_EXECUTION_ERROR,
    ADVISOR_REJECTED,
    UNKNOWN_ERROR
}
