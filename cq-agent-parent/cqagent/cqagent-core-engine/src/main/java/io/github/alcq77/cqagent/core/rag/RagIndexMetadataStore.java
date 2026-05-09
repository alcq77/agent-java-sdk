package io.github.alcq77.cqagent.core.rag;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 索引清单持久化（JSON 文件，原子写入）。
 * <p>
 * 写入策略：先写入临时文件，再原子 rename，避免 crash 时文件损坏。
 */
public class RagIndexMetadataStore {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Path manifestPath;

    public RagIndexMetadataStore(Path manifestPath) {
        this.manifestPath = manifestPath;
    }

    public RagIndexManifest load() {
        if (manifestPath == null || !Files.exists(manifestPath)) {
            return new RagIndexManifest();
        }
        try {
            RagIndexManifest manifest = OBJECT_MAPPER.readValue(manifestPath.toFile(), RagIndexManifest.class);
            return manifest == null ? new RagIndexManifest() : manifest;
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load rag manifest: " + manifestPath, ex);
        }
    }

    /**
     * Atomically saves the manifest: writes to a temp file first, then renames.
     * This prevents corruption if the process crashes mid-write.
     */
    public void save(RagIndexManifest manifest) {
        if (manifestPath == null) {
            return;
        }
        try {
            if (manifestPath.getParent() != null) {
                Files.createDirectories(manifestPath.getParent());
            }
            Path tempPath = manifestPath.resolveSibling(manifestPath.getFileName() + ".tmp");
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(tempPath.toFile(), manifest);
            Files.move(tempPath, manifestPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to save rag manifest: " + manifestPath, ex);
        }
    }
}
