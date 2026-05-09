package io.github.alcq77.cqagent.core.session;

import dev.langchain4j.data.message.*;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import io.github.alcq77.cqagent.model.api.dto.ChatMessageDto;
import io.github.alcq77.cqagent.spi.session.ProductSessionStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 将 cqagent 的 ProductSessionStore 适配为 LangChain4j ChatMemoryStore。
 */
public class ProductSessionStoreChatMemoryStore implements ChatMemoryStore {

    /**
     * 复用现有会话存储实现，保证四种存储后端都可直接继续工作。
     */
    private final ProductSessionStore sessionStore;

    public ProductSessionStoreChatMemoryStore(ProductSessionStore sessionStore) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        // DTO -> LangChain4j 消息对象
        return sessionStore.history(String.valueOf(memoryId)).stream()
                .map(ProductSessionStoreChatMemoryStore::toChatMessage)
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
                }
            }
        }
        // LangChain4j memory 快照落盘
        sessionStore.replaceHistory(String.valueOf(memoryId), converted);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        sessionStore.deleteSession(String.valueOf(memoryId));
    }

    public static ChatMessage toChatMessage(ChatMessageDto dto) {
        if (dto == null || dto.getRole() == null) {
            return UserMessage.from("");
        }
        return switch (dto.getRole()) {
            case "system" -> SystemMessage.from(dto.getContent());
            case "assistant" -> AiMessage.from(dto.getContent());
            default -> UserMessage.from(dto.getContent());
        };
    }

    public static ChatMessageDto toChatMessageDto(ChatMessage message) {
        if (message == null) {
            return null;
        }
        if (message instanceof SystemMessage systemMessage) {
            return ChatMessageDto.builder().role("system").content(systemMessage.text()).build();
        }
        if (message instanceof UserMessage userMessage) {
            return ChatMessageDto.builder().role("user").content(userMessage.singleText()).build();
        }
        if (message instanceof AiMessage aiMessage) {
            return ChatMessageDto.builder().role("assistant").content(aiMessage.text()).build();
        }
        if (message instanceof ToolExecutionResultMessage toolMessage) {
            return ChatMessageDto.builder()
                    .role("system")
                    .content("ToolResult(" + toolMessage.toolName() + "): " + toolMessage.text())
                    .build();
        }
        return null;
    }
}
