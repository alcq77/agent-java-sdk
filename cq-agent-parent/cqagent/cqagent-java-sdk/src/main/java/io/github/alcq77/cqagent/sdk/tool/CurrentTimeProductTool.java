package io.github.alcq77.cqagent.sdk.tool;

import io.github.alcq77.cqagent.spi.tool.ProductTool;
import io.github.alcq77.cqagent.spi.tool.ProductToolParameterSpec;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 默认时间工具：返回上海时区的 ISO-8601 时间。
 */
public class CurrentTimeProductTool implements ProductTool {

    @Override
    public String name() {
        return "current_time";
    }

    @Override
    public boolean supports(String userInput) {
        if (userInput == null) {
            return false;
        }
        String text = userInput.toLowerCase();
        return text.contains("时间") || text.contains("date") || text.contains("time");
    }

    @Override
    public String execute(String userInput) {
        return OffsetDateTime.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @Override
    public Map<String, ProductToolParameterSpec> parameterSpecs() {
        return Map.of();
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        return execute("");
    }
}
