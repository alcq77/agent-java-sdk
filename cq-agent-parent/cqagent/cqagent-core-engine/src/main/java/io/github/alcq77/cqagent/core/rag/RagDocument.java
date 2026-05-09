package io.github.alcq77.cqagent.core.rag;

import java.util.Map;

/**
 * 原始知识文档。
 */
public record RagDocument(
    String id,
    String title,
    String content,
    Map<String, String> metadata
) {
}
