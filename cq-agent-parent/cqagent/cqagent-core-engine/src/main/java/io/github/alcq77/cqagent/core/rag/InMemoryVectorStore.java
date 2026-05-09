package io.github.alcq77.cqagent.core.rag;

import io.github.alcq77.cqagent.spi.rag.MetadataFilter;
import io.github.alcq77.cqagent.spi.rag.ScoredChunk;
import io.github.alcq77.cqagent.spi.rag.VectorChunk;
import io.github.alcq77.cqagent.spi.rag.VectorStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 内存向量存储（copy-on-write），适合开发/测试和中小规模知识库。
 * <p>
 * 生产环境建议替换为 Milvus、PgVector、Elasticsearch 等专用向量数据库。
 */
public class InMemoryVectorStore implements VectorStore {

    private final AtomicReference<Map<String, VectorChunk>> snapshot = new AtomicReference<>(Map.of());

    @Override
    public void upsert(List<VectorChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        Map<String, VectorChunk> next = new ConcurrentHashMap<>(snapshot.get());
        for (VectorChunk chunk : chunks) {
            if (chunk != null) {
                next.put(chunk.chunkId(), chunk);
            }
        }
        snapshot.set(next);
    }

    @Override
    public List<ScoredChunk> search(double[] queryEmbedding, int topK, MetadataFilter filter) {
        List<ScoredChunk> scored = new ArrayList<>();
        for (VectorChunk chunk : snapshot.get().values()) {
            if (!matchesFilter(chunk, filter)) {
                continue;
            }
            double score = cosineSimilarity(queryEmbedding, chunk.embedding());
            scored.add(new ScoredChunk(chunk, score));
        }
        scored.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());
        int limit = Math.max(1, topK);
        return scored.subList(0, Math.min(limit, scored.size()));
    }

    @Override
    public void deleteByDocumentId(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            return;
        }
        Map<String, VectorChunk> old = snapshot.get();
        Map<String, VectorChunk> next = new ConcurrentHashMap<>(old);
        next.entrySet().removeIf(e -> documentId.equals(e.getValue().documentId()));
        snapshot.set(next);
    }

    @Override
    public void clear() {
        snapshot.set(Map.of());
    }

    @Override
    public long count() {
        return snapshot.get().size();
    }

    private static boolean matchesFilter(VectorChunk chunk, MetadataFilter filter) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        Map<String, String> meta = chunk.metadata();
        for (Map.Entry<String, String> entry : filter.equalsMetadata().entrySet()) {
            String actual = meta.get(entry.getKey());
            if (actual == null || !actual.equals(entry.getValue())) {
                return false;
            }
        }
        if (!filter.allowedSources().isEmpty()) {
            String source = meta.get("source");
            if (source == null || filter.allowedSources().stream().noneMatch(source::contains)) {
                return false;
            }
        }
        return true;
    }

    /**
     * True cosine similarity: dot(a,b) / (||a|| * ||b||).
     */
    static double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator == 0.0 ? 0.0 : dot / denominator;
    }
}
