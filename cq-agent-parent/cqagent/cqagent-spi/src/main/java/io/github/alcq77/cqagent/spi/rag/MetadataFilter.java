package io.github.alcq77.cqagent.spi.rag;

import java.util.Map;

/**
 * 元数据过滤条件，用于向量检索时筛选结果。
 *
 * @param equalsMetadata 精确匹配条件（key-value 全部相等才命中）
 * @param allowedSources 允许的来源列表（source 字段包含列表中任一值即命中，为空表示不过滤）
 */
public record MetadataFilter(
    Map<String, String> equalsMetadata,
    java.util.List<String> allowedSources
) {
    public static MetadataFilter of(Map<String, String> equalsMetadata, java.util.List<String> allowedSources) {
        return new MetadataFilter(
            equalsMetadata == null ? Map.of() : Map.copyOf(equalsMetadata),
            allowedSources == null ? java.util.List.of() : java.util.List.copyOf(allowedSources)
        );
    }

    public static MetadataFilter none() {
        return new MetadataFilter(Map.of(), java.util.List.of());
    }

    public boolean isEmpty() {
        return equalsMetadata.isEmpty() && allowedSources.isEmpty();
    }
}
