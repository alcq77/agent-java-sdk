package io.github.alcq77.cqagent.core.rag;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 索引清单持久化（JSON 文件）。
 */
public class RagIndexMetadataStore {

    private final Path manifestPath;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RagIndexMetadataStore(Path manifestPath) {
        this.manifestPath = manifestPath;
    }

    public RagIndexManifest load() {
        if (manifestPath == null || !Files.exists(manifestPath)) {
            return new RagIndexManifest();
        }
        try {
            RagIndexManifest manifest = objectMapper.readValue(manifestPath.toFile(), RagIndexManifest.class);
            return manifest == null ? new RagIndexManifest() : manifest;
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load rag manifest: " + manifestPath, ex);
        }
    }

    public void save(RagIndexManifest manifest) {
        if (manifestPath == null) {
            return;
        }
        try {
            if (manifestPath.getParent() != null) {
                Files.createDirectories(manifestPath.getParent());
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath.toFile(), manifest);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to save rag manifest: " + manifestPath, ex);
        }
    }
}
