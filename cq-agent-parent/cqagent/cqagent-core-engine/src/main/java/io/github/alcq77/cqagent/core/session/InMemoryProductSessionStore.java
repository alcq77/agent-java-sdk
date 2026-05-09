package io.github.alcq77.cqagent.core.session;

import io.github.alcq77.cqagent.model.api.dto.ChatMessageDto;
import io.github.alcq77.cqagent.spi.session.ProductSessionStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory session store using synchronized per-session locks.
 * <p>
 * Each session's message list is wrapped in {@code Collections.synchronizedList}
 * and explicitly synchronized during append/replace to prevent concurrent modification.
 */
public class InMemoryProductSessionStore implements ProductSessionStore {

    private final ConcurrentHashMap<String, List<ChatMessageDto>> sessions = new ConcurrentHashMap<>();
    private final int maxHistoryMessages;

    public InMemoryProductSessionStore(int maxHistoryMessages) {
        this.maxHistoryMessages = Math.max(10, maxHistoryMessages);
    }

    @Override
    public boolean hasSession(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    @Override
    public void register(String sessionId) {
        sessions.computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>()));
    }

    @Override
    public List<ChatMessageDto> history(String sessionId) {
        List<ChatMessageDto> history = sessions.get(sessionId);
        if (history == null) {
            return List.of();
        }
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    @Override
    public void append(String sessionId, ChatMessageDto user, ChatMessageDto assistant) {
        List<ChatMessageDto> history = sessions.computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (history) {
            history.add(user);
            history.add(assistant);
            int overflow = history.size() - maxHistoryMessages;
            if (overflow > 0) {
                history.subList(0, overflow).clear();
            }
        }
    }

    @Override
    public void replaceHistory(String sessionId, List<ChatMessageDto> messages) {
        List<ChatMessageDto> copy = messages == null ? new ArrayList<>() : new ArrayList<>(messages);
        int overflow = copy.size() - maxHistoryMessages;
        if (overflow > 0) {
            copy.subList(0, overflow).clear();
        }
        sessions.put(sessionId, Collections.synchronizedList(copy));
    }

    @Override
    public void deleteSession(String sessionId) {
        sessions.remove(sessionId);
    }

    @Override
    public boolean supportsSessionEnumeration() {
        return true;
    }

    @Override
    public List<String> listSessionIds() {
        return List.copyOf(sessions.keySet());
    }
}
