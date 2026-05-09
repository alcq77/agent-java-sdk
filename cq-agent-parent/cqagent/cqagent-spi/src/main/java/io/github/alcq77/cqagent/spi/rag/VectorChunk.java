package io.github.alcq77.cqagent.spi.rag;

import java.util.Map;

/**
 * 向量存储中的一个 chunk 记录。
 *
 * @param chunkId      唯一标识（如 "doc-1#0"）
 * @param documentId   所属文档 ID
 * @param text         原始文本内容
 * @param embedding    向量表示
 * @param metadata     附加元数据（如 source、page、tenant 等）
 */
public record VectorChunk(
    String chunkId,
    String documentId,
    String text,
    double[] embedding,
    Map<String, String> metadata
) {
    public VectorChunk {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
