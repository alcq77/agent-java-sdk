package io.github.alcq77.cqagent.core.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存知识块存储。
 */
public class InMemoryRagStore {

    private final Map<String, RagChunk> chunkById = new ConcurrentHashMap<>();

    public void replaceAll(List<RagChunk> newChunks) {
        chunkById.clear();
        if (newChunks != null) {
            for (RagChunk chunk : newChunks) {
                if (chunk != null) {
                    chunkById.put(chunk.chunkId(), chunk);
                }
            }
        }
    }

    public List<RagChunk> all() {
        return new ArrayList<>(chunkById.values());
    }

    public void replaceDocumentChunks(String documentId, List<RagChunk> chunks) {
        removeDocument(documentId);
        if (chunks == null) {
            return;
        }
        for (RagChunk chunk : chunks) {
            if (chunk != null) {
                chunkById.put(chunk.chunkId(), chunk);
            }
        }
    }

    public void removeDocument(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            return;
        }
        chunkById.entrySet().removeIf(entry -> documentId.equals(entry.getValue().documentId()));
    }
}
