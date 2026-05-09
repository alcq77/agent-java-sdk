package io.github.alcq77.cqagent.sdk.provider;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import io.github.alcq77.cqagent.model.api.spi.ModelProviderCodes;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqagent.spi.model.ProductProviderCapabilities;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BedrockProductProviderTest {

    @Test
    void providerMetadata() {
        BedrockProductProvider provider = new BedrockProductProvider();

        assertEquals(ModelProviderCodes.BEDROCK, provider.providerCode());
        assertEquals(ProductProviderCapabilities.chatOnly().withToolCalling(true), provider.capabilities());
    }

    @Test
    void createChatLanguageModel() {
        BedrockProductProvider provider = new BedrockProductProvider();
        ProductEndpointConfig endpoint = ProductEndpointConfig.builder()
            .id("bedrock-primary")
            .provider(ModelProviderCodes.BEDROCK)
            .defaultModel("anthropic.claude-3-5-sonnet-20240620-v1:0")
            .headers(Map.of("region", "us-east-1"))
            .build();

        ChatLanguageModel model = provider.createChatLanguageModel(endpoint, "primary-llm");
        assertNotNull(model);
    }

    @Test
    void createStreamingChatLanguageModelThrows() {
        BedrockProductProvider provider = new BedrockProductProvider();
        ProductEndpointConfig endpoint = ProductEndpointConfig.builder()
            .id("bedrock-primary")
            .provider(ModelProviderCodes.BEDROCK)
            .defaultModel("anthropic.claude-3-5-sonnet-20240620-v1:0")
            .headers(Map.of("region", "us-east-1"))
            .build();

        assertThrows(UnsupportedOperationException.class,
            () -> provider.createStreamingChatLanguageModel(endpoint, "primary-llm"));
    }

    @Test
    void parseAssistantTextFromContentBlocks() {
        String json = """
            {"content":[{"type":"text","text":"hello"}],"stop_reason":"end_turn"}
            """;
        AiMessage msg = BedrockProductProvider.parseAnthropicResponseBody(json);
        assertEquals("hello", msg.text());
        assertFalse(msg.hasToolExecutionRequests());
    }

    @Test
    void parseToolUseBlocks() {
        String json = """
            {"content":[{"type":"tool_use","id":"toolu_1","name":"current_time","input":{}}],"stop_reason":"tool_use"}
            """;
        AiMessage msg = BedrockProductProvider.parseAnthropicResponseBody(json);
        assertTrue(msg.hasToolExecutionRequests());
        assertEquals(1, msg.toolExecutionRequests().size());
        ToolExecutionRequest req = msg.toolExecutionRequests().get(0);
        assertEquals("current_time", req.name());
    }
}
