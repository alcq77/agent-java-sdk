package io.github.alcq77.cqagent.starter;

import io.github.alcq77.cqagent.agent.api.error.AgentError;
import io.github.alcq77.cqagent.agent.api.error.AgentErrorCode;
import io.github.alcq77.cqagent.agent.api.error.AgentRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Starter 默认异常映射：输出统一 AgentError 结构。
 */
@RestControllerAdvice
public class ProductAgentExceptionHandler {

    @ExceptionHandler(AgentRuntimeException.class)
    public ResponseEntity<AgentError> handleAgentError(AgentRuntimeException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case ROUTE_NOT_FOUND, PROVIDER_NOT_FOUND -> HttpStatus.BAD_REQUEST;
            case TOOL_VALIDATION_ERROR -> HttpStatus.UNPROCESSABLE_ENTITY;
            case MODEL_INVOKE_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return ResponseEntity.status(status).body(ex.toAgentError());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<AgentError> handleUnknown(RuntimeException ex) {
        AgentError error = AgentError.builder()
            .code(AgentErrorCode.UNKNOWN_ERROR)
            .retryable(false)
            .message(ex.getMessage())
            .userMessage("系统内部错误，请稍后重试")
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
