package io.github.alcq77.cqagent.starter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.alcq77.cqagent.model.api.dto.ChatMessageDto;
import io.github.alcq77.cqagent.spi.session.ProductSessionStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 JDBC 的会话存储实现。
 * <p>
 * 采用单行 JSON payload 存储会话历史，优先保证跨数据库接入简单性：
 * - hasSession/history 使用 select；
 * - append 使用事务内「读-改-写」；
 * - 可选自动建表（create table if not exists）。
 */
@Slf4j
public class JdbcProductSessionStore implements ProductSessionStore, SessionStoreHealthProbe, SessionStoreMetricsProvider {

    private static final TypeReference<List<ChatMessageDto>> MESSAGE_LIST_TYPE = new TypeReference<>() {};

    /**
     * 数据库访问模板。
     */
    private final JdbcTemplate jdbcTemplate;
    /**
     * 消息序列化器。
     */
    private final ObjectMapper objectMapper;
    /**
     * 事务模板，保证读改写一致性。
     */
    private final TransactionTemplate transactionTemplate;
    /**
     * 存储表名（已做白名单校验）。
     */
    private final String table;
    /**
     * 历史消息窗口上限。
     */
    private final int maxHistoryMessages;
    /**
     * 是否允许启动自动建表。
     */
    private final boolean autoInit;
    private final AtomicBoolean initDone = new AtomicBoolean(false);

    public JdbcProductSessionStore(JdbcTemplate jdbcTemplate,
                                   ObjectMapper objectMapper,
                                   PlatformTransactionManager transactionManager,
                                   String table,
                                   int maxHistoryMessages,
                                   boolean autoInit) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.transactionTemplate = new TransactionTemplate(Objects.requireNonNull(transactionManager, "transactionManager"));
        this.table = sanitizeTable(table);
        this.maxHistoryMessages = Math.max(10, maxHistoryMessages);
        this.autoInit = autoInit;
        ensureTable();
    }

    @Override
    public boolean hasSession(String sessionId) {
        ensureTable();
        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from " + table + " where session_id = ?",
                Integer.class,
                sessionId
        );
        return count != null && count > 0;
    }

    @Override
    public void register(String sessionId) {
        ensureTable();
        try {
            jdbcTemplate.update(
                    "insert into " + table + " (session_id, payload) values (?, ?)",
                    sessionId,
                    "[]"
            );
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // Another thread inserted the same session — ignore.
        }
    }

    @Override
    public List<ChatMessageDto> history(String sessionId) {
        ensureTable();
        List<String> payloadList = jdbcTemplate.query(
                "select payload from " + table + " where session_id = ?",
                (rs, rowNum) -> rs.getString("payload"),
                sessionId
        );
        if (payloadList.isEmpty()) {
            return List.of();
        }
        return decode(payloadList.get(0));
    }

    @Override
    public void append(String sessionId, ChatMessageDto user, ChatMessageDto assistant) {
        ensureTable();
        transactionTemplate.executeWithoutResult(status -> {
            List<ChatMessageDto> history = new ArrayList<>(history(sessionId));
            history.add(user);
            history.add(assistant);
            int overflow = history.size() - maxHistoryMessages;
            if (overflow > 0) {
                history.subList(0, overflow).clear();
            }
            String payload = encode(history);
            int updated = jdbcTemplate.update(
                    "update " + table + " set payload = ? where session_id = ?",
                    payload,
                    sessionId
            );
            if (updated == 0) {
                jdbcTemplate.update(
                        "insert into " + table + " (session_id, payload) values (?, ?)",
                        sessionId,
                        payload
                );
            }
        });
    }

    @Override
    public void replaceHistory(String sessionId, List<ChatMessageDto> messages) {
        ensureTable();
        transactionTemplate.executeWithoutResult(status -> {
            // 覆盖式更新，符合 ChatMemoryStore.updateMessages 语义。
            List<ChatMessageDto> history = messages == null ? new ArrayList<>() : new ArrayList<>(messages);
            int overflow = history.size() - maxHistoryMessages;
            if (overflow > 0) {
                history.subList(0, overflow).clear();
            }
            String payload = encode(history);
            int updated = jdbcTemplate.update(
                    "update " + table + " set payload = ? where session_id = ?",
                    payload,
                    sessionId
            );
            if (updated == 0) {
                jdbcTemplate.update(
                        "insert into " + table + " (session_id, payload) values (?, ?)",
                        sessionId,
                        payload
                );
            }
        });
    }

    @Override
    public void deleteSession(String sessionId) {
        ensureTable();
        jdbcTemplate.update("delete from " + table + " where session_id = ?", sessionId);
    }

    /**
     * 延迟初始化表结构，避免无用 DDL。
     */
    private void ensureTable() {
        if (!autoInit || initDone.get()) {
            return;
        }
        synchronized (this) {
            if (initDone.get()) {
                return;
            }
            jdbcTemplate.execute(
                    "create table if not exists " + table + " (" +
                            "session_id varchar(128) primary key, " +
                            "payload text not null, " +
                            "updated_at timestamp default current_timestamp" +
                            ")"
            );
            initDone.set(true);
            log.info("jdbc session store ready, table={}", table);
        }
    }

    private String encode(List<ChatMessageDto> messages) {
        try {
            return objectMapper.writeValueAsString(messages);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize session history", ex);
        }
    }

    private List<ChatMessageDto> decode(String payload) {
        try {
            List<ChatMessageDto> list = objectMapper.readValue(payload, MESSAGE_LIST_TYPE);
            return list == null ? List.of() : list;
        } catch (Exception ex) {
            log.warn("invalid jdbc session payload, fallback to empty history", ex);
            return List.of();
        }
    }

    private String sanitizeTable(String table) {
        // 仅允许字母/数字/下划线，防止 SQL 注入型表名拼接风险。
        String value = (table == null || table.isBlank()) ? "cqagent_session_store" : table.trim();
        if (!value.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Invalid jdbc table name: " + value);
        }
        return value;
    }

    @Override
    public boolean healthy() {
        try {
            Integer one = jdbcTemplate.queryForObject("select 1", Integer.class);
            return one != null && one == 1;
        } catch (Exception ex) {
            log.warn("jdbc session store health check failed", ex);
            return false;
        }
    }

    @Override
    public String detail() {
        return "jdbc:" + table;
    }

    @Override
    public boolean supportsSessionEnumeration() {
        return true;
    }

    @Override
    public List<String> listSessionIds() {
        ensureTable();
        return jdbcTemplate.queryForList("select session_id from " + table, String.class);
    }

    @Override
    public Map<String, Object> metrics() {
        return Map.of(
                "backend", "jdbc",
                "table", table,
                "autoInit", autoInit,
                "maxHistoryMessages", maxHistoryMessages
        );
    }
}
