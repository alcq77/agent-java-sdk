package io.github.alcq77.cqagent.core.rag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 本地知识库导入器：扫描目录并导入 md/txt 文档。
 */
public class RagLocalFileImporter {

    /**
     * Default max file size: 10 MB. Files larger than this are skipped.
     */
    private static final long DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * Default max directory depth: 10 levels.
     */
    private static final int DEFAULT_MAX_DEPTH = 10;

    private final long maxFileSize;
    private final int maxDepth;

    public RagLocalFileImporter() {
        this(DEFAULT_MAX_FILE_SIZE, DEFAULT_MAX_DEPTH);
    }

    public RagLocalFileImporter(long maxFileSize, int maxDepth) {
        this.maxFileSize = maxFileSize > 0 ? maxFileSize : DEFAULT_MAX_FILE_SIZE;
        this.maxDepth = maxDepth > 0 ? maxDepth : DEFAULT_MAX_DEPTH;
    }

    public List<RagDocument> load(Path root) {
        List<RagDocument> documents = new ArrayList<>();
        if (root == null || !Files.exists(root) || !Files.isDirectory(root)) {
            return documents;
        }
        // Use maxDepth to prevent deep recursion; NOFOLLOW_LINKS not available in all Java versions
        try (var stream = Files.walk(root, maxDepth)) {
            stream.filter(Files::isRegularFile)
                .filter(this::isSupportedTextFile)
                .forEach(path -> {
                    try {
                        long size = Files.size(path);
                        if (size > maxFileSize) {
                            return; // skip oversized files
                        }
                        if (size == 0) {
                            return; // skip empty files
                        }
                        documents.add(toDocument(root, path));
                    } catch (IOException ignored) {
                        // skip files that cannot be read
                    }
                });
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load rag documents from: " + root, ex);
        }
        return documents;
    }

    private boolean isSupportedTextFile(Path path) {
        String name = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase();
        return name.endsWith(".md") || name.endsWith(".txt");
    }

    private RagDocument toDocument(Path root, Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            String relative = root.relativize(path).toString().replace("\\", "/");
            long fileSize = Files.size(path);
            FileTime modified = Files.getLastModifiedTime(path);
            return new RagDocument(
                relative,
                path.getFileName() == null ? relative : path.getFileName().toString(),
                content,
                Map.of(
                    "source", path.toAbsolutePath().toString(),
                    "relativePath", relative,
                    "fileSize", String.valueOf(fileSize),
                    "lastModifiedEpochMs", String.valueOf(modified.toMillis())
                )
            );
        } catch (IOException ex) {
            throw new IllegalStateException("failed to read rag file: " + path, ex);
        }
    }
}
