package io.github.alcq77.cqagent.common.web;

import io.github.alcq77.cqagent.common.api.ApiResponse;
import io.github.alcq77.cqagent.common.exception.BusinessException;
import io.github.alcq77.cqagent.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：业务异常返回统一 JSON，未预期异常记录 ERROR 日志。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        if (ex.getHttpStatus().is5xxServerError()) {
            log.error("业务异常(5xx), code={}, message={}, traceId={}", ex.getCode(), ex.getMessage(), traceId, ex);
        } else if (ex.getHttpStatus() == HttpStatus.NOT_FOUND) {
            log.warn("业务异常(4xx), code={}, message={}, traceId={}", ex.getCode(), ex.getMessage(), traceId);
        } else {
            log.warn("业务异常, code={}, message={}, traceId={}", ex.getCode(), ex.getMessage(), traceId);
        }
        ApiResponse<Void> body = ApiResponse.fail(ex.getCode(), ex.getMessage(), traceId);
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidation(Exception ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn("参数校验失败, traceId={}, error={}", traceId, ex.getMessage());
        ApiResponse<Void> body = ApiResponse.fail(ErrorCode.BAD_REQUEST.getCode(), ErrorCode.BAD_REQUEST.getDefaultMessage(),
                traceId);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.error("未处理异常, traceId={}", traceId, ex);
        ApiResponse<Void> body = ApiResponse.fail(ErrorCode.INTERNAL_ERROR.getCode(),
                ErrorCode.INTERNAL_ERROR.getDefaultMessage(), traceId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private static String resolveTraceId(HttpServletRequest request) {
        String header = request.getHeader("X-Trace-Id");
        if (header != null && !header.isBlank()) {
            return header;
        }
        return request.getHeader("traceId");
    }
}
