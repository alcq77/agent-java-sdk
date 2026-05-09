package io.github.alcq77.cqagent.core.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文本切分器：按字符窗口切分并保留重叠。
 */
public class RagChunkSplitter {

    private final int chunkSize;
    private final int overlap;

    public RagChunkSplitter() {
        this(600, 120);
    }

    public RagChunkSplitter(int chunkSize, int overlap) {
        this.chunkSize = Math.max(100, chunkSize);
        this.overlap = Math.max(0, Math.min(overlap, this.chunkSize / 2));
    }

    public List<RagChunk> split(RagDocument document, TextEmbeddingModel embeddingModel) {
        List<RagChunk> chunks = new ArrayList<>();
        if (document == null || document.content() == null || document.content().isBlank()) {
            return chunks;
        }
        String text = document.content().trim();
        int start = 0;
        int index = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + chunkSize);
            String piece = text.substring(start, end);
            String chunkId = document.id() + "#" + index;
            chunks.add(new RagChunk(
                chunkId,
                document.id(),
                piece,
                document.metadata() == null ? Map.of() : document.metadata(),
                embeddingModel.embed(piece)
            ));
            if (end == text.length()) {
                break;
            }
            start = Math.max(start + 1, end - overlap);
            index++;
        }
        return chunks;
    }
}
