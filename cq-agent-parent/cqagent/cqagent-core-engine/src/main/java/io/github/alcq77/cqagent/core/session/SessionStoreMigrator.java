package io.github.alcq77.cqagent.core.session;

import io.github.alcq77.cqagent.model.api.dto.ChatMessageDto;
import io.github.alcq77.cqagent.spi.session.ProductSessionStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 跨 {@link ProductSessionStore} 迁移会话历史的辅助类。
 * <p>
 * 要求源存储 {@link ProductSessionStore#supportsSessionEnumeration()} 为 true，
 * 否则无法枚举待拷贝会话。
 */
public final class SessionStoreMigrator {

    private SessionStoreMigrator() {
    }

    /**
     * @param overwrite 目标已存在同名会话时是否覆盖
     */
    public static MigrationResult migrate(ProductSessionStore source,
                                          ProductSessionStore target,
                                          boolean overwrite) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        if (!source.supportsSessionEnumeration()) {
            throw new IllegalStateException("source session store does not support enumeration: " + source.getClass().getName());
        }
        List<String> ids = source.listSessionIds();
        int copied = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        for (String sessionId : ids) {
            try {
                if (target.hasSession(sessionId) && !overwrite) {
                    skipped++;
                    continue;
                }
                List<ChatMessageDto> history = source.history(sessionId);
                target.register(sessionId);
                target.replaceHistory(sessionId, history);
                copied++;
            } catch (RuntimeException ex) {
                errors.add(sessionId + ": " + ex.getMessage());
            }
        }
        return new MigrationResult(ids.size(), copied, skipped, errors);
    }

    /**
     * 迁移结果统计。
     *
     * @param scanned   源端枚举到的会话数
     * @param copied    实际写入目标的会话数
     * @param skipped   因已存在且未开启 overwrite 跳过的数量
     * @param errors    每条失败记录的简短说明
     */
    public record MigrationResult(int scanned, int copied, int skipped, List<String> errors) {
    }
}
