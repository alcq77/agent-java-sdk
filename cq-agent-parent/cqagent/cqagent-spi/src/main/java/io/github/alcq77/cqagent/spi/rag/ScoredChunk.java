package io.github.alcq77.cqagent.spi.rag;

/**
 * 带相似度分数的检索结果。
 *
 * @param chunk chunk 数据
 * @param score 相似度分数（0.0 ~ 1.0，越大越相关）
 */
public record ScoredChunk(VectorChunk chunk, double score) {
}
