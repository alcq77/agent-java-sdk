package io.github.alcq77.cqagent.core.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import io.github.alcq77.cqagent.core.observability.AgentRuntimeCounters;
import io.github.alcq77.cqagent.spi.tool.ProductTool;
import io.github.alcq77.cqagent.spi.tool.ProductToolParameterSpec;

import java.util.*;

/**
 * ProductTool 到 LangChain4j ToolSpecification 的适配器。
 */
public class ProductToolRegistry {

    public static final String PREFIX_VALIDATION = "cqagent.tool.validation: ";
    public static final String PREFIX_EXECUTION = "cqagent.tool.execution: ";

    /**
     * 运行时工具索引（key=toolName）。
     */
    private final Map<String, ProductTool> toolMap = new LinkedHashMap<>();
    /**
     * 预构建的 LangChain4j 工具描述，避免每轮重复创建。
     */
    private final List<ToolSpecification> specifications;
    /**
     * JSON 参数解析器。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 可选观测计数器；为空时不记录。
     */
    private final AgentRuntimeCounters counters;

    public ProductToolRegistry(List<ProductTool> tools) {
        this(tools, null);
    }

    public ProductToolRegistry(List<ProductTool> tools, AgentRuntimeCounters counters) {
        this.counters = counters;
        if (tools != null) {
            for (ProductTool tool : tools) {
                if (tool != null && tool.name() != null && !tool.name().isBlank()) {
                    toolMap.putIfAbsent(tool.name(), tool);
                }
            }
        }
        this.specifications = toolMap.values().stream()
                .map(ProductToolRegistry::toSpecification)
                .toList();
    }

    public List<ToolSpecification> specifications() {
        return specifications;
    }

    public boolean isEmpty() {
        return toolMap.isEmpty();
    }

    public String execute(ToolExecutionRequest request) {
        ProductTool tool = toolMap.get(request.name());
        if (tool == null) {
            if (counters != null) {
                counters.incrementToolValidationFailure();
            }
            throw validationException("tool not found: " + request.name());
        }
        Map<String, Object> arguments;
        try {
            arguments = parseArguments(request.arguments());
            validateArguments(tool, arguments);
        } catch (RuntimeException ex) {
            if (counters != null) {
                counters.incrementToolValidationFailure();
            }
            throw validationException(ex.getMessage(), ex);
        }
        if (counters != null) {
            counters.incrementToolInvocation();
        }
        try {
            return tool.execute(arguments);
        } catch (RuntimeException ex) {
            if (counters != null) {
                counters.incrementToolExecutionFailure();
            }
            throw executionException(ex.getMessage(), ex);
        }
    }

    private static IllegalStateException validationException(String message) {
        return new IllegalStateException(PREFIX_VALIDATION + message);
    }

    private static IllegalStateException validationException(String message, Throwable cause) {
        return new IllegalStateException(PREFIX_VALIDATION + message, cause);
    }

    private static IllegalStateException executionException(String message, Throwable cause) {
        return new IllegalStateException(PREFIX_EXECUTION + message, cause);
    }

    private static ToolSpecification toSpecification(ProductTool tool) {
        JsonObjectSchema.Builder schemaBuilder = JsonObjectSchema.builder();
        List<String> required = new ArrayList<>();
        for (Map.Entry<String, ProductToolParameterSpec> entry : tool.parameterSpecs().entrySet()) {
            String name = entry.getKey();
            ProductToolParameterSpec spec = Objects.requireNonNull(entry.getValue(), "parameter spec must not be null");
            String type = spec.type() == null ? "string" : spec.type().trim().toLowerCase();
            String description = spec.description();
            switch (type) {
                case "integer" -> schemaBuilder.addIntegerProperty(name, description);
                case "number" -> schemaBuilder.addNumberProperty(name, description);
                case "boolean" -> schemaBuilder.addBooleanProperty(name, description);
                default -> schemaBuilder.addStringProperty(name, description);
            }
            if (spec.required()) {
                required.add(name);
            }
        }
        if (!required.isEmpty()) {
            schemaBuilder.required(required);
        }
        JsonObjectSchema parameters = schemaBuilder.additionalProperties(false).build();
        return ToolSpecification.builder()
                .name(tool.name())
            .description(tool.description())
                .parameters(parameters)
                .build();
    }

    private Map<String, Object> parseArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return Map.of();
        }
        String text = arguments.trim();
        if (!text.startsWith("{")) {
            return Map.of("input", stripQuotes(text));
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(text, new TypeReference<>() {
            });
            return parsed == null ? Map.of() : parsed;
        } catch (Exception ex) {
            throw new IllegalStateException("invalid tool arguments json: " + arguments, ex);
        }
    }

    private static void validateArguments(ProductTool tool, Map<String, Object> arguments) {
        for (Map.Entry<String, ProductToolParameterSpec> entry : tool.parameterSpecs().entrySet()) {
            String name = entry.getKey();
            ProductToolParameterSpec spec = entry.getValue();
            Object value = arguments.get(name);
            if (spec.required() && value == null) {
                throw new IllegalStateException("tool argument missing: " + name);
            }
            if (value == null) {
                continue;
            }
            String type = spec.type() == null ? "string" : spec.type().trim().toLowerCase();
            boolean matched = switch (type) {
                case "integer" -> value instanceof Integer || value instanceof Long;
                case "number" -> value instanceof Number;
                case "boolean" -> value instanceof Boolean;
                default -> value instanceof String;
            };
            if (!matched) {
                throw new IllegalStateException("tool argument type mismatch for " + name + ": expected " + type);
            }
        }
    }

    private static String stripQuotes(String value) {
        String text = value.trim();
        if (text.startsWith("\"") && text.endsWith("\"") && text.length() >= 2) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }
}
