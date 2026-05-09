package io.github.alcq77.cqagent.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.alcq77.cqagent.model.api.dto.ChatMessageDto;
import io.github.alcq77.cqagent.spi.session.ProductSessionStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

/**
 * 基于 Redis List 的会话存储实现。
 * <p>
 * - 每个 session 使用独立 key；
 * - 每次 append 后刷新 key 的 TTL；
 * - 读取失败时记录日志并返回空历史，避免影响主流程可用性。
 */
@Slf4j
public class RedisProductSessionStore implements ProductSessionStore, SessionStoreHealthProbe, SessionStoreMetricsProvider {

    /**
     * Redis 操作入口（字符串模板）。
     */
    private final StringRedisTemplate redisTemplate;
    /**
     * 消息序列化器。
     */
    private final ObjectMapper objectMapper;
    /**
     * session key 前缀。
     */
    private final String keyPrefix;
    /**
     * 会话 TTL。
     */
    private final Duration ttl;
    /**
     * 历史消息窗口上限。
     */
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
        Long size = redisTemplate.opsForList().size(key);
        if (size == null || size <= 0) {
            return List.of();
        }
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
        redisTemplate.opsForList().rightPush(key, encode(user));
        redisTemplate.opsForList().rightPush(key, encode(assistant));
        trimAndRefresh(key);
    }

    @Override
    public void replaceHistory(String sessionId, List<ChatMessageDto> messages) {
        // 覆盖式更新：先删后写，保持与 ChatMemoryStore.updateMessages 语义一致。
        String key = key(sessionId);
        redisTemplate.delete(key);
        List<ChatMessageDto> safeMessages = messages == null ? List.of() : messages;
        int start = Math.max(0, safeMessages.size() - maxHistoryMessages);
        for (int i = start; i < safeMessages.size(); i++) {
            redisTemplate.opsForList().rightPush(key, encode(safeMessages.get(i)));
        }
        trimAndRefresh(key);
    }

    @Override
    public void deleteSession(String sessionId) {
        redisTemplate.delete(key(sessionId));
    }

    /**
     * 统一裁剪窗口并刷新 TTL。
     */
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
            String pong = redisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
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

    @Override
    public List<String> listSessionIds() {
        Set<String> keys = redisTemplate.keys(keyPrefix + "*");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>(keys.size());
        for (String k : keys) {
            if (k.startsWith(keyPrefix)) {
                out.add(k.substring(keyPrefix.length()));
            }
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
