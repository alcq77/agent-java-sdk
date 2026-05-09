package io.github.alcq77.cqagent.spi.tool;

/**
 * ProductTool 参数定义。
 */
public record ProductToolParameterSpec(
    String type,
    boolean required,
    String description
) {

    /**
     * string 参数。
     */
    public static ProductToolParameterSpec string(boolean required, String description) {
        return new ProductToolParameterSpec("string", required, description);
    }

    /**
     * integer 参数。
     */
    public static ProductToolParameterSpec integer(boolean required, String description) {
        return new ProductToolParameterSpec("integer", required, description);
    }

    /**
     * number 参数。
     */
    public static ProductToolParameterSpec number(boolean required, String description) {
        return new ProductToolParameterSpec("number", required, description);
    }

    /**
     * boolean 参数。
     */
    public static ProductToolParameterSpec bool(boolean required, String description) {
        return new ProductToolParameterSpec("boolean", required, description);
    }
}
