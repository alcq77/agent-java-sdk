package io.github.alcq77.cqagent.starter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.alcq77.cqagent.model.api.dto.ChatMessageDto;
import io.github.alcq77.cqagent.spi.session.ProductSessionStore;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件系统会话存储实现。
 * <p>
 * 约定每个 session 一个 JSON 文件：{sessionDir}/chat-{hash}.json
 * 使用 SHA-256 hash 作为文件名避免 ID 碰撞（如 "foo-bar" 和 "foo_bar"）。
 * 写入使用临时文件 + 原子 rename 防止 crash 时文件损坏。
 */
@Slf4j
public class FileSystemProductSessionStore implements ProductSessionStore, SessionStoreHealthProbe, SessionStoreMetricsProvider {

    private static final TypeReference<List<ChatMessageDto>> MESSAGE_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final Path sessionDir;
    private final int maxHistoryMessages;
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public FileSystemProductSessionStore(ObjectMapper objectMapper, String directory, int maxHistoryMessages) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.sessionDir = Paths.get(directory == null || directory.isBlank() ? "./workspace/sessions" : directory)
                .toAbsolutePath()
                .normalize();
        this.maxHistoryMessages = Math.max(10, maxHistoryMessages);
        ensureDirectory();
    }

    @Override
    public boolean hasSession(String sessionId) {
        return Files.exists(resolveFile(sessionId));
    }

    @Override
    public void register(String sessionId) {
        Object lock = locks.computeIfAbsent(sessionId, k -> new Object());
        synchronized (lock) {
            Path file = resolveFile(sessionId);
            if (Files.exists(file)) {
                return;
            }
            writeMessages(file, List.of());
        }
    }

    @Override
    public List<ChatMessageDto> history(String sessionId) {
        Object lock = locks.computeIfAbsent(sessionId, k -> new Object());
        synchronized (lock) {
            Path file = resolveFile(sessionId);
            if (!Files.exists(file)) {
                return List.of();
            }
            try {
                String payload = Files.readString(file);
                List<ChatMessageDto> messages = objectMapper.readValue(payload, MESSAGE_LIST_TYPE);
                return messages == null ? List.of() : new ArrayList<>(messages);
            } catch (Exception ex) {
                log.warn("failed to read session history from file={}, fallback empty", file, ex);
                return List.of();
            }
        }
    }

    @Override
    public void append(String sessionId, ChatMessageDto user, ChatMessageDto assistant) {
        Object lock = locks.computeIfAbsent(sessionId, k -> new Object());
        synchronized (lock) {
            List<ChatMessageDto> history = new ArrayList<>(readMessages(sessionId));
            history.add(user);
            history.add(assistant);
            trimToSize(history);
            writeMessages(resolveFile(sessionId), history);
        }
    }

    @Override
    public void replaceHistory(String sessionId, List<ChatMessageDto> messages) {
        Object lock = locks.computeIfAbsent(sessionId, k -> new Object());
        synchronized (lock) {
            List<ChatMessageDto> history = messages == null ? new ArrayList<>() : new ArrayList<>(messages);
            trimToSize(history);
            writeMessages(resolveFile(sessionId), history);
        }
    }

    @Override
    public void deleteSession(String sessionId) {
        Object lock = locks.computeIfAbsent(sessionId, k -> new Object());
        synchronized (lock) {
            try {
                Files.deleteIfExists(resolveFile(sessionId));
            } catch (Exception ex) {
                log.warn("failed to delete session file for sessionId={}", sessionId, ex);
            }
        }
    }

    @Override
    public boolean healthy() {
        try {
            ensureDirectory();
            Path probe = sessionDir.resolve(".probe");
            Files.writeString(probe, Instant.now().toString(), StandardCharsets.UTF_8);
            Files.deleteIfExists(probe);
            return true;
        } catch (Exception ex) {
            log.warn("filesystem session store health check failed", ex);
            return false;
        }
    }

    @Override
    public String detail() {
        return "filesystem:" + sessionDir;
    }

    @Override
    public boolean supportsSessionEnumeration() {
        return true;
    }

    @Override
    public List<String> listSessionIds() {
        try (var stream = Files.list(sessionDir)) {
            return stream
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return n.startsWith("chat-") && n.endsWith(".json");
                    })
                    .map(p -> {
                        String n = p.getFileName().toString();
                        return n.substring("chat-".length(), n.length() - ".json".length());
                    })
                    .toList();
        } catch (Exception ex) {
            log.warn("failed to list session files", ex);
            return List.of();
        }
    }

    @Override
    public Map<String, Object> metrics() {
        long files = 0;
        try (var stream = Files.list(sessionDir)) {
            files = stream.filter(p -> p.getFileName().toString().startsWith("chat-")
                            && p.getFileName().toString().endsWith(".json"))
                    .count();
        } catch (Exception ex) {
            log.debug("failed to count session files", ex);
        }
        return Map.of(
                "backend", "filesystem",
                "sessionDir", sessionDir.toString(),
                "sessionFiles", files,
                "maxHistoryMessages", maxHistoryMessages
        );
    }

    private void ensureDirectory() {
        try {
            Files.createDirectories(sessionDir);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to create session directory: " + sessionDir, ex);
        }
    }

    /**
     * Generates a collision-free filename using SHA-256 hash of the session ID.
     * Unlike the old approach (replaceAll non-alnum to "_"), this preserves uniqueness:
     * "foo-bar" and "foo_bar" will produce different hashes.
     */
    private Path resolveFile(String sessionId) {
        String hash = sha256Hex(sessionId);
        return sessionDir.resolve("chat-" + hash + ".json");
    }

    /**
     * Atomically writes messages: temp file → rename.
     */
    private void writeMessages(Path file, List<ChatMessageDto> messages) {
        try {
            ensureDirectory();
            String payload = objectMapper.writeValueAsString(messages);
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tempFile, payload, StandardCharsets.UTF_8);
            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to write session file: " + file, ex);
        }
    }

    private List<ChatMessageDto> readMessages(String sessionId) {
        Path file = resolveFile(sessionId);
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            String payload = Files.readString(file);
            List<ChatMessageDto> messages = objectMapper.readValue(payload, MESSAGE_LIST_TYPE);
            return messages == null ? List.of() : new ArrayList<>(messages);
        } catch (Exception ex) {
            log.warn("failed to read session file={}, fallback empty", file, ex);
            return List.of();
        }
    }

    private void trimToSize(List<ChatMessageDto> history) {
        int overflow = history.size() - maxHistoryMessages;
        if (overflow > 0) {
            history.subList(0, overflow).clear();
        }
    }

    private static String sha256Hex(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            // Fallback: use hashCode if SHA-256 is unavailable
            return String.format("%08x", input.hashCode());
        }
    }
}
