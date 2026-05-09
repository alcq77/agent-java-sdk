package io.github.alcq77.cqagent.core.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 内存知识块存储。使用 copy-on-write 策略保证读写线程安全。
 */
public class InMemoryRagStore {

    private final AtomicReference<Map<String, RagChunk>> snapshot = new AtomicReference<>(Map.of());

    public void replaceAll(List<RagChunk> newChunks) {
        Map<String, RagChunk> next = new ConcurrentHashMap<>();
        if (newChunks != null) {
            for (RagChunk chunk : newChunks) {
                if (chunk != null) {
                    next.put(chunk.chunkId(), chunk);
                }
            }
        }
        snapshot.set(next);
    }

    public List<RagChunk> all() {
        return new ArrayList<>(snapshot.get().values());
    }

    public void replaceDocumentChunks(String documentId, List<RagChunk> chunks) {
        Map<String, RagChunk> old = snapshot.get();
        Map<String, RagChunk> next = new ConcurrentHashMap<>(old);
        next.entrySet().removeIf(e -> documentId.equals(e.getValue().documentId()));
        if (chunks != null) {
            for (RagChunk chunk : chunks) {
                if (chunk != null) {
                    next.put(chunk.chunkId(), chunk);
                }
            }
        }
        snapshot.set(next);
    }

    public void removeDocument(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            return;
        }
        Map<String, RagChunk> old = snapshot.get();
        Map<String, RagChunk> next = new ConcurrentHashMap<>(old);
        next.entrySet().removeIf(entry -> documentId.equals(entry.getValue().documentId()));
        snapshot.set(next);
    }

    public int size() {
        return snapshot.get().size();
    }
}
