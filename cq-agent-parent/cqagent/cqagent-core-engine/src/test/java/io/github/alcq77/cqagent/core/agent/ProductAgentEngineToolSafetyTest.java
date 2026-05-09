package io.github.alcq77.cqagent.core.agent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import io.github.alcq77.cqagent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqagent.core.agent.LangChain4jProductAgentRuntime;
import io.github.alcq77.cqagent.core.observability.AgentRuntimeCounters;
import io.github.alcq77.cqagent.core.session.InMemoryProductSessionStore;
import io.github.alcq77.cqagent.core.tool.ProductToolRegistry;
import io.github.alcq77.cqagent.spi.tool.ProductTool;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductAgentEngineToolSafetyTest {

    @Test
    void shouldRejectUnknownToolNameFromModelOutput() {
        LangChain4jProductAgentRuntime runtime = new LangChain4jProductAgentRuntime(
            new InMemoryProductSessionStore(20),
            List.of(new EchoTool()),
            3,
            null,
            null,
            false,
            new AgentRuntimeCounters()
        );

        ChatLanguageModel mockModel = new MockChatLanguageModelWithUnknownToolName();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> runtime.chat(
            AgentChatRequest.builder().message("run tool").build(),
            mockModel,
            "primary-llm"
        ));
        assertTrue(ex.getMessage().contains(ProductToolRegistry.PREFIX_VALIDATION));
    }

    private static class MockChatLanguageModelWithUnknownToolName implements ChatLanguageModel {
        @Override
        public dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> generate(java.util.List<dev.langchain4j.data.message.ChatMessage> messages) {
            return dev.langchain4j.model.output.Response.from(
                dev.langchain4j.data.message.AiMessage.from("fallback")
            );
        }

        @Override
        public dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> generate(
            java.util.List<dev.langchain4j.data.message.ChatMessage> messages,
            java.util.List<dev.langchain4j.agent.tool.ToolSpecification> toolSpecifications
        ) {
            dev.langchain4j.agent.tool.ToolExecutionRequest toolRequest =
                dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                    .name("missing_tool")
                    .arguments("{}")
                    .id("test-id")
                    .build();

            dev.langchain4j.data.message.AiMessage aiMessage =
                dev.langchain4j.data.message.AiMessage.from(List.of(toolRequest));

            return dev.langchain4j.model.output.Response.from(aiMessage);
        }
    }

    private static class EchoTool implements ProductTool {
        @Override
        public String name() {
            return "echo";
        }

        @Override
        public boolean supports(String userInput) {
            return true;
        }

        @Override
        public String execute(String userInput) {
            return userInput;
        }
    }
}
