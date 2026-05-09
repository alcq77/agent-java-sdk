package io.github.alcq77.cqagent.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.alcq77.cqagent.model.api.dto.ChatMessageDto;
import io.github.alcq77.cqagent.spi.session.ProductSessionStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 基于 Redis List 的会话存储实现。
 * <p>
 * - 每个 session 使用独立 key；
 * - append 使用 Redis Pipeline 保证原子性；
 * - listSessionIds 使用 SCAN 替代 KEYS 避免生产环境阻塞。
 */
@Slf4j
public class RedisProductSessionStore implements ProductSessionStore, SessionStoreHealthProbe, SessionStoreMetricsProvider {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;
    private final Duration ttl;
    private final int maxHistoryMessages;

    public RedisProductSessionStore(StringRedisTemplate redisTemplate,
                                    ObjectMapper objectMapper,
                                    String keyPrefix,
                                    Duration ttl,
                                    int maxHistoryMessages) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.keyPrefix = (keyPrefix == null || keyPrefix.isBlank()) ? "cqagent:session:" : keyPrefix;
        this.ttl = ttl == null ? Duration.ofDays(7) : ttl;
        this.maxHistoryMessages = Math.max(10, maxHistoryMessages);
    }

    @Override
    public boolean hasSession(String sessionId) {
        Boolean existed = redisTemplate.hasKey(key(sessionId));
        return Boolean.TRUE.equals(existed);
    }

    @Override
    public void register(String sessionId) {
        // 懒加载：首次 append 时才真正创建会话 key。
    }

    @Override
    public List<ChatMessageDto> history(String sessionId) {
        String key = key(sessionId);
        List<String> payloads = redisTemplate.opsForList().range(key, 0, -1);
        if (payloads == null || payloads.isEmpty()) {
            return List.of();
        }
        List<ChatMessageDto> out = new ArrayList<>(payloads.size());
        for (String payload : payloads) {
            ChatMessageDto message = decode(payload);
            if (message != null) {
                out.add(message);
            }
        }
        return out;
    }

    @Override
    public void append(String sessionId, ChatMessageDto user, ChatMessageDto assistant) {
        String key = key(sessionId);
        // 使用 Pipeline 保证两条消息的原子性写入
        redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            connection.rPush(key.getBytes(), encode(user).getBytes(), encode(assistant).getBytes());
            return null;
        });
        trimAndRefresh(key);
    }

    @Override
    public void replaceHistory(String sessionId, List<ChatMessageDto> messages) {
        String key = key(sessionId);
        List<ChatMessageDto> safeMessages = messages == null ? List.of() : messages;
        int start = Math.max(0, safeMessages.size() - maxHistoryMessages);
        // 使用 Pipeline 原子执行：删除旧 key + 写入新数据
        List<String> payloads = new ArrayList<>();
        for (int i = start; i < safeMessages.size(); i++) {
            payloads.add(encode(safeMessages.get(i)));
        }
        byte[][] bytePayloads = payloads.stream().map(String::getBytes).toArray(byte[][]::new);
        redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
            connection.del(key.getBytes());
            if (bytePayloads.length > 0) {
                connection.rPush(key.getBytes(), bytePayloads);
            }
            return null;
        });
        trimAndRefresh(key);
    }

    @Override
    public void deleteSession(String sessionId) {
        redisTemplate.delete(key(sessionId));
    }

    private void trimAndRefresh(String key) {
        redisTemplate.opsForList().trim(key, -maxHistoryMessages, -1);
        if (!ttl.isZero() && !ttl.isNegative()) {
            redisTemplate.expire(key, ttl);
        }
    }

    private String key(String sessionId) {
        return keyPrefix + sessionId;
    }

    private String encode(ChatMessageDto message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize chat message", ex);
        }
    }

    private ChatMessageDto decode(String payload) {
        try {
            return objectMapper.readValue(payload, ChatMessageDto.class);
        } catch (Exception ex) {
            log.warn("skip invalid session payload in redis: {}", payload, ex);
            return null;
        }
    }

    @Override
    public boolean healthy() {
        try {
            String pong = redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<String>) connection -> connection.ping());
            return pong != null && "PONG".equalsIgnoreCase(pong);
        } catch (RedisConnectionFailureException ex) {
            log.warn("redis session store health check failed", ex);
            return false;
        } catch (Exception ex) {
            log.warn("redis session store health check got unexpected error", ex);
            return false;
        }
    }

    @Override
    public String detail() {
        return "redis";
    }

    @Override
    public boolean supportsSessionEnumeration() {
        return true;
    }

    /**
     * 使用 SCAN 替代 KEYS 命令，避免生产环境 Redis 阻塞。
     */
    @Override
    public List<String> listSessionIds() {
        List<String> out = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match(keyPrefix + "*").count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String k = cursor.next();
                if (k.startsWith(keyPrefix)) {
                    out.add(k.substring(keyPrefix.length()));
                }
            }
        } catch (Exception ex) {
            log.warn("failed to scan session keys", ex);
        }
        return out;
    }

    @Override
    public Map<String, Object> metrics() {
        return Map.of(
                "backend", "redis",
                "keyPrefix", keyPrefix,
                "ttlSeconds", ttl.getSeconds(),
                "maxHistoryMessages", maxHistoryMessages
        );
    }
}
