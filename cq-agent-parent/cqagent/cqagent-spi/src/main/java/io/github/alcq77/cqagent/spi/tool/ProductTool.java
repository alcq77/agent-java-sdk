package io.github.alcq77.cqagent.spi.tool;

import java.util.Map;

/**
 * 工具 SPI 契约。
 * <p>
 * 职责边界：
 * - 向运行时暴露工具名称、描述、参数 schema 与执行入口；
 * - 兼容旧字符串入参与新结构化入参两种模式。
 */
public interface ProductTool {

    /**
     * 工具唯一标识。
     */
    String name();

    /**
     * 工具是否适配某段输入（兼容旧接口）。
     */
    boolean supports(String userInput);

    /**
     * 旧执行入口（字符串入参）。
     */
    String execute(String userInput);

    /**
     * 工具描述（用于模型选择工具）。
     */
    default String description() {
        return "cqagent product tool: " + name();
    }

    /**
     * 结构化参数 schema（key=参数名）。
     * <p>
     * 默认保持旧协议：仅 input 字符串参数。
     */
    default Map<String, ProductToolParameterSpec> parameterSpecs() {
        return Map.of("input", ProductToolParameterSpec.string(true, "plain text tool input"));
    }

    /**
     * 新执行入口（结构化参数）。
     * <p>
     * 默认回退到旧 execute(String) 保持兼容。
     */
    default String execute(Map<String, Object> arguments) {
        Object value = arguments == null ? null : arguments.get("input");
        return execute(value == null ? "" : String.valueOf(value));
    }
}
