package io.github.alcq77.cqagent.core.rag;

import io.github.alcq77.cqagent.spi.rag.VectorChunk;
import io.github.alcq77.cqagent.spi.rag.VectorStore;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 建索引入口：文档导入 + 切分 + embedding。
 * <p>
 * 职责边界：
 * <ul>
 *   <li>只负责索引计算与提交，不负责定时调度</li>
 *   <li>通过 {@link VectorStore} SPI 抽象存储后端，用户可自由选择 Milvus、PgVector 等</li>
 *   <li>对外暴露 rebuild（全量）和 syncIncremental（增量）两种模式</li>
 * </ul>
 */
public class RagIndexer {

    private final RagChunkSplitter splitter;
    private final TextEmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    public RagIndexer(RagChunkSplitter splitter, TextEmbeddingModel embeddingModel, VectorStore vectorStore) {
        this.splitter = splitter != null ? splitter : throwIAE("splitter must not be null");
        this.embeddingModel = embeddingModel != null ? embeddingModel : throwIAE("embeddingModel must not be null");
        this.vectorStore = vectorStore != null ? vectorStore : throwIAE("vectorStore must not be null");
    }

    /**
     * Backward-compatible constructor that accepts InMemoryRagStore.
     */
    public RagIndexer(RagChunkSplitter splitter, TextEmbeddingModel embeddingModel, InMemoryRagStore store) {
        this(splitter, embeddingModel, new InMemoryRagStoreAdapter(store));
    }

    private static <T> T throwIAE(String msg) {
        throw new IllegalArgumentException(msg);
    }

    /**
     * 全量重建：清空 store 后重新索引所有文档。
     */
    public void rebuild(List<RagDocument> documents) {
        vectorStore.clear();
        if (documents != null) {
            for (RagDocument document : documents) {
                List<RagChunk> chunks = splitter.split(document, embeddingModel);
                vectorStore.upsert(toVectorChunks(chunks));
            }
        }
    }

    /**
     * 增量同步：仅重建变化文档，删除已移除文档，未变化文档保持不变。
     * <p>
     * 执行顺序：
     * <ol>
     *   <li>扫描文档并计算变化集（基于 manifest 中的 content hash）</li>
     *   <li>删除已移除文档的 chunks</li>
     *   <li>对变化文档重新切分并 upsert</li>
     *   <li>持久化最新 manifest</li>
     * </ol>
     */
    public RagIndexManifest syncIncremental(Path knowledgeRoot, RagLocalFileImporter importer, RagIndexMetadataStore metadataStore) {
        List<RagDocument> documents = importer.load(knowledgeRoot);
        RagIndexManifest manifest = metadataStore.load();
        Map<String, RagIndexManifest.DocumentSnapshot> snapshots = new LinkedHashMap<>(manifest.getDocuments());

        Map<String, RagDocument> documentMap = new LinkedHashMap<>();
        for (RagDocument document : documents) {
            documentMap.put(document.id(), document);
        }

        // Detect changed documents
        for (RagDocument document : documents) {
            String docId = document.id();
            RagIndexManifest.DocumentSnapshot old = snapshots.get(docId);
            RagIndexManifest.DocumentSnapshot latest = toSnapshot(document, 0);
            boolean changed = old == null
                || !safeEquals(old.getContentHash(), latest.getContentHash())
                || old.getFileSize() != latest.getFileSize()
                || old.getLastModifiedEpochMs() != latest.getLastModifiedEpochMs();
            if (!changed) {
                continue;
            }
            // Re-chunk and upsert
            List<RagChunk> chunks = splitter.split(document, embeddingModel);
            vectorStore.upsert(toVectorChunks(chunks));
            snapshots.put(docId, toSnapshot(document, chunks.size()));
        }

        // Delete removed documents
        List<String> removed = new ArrayList<>();
        for (String indexedDocId : snapshots.keySet()) {
            if (!documentMap.containsKey(indexedDocId)) {
                removed.add(indexedDocId);
            }
        }
        for (String removedDocId : removed) {
            vectorStore.deleteByDocumentId(removedDocId);
            snapshots.remove(removedDocId);
        }

        manifest.setDocuments(snapshots);
        metadataStore.save(manifest);
        return manifest;
    }

    private static List<VectorChunk> toVectorChunks(List<RagChunk> chunks) {
        List<VectorChunk> result = new ArrayList<>(chunks.size());
        for (RagChunk chunk : chunks) {
            result.add(new VectorChunk(
                chunk.chunkId(),
                chunk.documentId(),
                chunk.text(),
                chunk.embedding(),
                chunk.metadata()
            ));
        }
        return result;
    }

    private static RagIndexManifest.DocumentSnapshot toSnapshot(RagDocument document, int chunkCount) {
        RagIndexManifest.DocumentSnapshot snapshot = new RagIndexManifest.DocumentSnapshot();
        snapshot.setContentHash(sha256(document.content()));
        snapshot.setFileSize(parseLongMetadata(document, "fileSize", 0L));
        snapshot.setLastModifiedEpochMs(parseLongMetadata(document, "lastModifiedEpochMs", 0L));
        snapshot.setChunkCount(chunkCount);
        snapshot.setIndexedAtEpochMs(System.currentTimeMillis());
        return snapshot;
    }

    private static long parseLongMetadata(RagDocument document, String key, long defaultValue) {
        try {
            String value = document.metadata() == null ? null : document.metadata().get(key);
            return value == null ? defaultValue : Long.parseLong(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("failed to calculate sha256", ex);
        }
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    /**
     * Adapter that wraps InMemoryRagStore as a VectorStore, for backward compatibility.
     */
    private static class InMemoryRagStoreAdapter implements VectorStore {
        private final InMemoryRagStore delegate;

        InMemoryRagStoreAdapter(InMemoryRagStore delegate) {
            this.delegate = delegate;
        }

        @Override
        public void upsert(List<VectorChunk> chunks) {
            List<RagChunk> ragChunks = new ArrayList<>(chunks.size());
            for (VectorChunk vc : chunks) {
                ragChunks.add(new RagChunk(vc.chunkId(), vc.documentId(), vc.text(), vc.metadata(), vc.embedding()));
            }
            delegate.replaceAll(ragChunks);
        }

        @Override
        public List<io.github.alcq77.cqagent.spi.rag.ScoredChunk> search(double[] queryEmbedding, int topK, io.github.alcq77.cqagent.spi.rag.MetadataFilter filter) {
            throw new UnsupportedOperationException("InMemoryRagStoreAdapter does not support search; use RagRetriever instead");
        }

        @Override
        public void deleteByDocumentId(String documentId) {
            delegate.removeDocument(documentId);
        }

        @Override
        public void clear() {
            delegate.replaceAll(List.of());
        }

        @Override
        public long count() {
            return delegate.size();
        }
    }
}
