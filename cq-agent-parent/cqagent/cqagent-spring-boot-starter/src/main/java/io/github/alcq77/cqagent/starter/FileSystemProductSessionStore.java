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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件系统会话存储实现，借鉴 JavaClaw 的 workspace 持久化思路。
 * <p>
 * 约定每个 session 一个 JSON 文件：{sessionDir}/chat-{sessionId}.json
 */
@Slf4j
public class FileSystemProductSessionStore implements ProductSessionStore, SessionStoreHealthProbe, SessionStoreMetricsProvider {

    private static final TypeReference<List<ChatMessageDto>> MESSAGE_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final Path sessionDir;
    private final int maxHistoryMessages;
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

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
        Path file = resolveFile(sessionId);
        if (Files.exists(file)) {
            return;
        }
        writeMessages(file, List.of());
    }

    @Override
    public List<ChatMessageDto> history(String sessionId) {
        Path file = resolveFile(sessionId);
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            String payload = Files.readString(file);
            List<ChatMessageDto> messages = objectMapper.readValue(payload, MESSAGE_LIST_TYPE);
            return messages == null ? List.of() : messages;
        } catch (Exception ex) {
            log.warn("failed to read session history from file={}, fallback empty", file, ex);
            return List.of();
        }
    }

    @Override
    public void append(String sessionId, ChatMessageDto user, ChatMessageDto assistant) {
        Object lock = locks.computeIfAbsent(sessionId, k -> new Object());
        synchronized (lock) {
            List<ChatMessageDto> history = new ArrayList<>(history(sessionId));
            history.add(user);
            history.add(assistant);
            int overflow = history.size() - maxHistoryMessages;
            if (overflow > 0) {
                history.subList(0, overflow).clear();
            }
            writeMessages(resolveFile(sessionId), history);
        }
    }

    @Override
    public void replaceHistory(String sessionId, List<ChatMessageDto> messages) {
        Object lock = locks.computeIfAbsent(sessionId, k -> new Object());
        synchronized (lock) {
            List<ChatMessageDto> history = messages == null ? new ArrayList<>() : new ArrayList<>(messages);
            int overflow = history.size() - maxHistoryMessages;
            if (overflow > 0) {
                history.subList(0, overflow).clear();
            }
            writeMessages(resolveFile(sessionId), history);
        }
    }

    @Override
    public void deleteSession(String sessionId) {
        try {
            Files.deleteIfExists(resolveFile(sessionId));
        } catch (Exception ex) {
            log.warn("failed to delete session file for sessionId={}", sessionId, ex);
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

    private Path resolveFile(String sessionId) {
        String safe = sessionId.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return sessionDir.resolve("chat-" + safe + ".json");
    }

    private void writeMessages(Path file, List<ChatMessageDto> messages) {
        try {
            ensureDirectory();
            String payload = objectMapper.writeValueAsString(messages);
            Files.writeString(file, payload, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to write session file: " + file, ex);
        }
    }
}
