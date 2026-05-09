package io.github.alcq77.cqagent.sdk.internal;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import io.github.alcq77.cqagent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqagent.agent.api.dto.AgentChatResponse;
import io.github.alcq77.cqagent.agent.api.error.AgentErrorCode;
import io.github.alcq77.cqagent.agent.api.error.AgentRuntimeException;
import io.github.alcq77.cqagent.core.agent.LangChain4jProductAgentRuntime;
import io.github.alcq77.cqagent.core.model.ProductModelRouter;
import io.github.alcq77.cqagent.core.runtime.advisor.RuntimeAdvisorChain;
import io.github.alcq77.cqagent.core.tool.ProductToolRegistry;
import io.github.alcq77.cqagent.sdk.AgentClient;
import io.github.alcq77.cqagent.sdk.AgentStreamingListener;
import io.github.alcq77.cqagent.sdk.ProductSdkOptions;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqagent.spi.model.ProductModelProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

/**
 * 嵌入式 SDK 外壳：在统一路由、熔断、重试与 Advisor 链之上，分别调用运行时的同步 {@link LangChain4jProductAgentRuntime#chat}
 * 与流式 {@link LangChain4jProductAgentRuntime#stream}。
 * <p>
 * 流式并非“未实现”，而是经 {@link #stream} → {@link #invokeStreamingRuntime} 走 LangChain4j 的
 * {@link StreamingChatLanguageModel}；若 Provider 未声明 streaming 能力会在调用前失败。
 */
@Slf4j
public class EmbeddedAgentClient implements AgentClient, CircuitBreakerSnapshotProvider, AgentRuntimeMetricsProvider {

    private final ProductSdkOptions options;
    /**
     * 只负责 endpoint 选路，不负责模型调用细节。
     */
    private final ProductModelRouter modelRouter;
    /**
     * LangChain4j 基座运行时门面。
     */
    private final LangChain4jProductAgentRuntime runtime;
    private final Map<String, ProductModelProvider> providers;
    private final Map<String, EndpointCircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder totalFailures = new LongAdder();
    private final Map<String, LongAdder> endpointFailures = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> failureByType = new ConcurrentHashMap<>();
    private final LongAdder circuitSkipped = new LongAdder();
    private final RuntimeAdvisorChain advisorChain;

    public EmbeddedAgentClient(ProductSdkOptions options,
                               ProductModelRouter modelRouter,
                               LangChain4jProductAgentRuntime runtime,
                               Map<String, ProductModelProvider> providers) {
        this.options = options;
        this.modelRouter = modelRouter;
        this.runtime = runtime;
        this.providers = providers;
        this.advisorChain = new RuntimeAdvisorChain(options.getRuntimeAdvisors());
    }

    /**
     * 同步路径：endpoint 候选顺序内依次尝试；单次 endpoint 上按配置重试；成功则经 {@code advisorChain.after} 返回。
     */
    @Override
    public AgentChatResponse chat(AgentChatRequest request) {
        totalRequests.increment();
        runtime.runtimeCounters().incrementSyncChatInvocation();
        AgentChatRequest advised = advisorChain.before(request);
        String traceId = resolveTraceId(advised);
        String logicalModel = modelRouter.resolveLogicalModel(
            options.getLogicalModel(),
            options.getModelDispatchPolicies(),
            advised.getTaskType(),
            advised.getTags()
        );
        List<ProductEndpointConfig> candidates = modelRouter.resolveCandidates(
                logicalModel, options.getEndpoints(), options.getRouting(), options.getRoutePolicies());
        if (candidates.isEmpty()) {
            markFailure("route", "none", new IllegalStateException("no route found"));
        }
        RuntimeException last = null;
        for (ProductEndpointConfig endpoint : candidates) {
            // 熔断只作用于 endpoint 维度，避免单点持续雪崩。
            if (options.isCircuitBreakerEnabled() && !breaker(endpoint).allowRequest()) {
                circuitSkipped.increment();
                log.warn("traceId={} endpoint={} skipped by circuit breaker", traceId, endpoint.getId());
                continue;
            }
            ProductModelProvider provider = providers.get(endpoint.getProvider());
            if (provider == null) {
                last = new IllegalStateException("provider not found: " + endpoint.getProvider());
                markFailure("provider", endpoint.getId(), last);
                continue;
            }
            for (int attempt = 0; attempt <= options.getMaxRetries(); attempt++) {
                try {
                    // traceId 写入请求对象，便于下游日志与响应关联（流式路径同理）。
                    request.setTraceId(traceId);
                    advised.setTraceId(traceId);
                    AgentChatResponse response = invokeWithTimeout(advised, endpoint, provider, logicalModel);
                    breaker(endpoint).onSuccess();
                    response.setTraceId(traceId);
                    return advisorChain.after(advised, response);
                } catch (RuntimeException ex) {
                    breaker(endpoint).onFailure(ex.getMessage());
                    markFailure(classifyFailureType(ex), endpoint.getId(), ex);
                    last = ex;
                    if (attempt < options.getMaxRetries()) {
                        sleepBackoff();
                    }
                }
            }
        }
        RuntimeException wrapped = mapToStandardError(
            last == null ? new IllegalStateException("no route found") : last,
            traceId
        );
        throw advisorChain.onError(advised, wrapped);
    }

    /**
     * 流式路径：路由/熔断/重试策略与 {@link #chat} 对齐；成功后由 listener 异步接收 token，
     * 不在此方法返回完整 {@link AgentChatResponse}（见 listener 回调）。
     */
    @Override
    public void stream(AgentChatRequest request, AgentStreamingListener listener) {
        totalRequests.increment();
        runtime.runtimeCounters().incrementStreamingInvocation();
        AgentChatRequest advised = advisorChain.before(request);
        String traceId = resolveTraceId(advised);
        String logicalModel = modelRouter.resolveLogicalModel(
            options.getLogicalModel(),
            options.getModelDispatchPolicies(),
            advised.getTaskType(),
            advised.getTags()
        );
        List<ProductEndpointConfig> candidates = modelRouter.resolveCandidates(
            logicalModel, options.getEndpoints(), options.getRouting(), options.getRoutePolicies());
        if (candidates.isEmpty()) {
            RuntimeException ex = new IllegalStateException("no route found");
            markFailure("route", "none", ex);
            listener.onError(advisorChain.onError(advised, mapToStandardError(ex, traceId)));
            return;
        }
        RuntimeException last = null;
        for (ProductEndpointConfig endpoint : candidates) {
            if (options.isCircuitBreakerEnabled() && !breaker(endpoint).allowRequest()) {
                circuitSkipped.increment();
                continue;
            }
            ProductModelProvider provider = providers.get(endpoint.getProvider());
            if (provider == null) {
                last = new IllegalStateException("provider not found: " + endpoint.getProvider());
                markFailure("provider", endpoint.getId(), last);
                continue;
            }
            for (int attempt = 0; attempt <= options.getMaxRetries(); attempt++) {
                try {
                    advised.setTraceId(traceId);
                    // 流式一次成功即结束外层候选循环（token 已在回调中推送）。
                    invokeStreamingRuntime(advised, endpoint, provider, logicalModel, listener);
                    breaker(endpoint).onSuccess();
                    return;
                } catch (RuntimeException ex) {
                    breaker(endpoint).onFailure(ex.getMessage());
                    markFailure(classifyFailureType(ex), endpoint.getId(), ex);
                    last = ex;
                    if (attempt < options.getMaxRetries()) {
                        sleepBackoff();
                    }
                }
            }
        }
        RuntimeException wrapped = mapToStandardError(
            last == null ? new IllegalStateException("no route found") : last,
            traceId
        );
        listener.onError(advisorChain.onError(advised, wrapped));
    }

    private EndpointCircuitBreaker breaker(ProductEndpointConfig endpoint) {
        return circuitBreakers.computeIfAbsent(endpoint.getId(),
                k -> new EndpointCircuitBreaker(options.getCircuitFailureThreshold(), options.getCircuitOpenSeconds()));
    }

    @Override
    public Map<String, EndpointCircuitBreaker.Snapshot> circuitBreakerSnapshot() {
        Map<String, EndpointCircuitBreaker.Snapshot> out = new ConcurrentHashMap<>();
        circuitBreakers.forEach((k, v) -> out.put(k, v.snapshot()));
        return out;
    }

    @Override
    public Map<String, Object> runtimeMetrics() {
        Map<String, Long> failureByEndpoint = new ConcurrentHashMap<>();
        endpointFailures.forEach((k, v) -> failureByEndpoint.put(k, v.longValue()));
        Map<String, Long> typedFailures = new ConcurrentHashMap<>();
        failureByType.forEach((k, v) -> typedFailures.put(k, v.longValue()));
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalRequests", totalRequests.longValue());
        metrics.put("totalFailures", totalFailures.longValue());
        metrics.put("endpointFailures", failureByEndpoint);
        metrics.put("failureByType", typedFailures);
        metrics.put("circuitSkipped", circuitSkipped.longValue());
        metrics.put("promptTemplates", runtime.promptTemplateMetrics());
        metrics.putAll(runtime.runtimeCounters().snapshot());
        return metrics;
    }

    private static String resolveTraceId(AgentChatRequest request) {
        if (request.getTraceId() != null && !request.getTraceId().isBlank()) {
            return request.getTraceId().trim();
        }
        return UUID.randomUUID().toString();
    }

    private void markFailure(String type, String endpointId, RuntimeException ex) {
        totalFailures.increment();
        endpointFailures.computeIfAbsent(endpointId, k -> new LongAdder()).increment();
        failureByType.computeIfAbsent(type, k -> new LongAdder()).increment();
        log.warn("endpoint invoke failed, type={}, endpointId={}, message={}", type, endpointId, ex.getMessage());
    }

    private static String classifyFailureType(RuntimeException ex) {
        String raw = ex.getMessage() == null ? "" : ex.getMessage();
        if (raw.startsWith(ProductToolRegistry.PREFIX_VALIDATION)) {
            return "tool_validation";
        }
        if (raw.startsWith(ProductToolRegistry.PREFIX_EXECUTION)) {
            return "tool_execution";
        }
        String message = raw.toLowerCase();
        if (message.contains("timeout")) {
            return "timeout";
        }
        if (message.contains("tool")) {
            return "tool";
        }
        if (message.contains("route")) {
            return "route";
        }
        return "upstream";
    }

    private RuntimeException mapToStandardError(RuntimeException ex, String traceId) {
        if (ex instanceof AgentRuntimeException) {
            return ex;
        }
        String type = classifyFailureType(ex);
        AgentErrorCode code = switch (type) {
            case "route" -> AgentErrorCode.ROUTE_NOT_FOUND;
            case "provider" -> AgentErrorCode.PROVIDER_NOT_FOUND;
            case "timeout" -> AgentErrorCode.MODEL_INVOKE_TIMEOUT;
            case "tool_validation" -> AgentErrorCode.TOOL_VALIDATION_ERROR;
            case "tool_execution" -> AgentErrorCode.TOOL_EXECUTION_ERROR;
            default -> AgentErrorCode.MODEL_UPSTREAM_ERROR;
        };
        boolean retryable = code == AgentErrorCode.MODEL_INVOKE_TIMEOUT || code == AgentErrorCode.MODEL_UPSTREAM_ERROR;
        String userMessage = switch (code) {
            case ROUTE_NOT_FOUND -> "当前无可用模型路由，请联系管理员检查配置";
            case PROVIDER_NOT_FOUND -> "模型提供方未注册，请联系管理员";
            case TOOL_VALIDATION_ERROR -> "工具参数不合法，请调整请求后重试";
            case TOOL_EXECUTION_ERROR -> "工具执行失败，请稍后重试";
            case MODEL_INVOKE_TIMEOUT -> "模型响应超时，请稍后重试";
            default -> "模型调用失败，请稍后重试";
        };
        return new AgentRuntimeException(code, retryable, ex.getMessage(), userMessage, traceId, ex);
    }

    /**
     * 同步调用可选超时包装：流式路径当前不在此外壳层做同等超时（由 LangChain4j / 端点 readTimeout 约束）。
     */
    private AgentChatResponse invokeWithTimeout(AgentChatRequest request,
                                                ProductEndpointConfig endpoint,
                                                ProductModelProvider provider,
                                                String logicalModel) {
        long timeoutMs = options.getInvokeTimeoutMs();
        if (timeoutMs <= 0) {
            return invokeRuntime(request, endpoint, provider, logicalModel);
        }
        CompletableFuture<AgentChatResponse> future = CompletableFuture.supplyAsync(
                () -> invokeRuntime(request, endpoint, provider, logicalModel)
        );
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new IllegalStateException("invoke timeout after " + timeoutMs + "ms", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("invoke interrupted", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("invoke execution failed", cause);
        }
    }

    /** 构建非流式 {@link ChatLanguageModel} 并委托运行时同步主链。 */
    private AgentChatResponse invokeRuntime(AgentChatRequest request,
                                            ProductEndpointConfig endpoint,
                                            ProductModelProvider provider,
                                            String logicalModel) {
        if (!provider.capabilities().chat()) {
            throw new IllegalStateException("chat model is not supported by provider: " + provider.providerCode());
        }
        // 由 provider 生成 LangChain4j 模型，再交给 runtime 执行统一对话流程。
        ChatLanguageModel model = provider.createChatLanguageModel(endpoint, logicalModel);
        return runtime.chat(request, model, logicalModel);
    }

    /** 构建 {@link StreamingChatLanguageModel} 并委托运行时流式主链（含工具多轮时的后续 generate）。 */
    private void invokeStreamingRuntime(AgentChatRequest request,
                                        ProductEndpointConfig endpoint,
                                        ProductModelProvider provider,
                                        String logicalModel,
                                        AgentStreamingListener listener) {
        if (!provider.capabilities().streaming()) {
            throw new IllegalStateException("streaming model is not supported by provider: " + provider.providerCode());
        }
        StreamingChatLanguageModel model = provider.createStreamingChatLanguageModel(endpoint, logicalModel);
        runtime.stream(
            request,
            model,
            logicalModel,
            listener::onToken,
            response -> {
                response.setTraceId(request.getTraceId());
                listener.onComplete(response);
            },
            listener::onError
        );
    }

    private void sleepBackoff() {
        if (options.getRetryBackoffMs() <= 0) {
            return;
        }
        try {
            Thread.sleep(options.getRetryBackoffMs());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
