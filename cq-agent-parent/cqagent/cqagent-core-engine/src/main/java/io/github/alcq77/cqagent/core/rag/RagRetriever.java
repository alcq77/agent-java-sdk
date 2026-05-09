package io.github.alcq77.cqagent.core.rag;

import io.github.alcq77.cqagent.spi.rag.MetadataFilter;
import io.github.alcq77.cqagent.spi.rag.ScoredChunk;
import io.github.alcq77.cqagent.spi.rag.VectorChunk;
import io.github.alcq77.cqagent.spi.rag.VectorStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG 检索器 —— 将查询 embedding 后委托给 VectorStore 做相似度检索。
 * <p>
 * 职责边界：
 * <ul>
 *   <li>负责查询向量化 + 结果转换（VectorChunk → RagChunk）</li>
 *   <li>向量检索逻辑（相似度计算、索引、过滤）完全委托给 VectorStore 实现</li>
 *   <li>用户可通过替换 VectorStore bean 自由选择 Milvus、PgVector 等后端</li>
 * </ul>
 */
public class RagRetriever {

    private final VectorStore vectorStore;
    private final TextEmbeddingModel embeddingModel;

    public RagRetriever(VectorStore vectorStore, TextEmbeddingModel embeddingModel) {
        if (vectorStore == null) throw new IllegalArgumentException("vectorStore must not be null");
        if (embeddingModel == null) throw new IllegalArgumentException("embeddingModel must not be null");
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
    }

    /**
     * 向量检索，返回最相关的 RagChunk 列表。
     */
    public List<RagChunk> retrieve(String query, int topK) {
        return retrieve(query, topK, null);
    }

    /**
     * 向量检索（带过滤条件）。
     */
    public List<RagChunk> retrieve(String query, int topK, RagRetrievalFilter filter) {
        double[] queryEmbedding = embeddingModel.embed(query);
        MetadataFilter metadataFilter = toMetadataFilter(filter);
        List<ScoredChunk> results = vectorStore.search(queryEmbedding, Math.max(1, topK), metadataFilter);

        List<RagChunk> chunks = new ArrayList<>(results.size());
        for (ScoredChunk scored : results) {
            VectorChunk vc = scored.chunk();
            chunks.add(new RagChunk(
                vc.chunkId(),
                vc.documentId(),
                vc.text(),
                vc.metadata(),
                vc.embedding()
            ));
        }
        return chunks;
    }

    /**
     * Returns the underlying vector store (for direct access if needed).
     */
    public VectorStore vectorStore() {
        return vectorStore;
    }

    private static MetadataFilter toMetadataFilter(RagRetrievalFilter filter) {
        if (filter == null) {
            return MetadataFilter.none();
        }
        return MetadataFilter.of(filter.getEqualsMetadata(), filter.getAllowedSources());
    }
}
