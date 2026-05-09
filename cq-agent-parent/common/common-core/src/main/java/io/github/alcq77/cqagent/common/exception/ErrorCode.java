package io.github.alcq77.cqagent.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 平台统一错误码（可与架构文档附录对齐并扩展）。
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "OK", HttpStatus.OK),

    BAD_REQUEST(40000, "请求参数错误", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(40100, "未授权", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40300, "禁止访问", HttpStatus.FORBIDDEN),
    NOT_FOUND(40400, "资源不存在", HttpStatus.NOT_FOUND),
    CONFLICT(40900, "资源冲突", HttpStatus.CONFLICT),

    /** 模型调用失败（上游错误或网络异常） */
    MODEL_INVOKE_FAILED(20001, "模型调用失败", HttpStatus.BAD_GATEWAY),
    /** 未配置路由或厂商端点 */
    MODEL_NOT_CONFIGURED(20002, "模型或厂商端点未配置", HttpStatus.BAD_REQUEST),

    /** 依赖服务未启用或不可用（如 LangChain4j 模型网关未打开） */
    SERVICE_UNAVAILABLE(50300, "服务暂不可用", HttpStatus.SERVICE_UNAVAILABLE),

    INTERNAL_ERROR(50000, "系统内部错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }
}
