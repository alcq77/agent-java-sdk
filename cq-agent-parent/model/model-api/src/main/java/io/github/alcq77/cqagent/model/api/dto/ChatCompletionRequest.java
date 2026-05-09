package io.github.alcq77.cqagent.model.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Legacy compatibility DTO for OpenAI-compatible chat completion requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionRequest {

    /**
     * 逻辑模型名，用于查找 {@code agent.model.routing} 路由到具体厂商端点。
     */
    @NotBlank
    private String model;

    @NotEmpty
    @Valid
    private List<ChatMessageDto> messages;

    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @JsonProperty("top_p")
    private Double topP;

    /**
     * 是否流式；为 true 时请调用流式接口（本对象在同步接口中若 stream=true 将返回 400 提示）。
     */
    private Boolean stream;

    private List<String> stop;

    /**
     * 透传给厂商的扩展字段（高级场景）。
     */
    private Map<String, Object> extra;
}
