package io.github.alcq77.cqagent.spi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Physical model endpoint configuration.
 *
 * <p>The router selects one endpoint and passes it to {@link ProductModelProvider}
 * to construct LangChain4j model instances.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEndpointConfig {

    private String id;

    private String provider;

    private String baseUrl;

    private String apiKey;

    private String defaultModel;

    @Builder.Default
    private Map<String, String> headers = new HashMap<>();

    @Builder.Default
    private Duration connectTimeout = Duration.ofSeconds(10);

    @Builder.Default
    private Duration readTimeout = Duration.ofSeconds(120);
}
