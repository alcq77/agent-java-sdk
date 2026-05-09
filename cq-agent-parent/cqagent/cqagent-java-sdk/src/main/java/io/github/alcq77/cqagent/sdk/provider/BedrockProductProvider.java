package io.github.alcq77.cqagent.sdk.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import io.github.alcq77.cqagent.model.api.spi.ModelProviderCodes;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqagent.spi.model.ProductModelProvider;
import io.github.alcq77.cqagent.spi.model.ProductProviderCapabilities;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * AWS Bedrock {@link ChatLanguageModel} 适配（InvokeModel + Claude Messages 格式）。
 * <p>
 * 凭证：仅使用 AWS SDK 默认凭证链（环境变量、共享凭证文件、实例角色等），可选 {@code headers["aws.profile"]}
 * 指定本地 profile；不在 headers 中读取明文 Secret。
 */
public class BedrockProductProvider implements ProductModelProvider {

    private static final String DEFAULT_REGION = "us-east-1";
    private static final String ANTHROPIC_VERSION = "bedrock-2023-05-31";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Override
    public String providerCode() {
        return ModelProviderCodes.BEDROCK;
    }

    @Override
    public ProductProviderCapabilities capabilities() {
        return ProductProviderCapabilities.chatOnly().withToolCalling(true);
    }

    @Override
    public ChatLanguageModel createChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        return new BedrockChatLanguageModel(endpoint, logicalModel);
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        throw new UnsupportedOperationException(
            "Bedrock streaming is not implemented in this provider; use synchronous chat() or another provider.");
    }

    static String resolveModelName(ProductEndpointConfig endpoint, String logicalModel) {
        return endpoint.getDefaultModel() == null || endpoint.getDefaultModel().isBlank()
            ? logicalModel
            : endpoint.getDefaultModel();
    }

    static Region resolveRegion(ProductEndpointConfig endpoint) {
        String region = endpoint.getHeaders() != null ? endpoint.getHeaders().get("region") : null;
        if (region == null || region.isBlank()) {
            return Region.of(DEFAULT_REGION);
        }
        return Region.of(region.trim());
    }

    /**
     * 解析凭证：禁止从 headers 读取 access-key/secret-key（避免泄露到日志）；仅 profile 或默认链。
     */
    static AwsCredentialsProvider resolveCredentialsProvider(ProductEndpointConfig endpoint) {
        String profile = endpoint.getHeaders() != null ? endpoint.getHeaders().get("aws.profile") : null;
        if (profile != null && !profile.isBlank()) {
            return ProfileCredentialsProvider.create(profile.trim());
        }
        return DefaultCredentialsProvider.create();
    }

    /**
     * 从 Bedrock InvokeModel 返回体中提取助手文本或 tool_use（供单测覆盖）。
     */
    static AiMessage parseAnthropicResponseBody(String responseBody) {
        try {
            JsonNode root = JSON.readTree(responseBody);
            JsonNode content = root.get("content");
            if (content == null || !content.isArray()) {
                JsonNode text = root.get("completion");
                if (text != null && text.isTextual()) {
                    return AiMessage.from(text.asText());
                }
                throw new IllegalStateException("Bedrock response missing content array: " + truncate(responseBody));
            }
            StringBuilder textBuf = new StringBuilder();
            List<ToolExecutionRequest> toolCalls = new ArrayList<>();
            for (JsonNode block : content) {
                String type = textOrEmpty(block.get("type"));
                if ("text".equals(type)) {
                    textBuf.append(textOrEmpty(block.get("text")));
                } else if ("tool_use".equals(type)) {
                    String id = textOrEmpty(block.get("id"));
                    String name = textOrEmpty(block.get("name"));
                    JsonNode input = block.get("input");
                    String args = input == null || input.isNull() ? "{}" : input.toString();
                    toolCalls.add(ToolExecutionRequest.builder()
                        .id(id)
                        .name(name)
                        .arguments(args)
                        .build());
                }
            }
            if (!toolCalls.isEmpty()) {
                if (textBuf.isEmpty()) {
                    return toolCalls.size() == 1
                        ? AiMessage.from(toolCalls.get(0))
                        : AiMessage.from(toolCalls);
                }
                return AiMessage.from(textBuf.toString(), toolCalls);
            }
            return AiMessage.from(textBuf.toString());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Bedrock response JSON", e);
        }
    }

    private static String textOrEmpty(JsonNode n) {
        return n == null || n.isNull() ? "" : n.asText("");
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }

    private static final class BedrockChatLanguageModel implements ChatLanguageModel {

        private final ProductEndpointConfig endpoint;
        private final String modelName;

        BedrockChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
            this.endpoint = endpoint;
            this.modelName = resolveModelName(endpoint, logicalModel);
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages) {
            return generate(messages, List.of());
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
            ObjectNode body = buildRequestBody(messages, toolSpecifications);
            try (BedrockRuntimeClient client = BedrockRuntimeClient.builder()
                .region(resolveRegion(endpoint))
                .credentialsProvider(resolveCredentialsProvider(endpoint))
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                    .apiCallTimeout(endpoint.getReadTimeout())
                    .build())
                .build()) {

                InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelName)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(JSON.writeValueAsString(body)))
                    .build();

                InvokeModelResponse response = client.invokeModel(request);
                String raw = response.body().asUtf8String();
                AiMessage ai = parseAnthropicResponseBody(raw);
                return Response.from(ai);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("Bedrock invoke failed: " + e.getMessage(), e);
            }
        }

        /**
         * 组装 Claude Messages API（Bedrock InvokeModel）请求体。
         */
        private ObjectNode buildRequestBody(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
            ObjectNode root = JSON.createObjectNode();
            root.put("anthropic_version", ANTHROPIC_VERSION);

            int maxTokens = 4096;
            if (endpoint.getHeaders() != null && endpoint.getHeaders().get("max_tokens") != null) {
                try {
                    maxTokens = Integer.parseInt(endpoint.getHeaders().get("max_tokens").trim());
                } catch (NumberFormatException ignored) {
                    // keep default
                }
            }
            root.put("max_tokens", maxTokens);

            StringBuilder systemBuf = new StringBuilder();
            ArrayNode messagesArr = JSON.createArrayNode();
            for (ChatMessage m : messages) {
                if (m instanceof SystemMessage sm) {
                    if (!systemBuf.isEmpty()) {
                        systemBuf.append('\n');
                    }
                    systemBuf.append(sm.text());
                    continue;
                }
                if (m instanceof UserMessage um) {
                    if (!um.hasSingleText()) {
                        throw new IllegalArgumentException(
                            "Bedrock adapter supports single-text UserMessage only (no multimodal contents)");
                    }
                    ObjectNode msg = JSON.createObjectNode();
                    msg.put("role", "user");
                    msg.set("content", textBlockArray(um.singleText()));
                    messagesArr.add(msg);
                    continue;
                }
                if (m instanceof AiMessage am) {
                    ObjectNode msg = JSON.createObjectNode();
                    msg.put("role", "assistant");
                    msg.set("content", assistantBlocks(am));
                    messagesArr.add(msg);
                    continue;
                }
                if (m instanceof ToolExecutionResultMessage tr) {
                    ObjectNode msg = JSON.createObjectNode();
                    msg.put("role", "user");
                    ArrayNode content = JSON.createArrayNode();
                    ObjectNode toolResult = JSON.createObjectNode();
                    toolResult.put("type", "tool_result");
                    toolResult.put("tool_use_id", tr.id());
                    toolResult.put("content", tr.text());
                    content.add(toolResult);
                    msg.set("content", content);
                    messagesArr.add(msg);
                }
            }
            root.set("messages", messagesArr);
            if (!systemBuf.isEmpty()) {
                root.put("system", systemBuf.toString());
            }
            if (toolSpecifications != null && !toolSpecifications.isEmpty()) {
                ArrayNode tools = JSON.createArrayNode();
                for (ToolSpecification spec : toolSpecifications) {
                    tools.add(toolSpecificationToClaudeTool(spec));
                }
                root.set("tools", tools);
            }
            return root;
        }

        private static ArrayNode textBlockArray(String text) {
            ArrayNode arr = JSON.createArrayNode();
            ObjectNode block = JSON.createObjectNode();
            block.put("type", "text");
            block.put("text", text == null ? "" : text);
            arr.add(block);
            return arr;
        }

        private static ArrayNode assistantBlocks(AiMessage am) {
            ArrayNode arr = JSON.createArrayNode();
            if (am.text() != null && !am.text().isBlank()) {
                ObjectNode textBlock = JSON.createObjectNode();
                textBlock.put("type", "text");
                textBlock.put("text", am.text());
                arr.add(textBlock);
            }
            if (am.hasToolExecutionRequests()) {
                @SuppressWarnings("unchecked")
                List<ToolExecutionRequest> reqs = am.toolExecutionRequests();
                for (ToolExecutionRequest req : reqs) {
                    ObjectNode tu = JSON.createObjectNode();
                    tu.put("type", "tool_use");
                    tu.put("id", req.id());
                    tu.put("name", req.name());
                    try {
                        tu.set("input", JSON.readTree(req.arguments() == null ? "{}" : req.arguments()));
                    } catch (Exception e) {
                        tu.set("input", JSON.createObjectNode());
                    }
                    arr.add(tu);
                }
            }
            return arr;
        }

        private static ObjectNode toolSpecificationToClaudeTool(ToolSpecification spec) {
            ObjectNode tool = JSON.createObjectNode();
            tool.put("name", spec.name());
            tool.put("description", spec.description() == null ? "" : spec.description());
            JsonNode schema = jsonObjectSchemaToJson(spec);
            tool.set("input_schema", schema);
            return tool;
        }

        /** 将 LangChain4j JsonObjectSchema 转为 JSON Schema 风格的 input_schema。 */
        private static JsonNode jsonObjectSchemaToJson(ToolSpecification spec) {
            try {
                if (spec.parameters() != null) {
                    JsonNode n = JSON.valueToTree(spec.parameters());
                    if (n != null && !n.isNull()) {
                        return n;
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
            ObjectNode fallback = JSON.createObjectNode();
            fallback.put("type", "object");
            fallback.set("properties", JSON.createObjectNode());
            return fallback;
        }
    }
}
