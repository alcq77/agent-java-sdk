package io.github.alcq77.cqagent.starter;

import io.github.alcq77.cqagent.core.rag.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RagIndexRefresherTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRefreshAndReportHealthyState() throws Exception {
        Path kb = tempDir.resolve("knowledge");
        Files.createDirectories(kb);
        Files.writeString(kb.resolve("a.md"), "hello rag");

        RagIndexer indexer = new RagIndexer(new RagChunkSplitter(200, 20), new SimpleHashEmbeddingModel(64), new InMemoryRagStore());
        RagIndexRefresher refresher = new RagIndexRefresher(
                indexer,
                new RagLocalFileImporter(),
                new RagIndexMetadataStore(tempDir.resolve("manifest.json")),
                kb,
                0
        );

        refresher.refreshNow();

        assertTrue(refresher.healthy());
        Map<String, Object> snap = refresher.snapshot();
        assertEquals(1L, snap.get("documentCount"));
        assertNotNull(snap.get("lastRefreshEpochMs"));
    }

    @Test
    void shouldReportUnhealthyWhenRefreshFails() {
        RagIndexer brokenIndexer = new RagIndexer(new RagChunkSplitter(200, 20), new SimpleHashEmbeddingModel(64), new InMemoryRagStore()) {
            @Override
            public RagIndexManifest syncIncremental(Path knowledgeRoot, RagLocalFileImporter importer, RagIndexMetadataStore metadataStore) {
                throw new IllegalStateException("boom");
            }
        };

        RagIndexRefresher refresher = new RagIndexRefresher(
                brokenIndexer,
                new RagLocalFileImporter(),
                new RagIndexMetadataStore(tempDir.resolve("manifest.json")),
                tempDir.resolve("missing"),
                0
        );

        refresher.refreshNow();

        assertFalse(refresher.healthy());
        assertTrue(String.valueOf(refresher.snapshot().get("lastError")).contains("boom"));
    }
}
