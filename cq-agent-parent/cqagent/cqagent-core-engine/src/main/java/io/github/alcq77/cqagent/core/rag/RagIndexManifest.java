package io.github.alcq77.cqagent.core.rag;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RAG 索引清单：记录文档快照用于增量同步。
 */
public class RagIndexManifest {

    private Map<String, DocumentSnapshot> documents = new LinkedHashMap<>();

    public Map<String, DocumentSnapshot> getDocuments() {
        return documents;
    }

    public void setDocuments(Map<String, DocumentSnapshot> documents) {
        this.documents = documents == null ? new LinkedHashMap<>() : new LinkedHashMap<>(documents);
    }

    public static class DocumentSnapshot {
        private String contentHash;
        private long fileSize;
        private long lastModifiedEpochMs;
        private int chunkCount;
        private long indexedAtEpochMs;

        public String getContentHash() {
            return contentHash;
        }

        public void setContentHash(String contentHash) {
            this.contentHash = contentHash;
        }

        public long getFileSize() {
            return fileSize;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }

        public long getLastModifiedEpochMs() {
            return lastModifiedEpochMs;
        }

        public void setLastModifiedEpochMs(long lastModifiedEpochMs) {
            this.lastModifiedEpochMs = lastModifiedEpochMs;
        }

        public int getChunkCount() {
            return chunkCount;
        }

        public void setChunkCount(int chunkCount) {
            this.chunkCount = chunkCount;
        }

        public long getIndexedAtEpochMs() {
            return indexedAtEpochMs;
        }

        public void setIndexedAtEpochMs(long indexedAtEpochMs) {
            this.indexedAtEpochMs = indexedAtEpochMs;
        }
    }
}
