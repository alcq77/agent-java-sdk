package io.github.alcq77.cqagent.core.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class RoutePolicy {

    private String primaryEndpoint;

    private List<String> secondaryEndpoints = new ArrayList<>();

    private Map<String, Integer> weightedEndpoints = new LinkedHashMap<>();

    private boolean healthAware = true;
}
