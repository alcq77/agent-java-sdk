package io.github.alcq77.cqagent.core.rag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 检索过滤条件（基于 chunk metadata）。
 */
public class RagRetrievalFilter {

    private final Map<String, String> equalsMetadata = new LinkedHashMap<>();
    private List<String> allowedSources = List.of();

    public Map<String, String> getEqualsMetadata() {
        return equalsMetadata;
    }

    public List<String> getAllowedSources() {
        return allowedSources;
    }

    public RagRetrievalFilter allowedSources(List<String> allowedSources) {
        this.allowedSources = allowedSources == null ? List.of() : allowedSources;
        return this;
    }

    public RagRetrievalFilter equalsMetadata(Map<String, String> metadata) {
        this.equalsMetadata.clear();
        if (metadata != null) {
            this.equalsMetadata.putAll(metadata);
        }
        return this;
    }
}
