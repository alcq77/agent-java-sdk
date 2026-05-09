package io.github.alcq77.cqagent.core.session;

import dev.langchain4j.data.message.*;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.github.alcq77.cqagent.model.api.dto.ChatMessageDto;
import io.github.alcq77.cqagent.spi.session.ProductSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 将 cqagent 的 ProductSessionStore 适配为 LangChain4j ChatMemoryStore。
 * <p>
 * ToolExecutionResultMessage 使用特殊 JSON 格式序列化以保留 tool call ID，
 * 反序列化时自动还原为 ToolExecutionResultMessage。
 */
public class ProductSessionStoreChatMemoryStore implements ChatMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(ProductSessionStoreChatMemoryStore.class);
    private static final String TOOL_RESULT_PREFIX = "__TOOL_RESULT__:";

    private final ProductSessionStore sessionStore;

    public ProductSessionStoreChatMemoryStore(ProductSessionStore sessionStore) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        return sessionStore.history(String.valueOf(memoryId)).stream()
                .map(ProductSessionStoreChatMemoryStore::toChatMessage)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        List<ChatMessageDto> converted = new ArrayList<>();
        if (messages != null) {
            for (ChatMessage message : messages) {
                ChatMessageDto dto = toChatMessageDto(message);
                if (dto != null) {
                    converted.add(dto);
                } else {
                    log.debug("skipping unrecognized message type: {}", message.getClass().getSimpleName());
                }
            }
        }
        sessionStore.replaceHistory(String.valueOf(memoryId), converted);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        sessionStore.deleteSession(String.valueOf(memoryId));
    }

    /**
     * Converts a DTO back to a LangChain4j ChatMessage.
     * Preserves ToolExecutionResultMessage type information.
     */
    public static ChatMessage toChatMessage(ChatMessageDto dto) {
        if (dto == null || dto.getRole() == null) {
            return null;
        }
        return switch (dto.getRole()) {
            case "system" -> {
                String content = dto.getContent();
                if (content != null && content.startsWith(TOOL_RESULT_PREFIX)) {
                    yield parseToolResult(content);
                }
                yield SystemMessage.from(content == null ? "" : content);
            }
            case "assistant" -> AiMessage.from(dto.getContent() == null ? "" : dto.getContent());
            case "user" -> UserMessage.from(dto.getContent() == null ? "" : dto.getContent());
            default -> {
                log.warn("unknown message role: {}", dto.getRole());
                yield null;
            }
        };
    }

    /**
     * Converts a LangChain4j ChatMessage to a DTO for persistence.
     * ToolExecutionResultMessage is serialized with special prefix to preserve metadata.
     */
    public static ChatMessageDto toChatMessageDto(ChatMessage message) {
        if (message == null) {
            return null;
        }
        if (message instanceof SystemMessage systemMessage) {
            return ChatMessageDto.builder().role("system").content(systemMessage.text()).build();
        }
        if (message instanceof UserMessage userMessage) {
            String text = userMessage.singleText();
            return ChatMessageDto.builder().role("user").content(text == null ? "" : text).build();
        }
        if (message instanceof AiMessage aiMessage) {
            return ChatMessageDto.builder().role("assistant").content(aiMessage.text()).build();
        }
        if (message instanceof ToolExecutionResultMessage toolMessage) {
            String id = toolMessage.id() == null ? "" : toolMessage.id();
            String name = toolMessage.toolName() == null ? "" : toolMessage.toolName();
            String text = toolMessage.text() == null ? "" : toolMessage.text();
            // Store as system message with metadata prefix for round-trip preservation
            return ChatMessageDto.builder()
                    .role("system")
                    .content(TOOL_RESULT_PREFIX + id + "|" + name + "|" + text)
                    .build();
        }
        return null;
    }

    /**
     * Parses a tool result from the serialized format: __TOOL_RESULT__:{id}|{name}|{text}
     */
    private static ToolExecutionResultMessage parseToolResult(String content) {
        String payload = content.substring(TOOL_RESULT_PREFIX.length());
        int firstSep = payload.indexOf('|');
        int secondSep = firstSep >= 0 ? payload.indexOf('|', firstSep + 1) : -1;
        if (firstSep < 0 || secondSep < 0) {
            // Legacy format or malformed — fallback to SystemMessage
            return null;
        }
        String id = payload.substring(0, firstSep);
        String name = payload.substring(firstSep + 1, secondSep);
        String text = payload.substring(secondSep + 1);
        return ToolExecutionResultMessage.from(id, name, text);
    }
}
