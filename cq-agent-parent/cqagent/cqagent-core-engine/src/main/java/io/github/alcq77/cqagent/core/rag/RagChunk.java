package io.github.alcq77.cqagent.core.rag;

import java.util.Map;

/**
 * 切分后的知识块。
 */
public record RagChunk(
    String chunkId,
    String documentId,
    String text,
    Map<String, String> metadata,
    double[] embedding
) {
}
