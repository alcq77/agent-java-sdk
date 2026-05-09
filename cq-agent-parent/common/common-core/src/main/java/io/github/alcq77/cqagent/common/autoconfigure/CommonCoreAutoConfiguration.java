package io.github.alcq77.cqagent.common.autoconfigure;

import io.github.alcq77.cqagent.common.web.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * 自动注册公共 Web 组件（如全局异常处理）。
 */
@AutoConfiguration
@Import(GlobalExceptionHandler.class)
public class CommonCoreAutoConfiguration {
}
