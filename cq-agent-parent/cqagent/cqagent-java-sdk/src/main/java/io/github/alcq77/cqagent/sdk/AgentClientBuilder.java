package io.github.alcq77.cqagent.sdk;

import io.github.alcq77.cqagent.core.agent.LangChain4jProductAgentRuntime;
import io.github.alcq77.cqagent.core.model.ModelDispatchPolicy;
import io.github.alcq77.cqagent.core.model.ProductModelRouter;
import io.github.alcq77.cqagent.core.model.RoutePolicy;
import io.github.alcq77.cqagent.core.observability.AgentRuntimeCounters;
import io.github.alcq77.cqagent.core.runtime.advisor.AgentRuntimeAdvisor;
import io.github.alcq77.cqagent.core.session.InMemoryProductSessionStore;
import io.github.alcq77.cqagent.sdk.internal.EmbeddedAgentClient;
import io.github.alcq77.cqagent.sdk.provider.AnthropicProductProvider;
import io.github.alcq77.cqagent.sdk.provider.BedrockProductProvider;
import io.github.alcq77.cqagent.sdk.provider.DashScopeProductProvider;
import io.github.alcq77.cqagent.sdk.provider.OllamaProductProvider;
import io.github.alcq77.cqagent.sdk.provider.OpenAiCompatibleProductProvider;
import io.github.alcq77.cqagent.sdk.provider.OpenAiProductProvider;
import io.github.alcq77.cqagent.sdk.provider.SubprocessModelBridgeProvider;
import io.github.alcq77.cqagent.sdk.tool.CurrentTimeProductTool;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqagent.spi.model.ProductModelProvider;
import io.github.alcq77.cqagent.spi.session.ProductSessionStore;
import io.github.alcq77.cqagent.spi.tool.ProductTool;

import java.util.*;

/**
 * SDK 主入口构建器。
 * <p>
 * 负责聚合：路由配置、provider、tool、session store 与 runtime 参数，
 * 最终构建可直接调用的 {@link AgentClient}。
 */
public class AgentClientBuilder {

    /**
     * SDK 运行参数集合。
     */
    private final ProductSdkOptions options = new ProductSdkOptions();
    /**
     * 显式注册的工具集合。
     */
    private final List<ProductTool> tools = new ArrayList<>();
    /**
     * provider 索引（key=providerCode）。
     */
    private final Map<String, ProductModelProvider> providers = new LinkedHashMap<>();
    /**
     * 会话存储（未指定时自动使用内存实现）。
     */
    private ProductSessionStore sessionStore;
    /**
     * endpoint 健康探测器（默认恒 true）。
     */
    private ProductModelRouter.EndpointHealthChecker healthChecker = endpoint -> true;

    /**
     * 创建 Builder。
     */
    public static AgentClientBuilder create() {
        return new AgentClientBuilder();
    }

    /**
     * 设置默认逻辑模型名。
     */
    public AgentClientBuilder logicalModel(String logicalModel) {
        options.setLogicalModel(logicalModel);
        return this;
    }

    /**
     * 设置会话最大历史消息数。
     */
    public AgentClientBuilder maxHistoryMessages(int maxHistoryMessages) {
        options.setMaxHistoryMessages(maxHistoryMessages);
        return this;
    }

    /**
     * 注册物理端点配置。
     */
    public AgentClientBuilder endpoint(ProductEndpointConfig endpoint) {
        options.getEndpoints().put(endpoint.getId(), endpoint);
        return this;
    }

    /**
     * 设置逻辑模型到端点的静态路由。
     */
    public AgentClientBuilder route(String logicalModel, String endpointId) {
        options.getRouting().put(logicalModel, endpointId);
        return this;
    }

    /**
     * 设置路由策略（主备/加权/健康感知）。
     */
    public AgentClientBuilder routePolicy(String logicalModel, RoutePolicy policy) {
        options.getRoutePolicies().put(logicalModel, policy);
        return this;
    }

    /**
     * 任务类型/标签分流策略。
     */
    public AgentClientBuilder modelDispatchPolicy(String policyId, ModelDispatchPolicy policy) {
        options.getModelDispatchPolicies().put(policyId, policy);
        return this;
    }

    /**
     * 设置工具回路最大迭代次数。
     */
    public AgentClientBuilder maxToolCallIterations(int maxIterations) {
        options.setMaxToolCallIterations(Math.max(1, maxIterations));
        return this;
    }

    /**
     * 配置端点熔断策略。
     */
    public AgentClientBuilder circuitBreaker(boolean enabled, int failureThreshold, int openSeconds) {
        options.setCircuitBreakerEnabled(enabled);
        options.setCircuitFailureThreshold(Math.max(1, failureThreshold));
        options.setCircuitOpenSeconds(Math.max(1, openSeconds));
        return this;
    }

    /**
     * 配置单次调用超时（毫秒）。
     */
    public AgentClientBuilder invokeTimeoutMillis(long timeoutMs) {
        options.setInvokeTimeoutMs(Math.max(0, timeoutMs));
        return this;
    }

    /**
     * 配置重试次数与退避时长。
     */
    public AgentClientBuilder retry(int maxRetries, long backoffMs) {
        options.setMaxRetries(Math.max(0, maxRetries));
        options.setRetryBackoffMs(Math.max(0, backoffMs));
        return this;
    }

    /**
     * 注册命名 Prompt 模板。
     */
    public AgentClientBuilder promptTemplate(String templateId, String systemPrompt, String userMessage) {
        if (templateId == null || templateId.isBlank()) {
            throw new IllegalArgumentException("templateId must not be blank");
        }
        ProductSdkOptions.PromptTemplate template = new ProductSdkOptions.PromptTemplate();
        template.setSystemPrompt(systemPrompt);
        template.setUserMessage(userMessage);
        options.getPromptTemplates().put(templateId.trim(), template);
        return this;
    }

    /**
     * 设置默认模板 ID。
     */
    public AgentClientBuilder defaultPromptTemplate(String templateId) {
        options.setDefaultPromptTemplateId(templateId);
        return this;
    }

    /**
     * 控制请求模板缺失时是否回退默认模板。
     */
    public AgentClientBuilder fallbackToDefaultPromptTemplate(boolean enabled) {
        options.setFallbackToDefaultPromptTemplate(enabled);
        return this;
    }

    /**
     * 指定会话存储实现。
     */
    public AgentClientBuilder sessionStore(ProductSessionStore store) {
        this.sessionStore = store;
        return this;
    }

    /**
     * 指定 endpoint 健康探测器。
     */
    public AgentClientBuilder healthChecker(ProductModelRouter.EndpointHealthChecker checker) {
        this.healthChecker = checker;
        return this;
    }

    /**
     * 注册业务工具。
     */
    public AgentClientBuilder tool(ProductTool tool) {
        this.tools.add(tool);
        return this;
    }

    /**
     * 注册模型 provider。
     */
    public AgentClientBuilder modelProvider(ProductModelProvider provider) {
        this.providers.put(provider.providerCode(), provider);
        return this;
    }

    /**
     * 注册运行时增强器。
     */
    public AgentClientBuilder advisor(AgentRuntimeAdvisor advisor) {
        if (advisor != null) {
            options.getRuntimeAdvisors().add(advisor);
        }
        return this;
    }

    /**
     * 构建最终 AgentClient。
     * <p>
     * 构建顺序：
     * 1) 合并显式注册与 SPI 自动发现；
     * 2) 兜底默认 provider/tool；
     * 3) 组装 router 与 LangChain4j runtime；
     * 4) 构建 EmbeddedAgentClient 外壳（重试/超时/熔断/指标）。
     */
    public AgentClient build() {
        ProductSessionStore store = sessionStore != null
                ? sessionStore
                : new InMemoryProductSessionStore(options.getMaxHistoryMessages());
        ServiceLoader.load(ProductTool.class).forEach(this::tool);
        ServiceLoader.load(ProductModelProvider.class).forEach(this::modelProvider);
        if (tools.isEmpty()) {
            tools.add(new CurrentTimeProductTool());
        }
        if (providers.isEmpty()) {
            ProductModelProvider openai = new OpenAiProductProvider();
            providers.put(openai.providerCode(), openai);
            ProductModelProvider openaiCompat = new OpenAiCompatibleProductProvider();
            providers.put(openaiCompat.providerCode(), openaiCompat);
            ProductModelProvider anthropic = new AnthropicProductProvider();
            providers.put(anthropic.providerCode(), anthropic);
            ProductModelProvider ollama = new OllamaProductProvider();
            providers.put(ollama.providerCode(), ollama);
            ProductModelProvider dashScope = new DashScopeProductProvider();
            providers.put(dashScope.providerCode(), dashScope);
            ProductModelProvider bedrock = new BedrockProductProvider();
            providers.put(bedrock.providerCode(), bedrock);
            ProductModelProvider pythonBridge = new SubprocessModelBridgeProvider();
            providers.put(pythonBridge.providerCode(), pythonBridge);
        }
        ProductModelRouter router = new ProductModelRouter(healthChecker);
        Map<String, LangChain4jProductAgentRuntime.PromptTemplate> promptTemplates = new LinkedHashMap<>();
        options.getPromptTemplates().forEach((templateId, template) -> promptTemplates.put(
                templateId,
                new LangChain4jProductAgentRuntime.PromptTemplate(template.getSystemPrompt(), template.getUserMessage())
        ));
        AgentRuntimeCounters runtimeCounters = new AgentRuntimeCounters();
        LangChain4jProductAgentRuntime runtime = new LangChain4jProductAgentRuntime(
                store,
                tools,
                options.getMaxToolCallIterations(),
                promptTemplates,
                options.getDefaultPromptTemplateId(),
            options.isFallbackToDefaultPromptTemplate(),
            runtimeCounters
        );
        return new EmbeddedAgentClient(options, router, runtime, providers);
    }
}
