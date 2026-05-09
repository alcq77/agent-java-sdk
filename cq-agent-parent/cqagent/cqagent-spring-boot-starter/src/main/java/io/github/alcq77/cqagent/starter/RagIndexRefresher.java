package io.github.alcq77.cqagent.starter;

import io.github.alcq77.cqagent.core.rag.RagIndexManifest;
import io.github.alcq77.cqagent.core.rag.RagIndexMetadataStore;
import io.github.alcq77.cqagent.core.rag.RagIndexer;
import io.github.alcq77.cqagent.core.rag.RagLocalFileImporter;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

/**
 * RAG 增量索引热更新执行器（启动全量一次 + 定时增量）。
 * <p>
 * 职责边界：
 * - 只负责编排刷新时机与状态快照；
 * - 具体索引逻辑交由 {@link RagIndexer}；
 * - 对外暴露轻量健康态与统计，用于 health endpoint。
 */
@Slf4j
public class RagIndexRefresher {

    private final RagIndexer indexer;
    private final RagLocalFileImporter importer;
    private final RagIndexMetadataStore metadataStore;
    private final Path knowledgeRoot;
    private final long refreshIntervalSeconds;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "cqagent-rag-refresh"));

    private final AtomicReference<RagIndexManifest> latestManifest = new AtomicReference<>(new RagIndexManifest());
    private final AtomicReference<String> lastError = new AtomicReference<>();
    private volatile long lastRefreshEpochMs;
    private volatile long lastErrorEpochMs;

    public RagIndexRefresher(RagIndexer indexer,
                             RagLocalFileImporter importer,
                             RagIndexMetadataStore metadataStore,
                             Path knowledgeRoot,
                             long refreshIntervalSeconds) {
        this.indexer = indexer;
        this.importer = importer;
        this.metadataStore = metadataStore;
        this.knowledgeRoot = knowledgeRoot;
        this.refreshIntervalSeconds = refreshIntervalSeconds;
    }

    public void start() {
        // 启动阶段先做一次同步，确保服务启动后立即可检索。
        refreshNow();
        if (refreshIntervalSeconds > 0) {
            scheduler.scheduleAtFixedRate(this::refreshNow, refreshIntervalSeconds, refreshIntervalSeconds, TimeUnit.SECONDS);
        }
    }

    public void stop() {
        // Spring 容器销毁时停止后台刷新线程，避免线程泄漏。
        scheduler.shutdownNow();
    }

    public void refreshNow() {
        try {
            RagIndexManifest manifest = indexer.syncIncremental(knowledgeRoot, importer, metadataStore);
            latestManifest.set(manifest);
            lastRefreshEpochMs = System.currentTimeMillis();
            lastError.set(null);
        } catch (Exception ex) {
            lastError.set(ex.getMessage());
            lastErrorEpochMs = System.currentTimeMillis();
            log.error("RAG incremental refresh failed: root={}", knowledgeRoot, ex);
        }
    }

    public boolean healthy() {
        return lastError.get() == null || lastError.get().isBlank();
    }

    public Map<String, Object> snapshot() {
        RagIndexManifest manifest = latestManifest.get();
        long documentCount = manifest.getDocuments().size();
        long chunkCount = manifest.getDocuments().values().stream()
                .mapToLong(RagIndexManifest.DocumentSnapshot::getChunkCount)
                .sum();
        long lastIndexedEpochMs = manifest.getDocuments().values().stream()
                .mapToLong(RagIndexManifest.DocumentSnapshot::getIndexedAtEpochMs)
                .max()
                .orElse(0L);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("knowledgeRoot", knowledgeRoot.toString());
        out.put("refreshIntervalSeconds", refreshIntervalSeconds);
        out.put("documentCount", documentCount);
        out.put("chunkCount", chunkCount);
        out.put("lastIndexedEpochMs", lastIndexedEpochMs);
        out.put("lastRefreshEpochMs", lastRefreshEpochMs);
        out.put("lastErrorEpochMs", lastErrorEpochMs);
        out.put("lastError", lastError.get());
        out.put("healthy", healthy());
        return out;
    }
}
