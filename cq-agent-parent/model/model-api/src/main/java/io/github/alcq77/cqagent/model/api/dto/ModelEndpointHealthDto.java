package io.github.alcq77.cqagent.model.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelEndpointHealthDto {

    private String endpointId;

    private String provider;

    private boolean reachable;

    private Integer statusCode;

    private String message;
}
