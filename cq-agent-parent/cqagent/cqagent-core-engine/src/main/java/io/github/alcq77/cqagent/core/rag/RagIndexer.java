package io.github.alcq77.cqagent.core.rag;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG 建索引入口：文档导入 + 切分 + embedding。
 * <p>
 * 职责边界：
 * 1) 只负责索引计算与提交，不负责定时调度；
 * 2) 只依赖 {@link InMemoryRagStore}，不绑定具体向量数据库实现；
 * 3) 对外暴露 rebuild（全量）和 syncIncremental（增量）两种模式。
 */
public class RagIndexer {

    private final RagChunkSplitter splitter;
    private final TextEmbeddingModel embeddingModel;
    private final InMemoryRagStore store;

    public RagIndexer(RagChunkSplitter splitter, TextEmbeddingModel embeddingModel, InMemoryRagStore store) {
        this.splitter = splitter;
        this.embeddingModel = embeddingModel;
        this.store = store;
    }

    public void rebuild(List<RagDocument> documents) {
        List<RagChunk> chunks = new ArrayList<>();
        if (documents != null) {
            for (RagDocument document : documents) {
                chunks.addAll(splitter.split(document, embeddingModel));
            }
        }
        store.replaceAll(chunks);
    }

    /**
     * 增量同步：仅重建变化文档，回收已删除文档。
     * <p>
     * 执行顺序：
     * 1) 扫描文档并计算变化集；
     * 2) 在内存中构建下一版 chunksByDocument；
     * 3) 持久化最新 manifest；
     * 4) 一次性 replaceAll 提交到 store，避免逐文档写入造成中间态。
     */
    public RagIndexManifest syncIncremental(Path knowledgeRoot, RagLocalFileImporter importer, RagIndexMetadataStore metadataStore) {
        List<RagDocument> documents = importer.load(knowledgeRoot);
        RagIndexManifest manifest = metadataStore.load();
        Map<String, RagIndexManifest.DocumentSnapshot> snapshots = new LinkedHashMap<>(manifest.getDocuments());
        Map<String, List<RagChunk>> chunksByDocument = store.all().stream()
                .collect(Collectors.groupingBy(RagChunk::documentId, LinkedHashMap::new, Collectors.toList()));

        Map<String, RagDocument> documentMap = new LinkedHashMap<>();
        for (RagDocument document : documents) {
            documentMap.put(document.id(), document);
        }

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
            List<RagChunk> chunks = splitter.split(document, embeddingModel);
            chunksByDocument.put(docId, chunks);
            snapshots.put(docId, toSnapshot(document, chunks.size()));
        }

        List<String> removed = new ArrayList<>();
        for (String indexedDocId : snapshots.keySet()) {
            if (!documentMap.containsKey(indexedDocId)) {
                removed.add(indexedDocId);
            }
        }
        for (String removedDocId : removed) {
            chunksByDocument.remove(removedDocId);
            snapshots.remove(removedDocId);
        }

        manifest.setDocuments(snapshots);
        // 先持久化清单，确保索引元信息不会落后于本次计算结果。
        metadataStore.save(manifest);
        List<RagChunk> flattened = chunksByDocument.values().stream()
                .flatMap(Collection::stream)
                .toList();
        store.replaceAll(flattened);
        return manifest;
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
}
