package io.github.alcq77.cqagent.model.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 已配置的厂商端点信息（不含密钥），供控制台/调试列出可用模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelEndpointInfoDto {

    private String endpointId;

    /** 对应配置项 {@code provider}，如 openai_compat、anthropic。 */
    private String provider;

    private String baseUrl;

    private String defaultModel;

    private List<String> routedModelAliases;
}
