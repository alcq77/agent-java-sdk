package io.github.alcq77.cqagent.spi.session;

import io.github.alcq77.cqagent.model.api.dto.ChatMessageDto;

import java.util.Collections;
import java.util.List;

/**
 * 会话存储 SPI。
 * <p>
 * 职责边界：
 * - 定义会话历史读写契约，不限定具体存储介质（内存/文件/Redis/JDBC）；
 * - 为运行时会话记忆提供统一接口，保证核心链路可替换实现。
 */
public interface ProductSessionStore {

    /**
     * 判断会话是否已存在。
     */
    boolean hasSession(String sessionId);

    /**
     * 注册会话（允许幂等）。
     */
    void register(String sessionId);

    /**
     * 读取会话历史（按时间顺序）。
     */
    List<ChatMessageDto> history(String sessionId);

    /**
     * 追加一轮用户/助手消息。
     */
    void append(String sessionId, ChatMessageDto user, ChatMessageDto assistant);

    /**
     * 用完整历史覆盖会话。
     * <p>
     * 主要用于 LangChain4j ChatMemoryStore 的同步写入。
     */
    default void replaceHistory(String sessionId, List<ChatMessageDto> messages) {
        throw new UnsupportedOperationException("replaceHistory is not supported by " + getClass().getName());
    }

    /**
     * 删除会话及其历史。
     */
    default void deleteSession(String sessionId) {
        throw new UnsupportedOperationException("deleteSession is not supported by " + getClass().getName());
    }

    /**
     * 是否支持枚举会话 ID（用于跨存储迁移、运维巡检）。
     * <p>
     * 默认不支持；实现类在可安全枚举时应返回 {@code true}。
     */
    default boolean supportsSessionEnumeration() {
        return false;
    }

    /**
     * 枚举当前存储中的会话 ID；不支持时返回空列表。
     * <p>
     * 文件存储实现返回的是文件名中的「安全化」会话标识，可能与极少数含特殊字符的原始 ID 在字面上不一致。
     */
    default List<String> listSessionIds() {
        return Collections.emptyList();
    }
}
