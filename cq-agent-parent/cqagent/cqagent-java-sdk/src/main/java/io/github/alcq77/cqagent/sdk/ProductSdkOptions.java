package io.github.alcq77.cqagent.sdk;

import io.github.alcq77.cqagent.core.model.ModelDispatchPolicy;
import io.github.alcq77.cqagent.core.model.RoutePolicy;
import io.github.alcq77.cqagent.core.runtime.advisor.AgentRuntimeAdvisor;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ProductSdkOptions {

    private String logicalModel = "primary-llm";

    private int maxHistoryMessages = 40;

    private Map<String, ProductEndpointConfig> endpoints = new LinkedHashMap<>();

    private Map<String, String> routing = new LinkedHashMap<>();

    private Map<String, RoutePolicy> routePolicies = new LinkedHashMap<>();

    /**
     * 任务维度模型分发策略（key=policyId）。
     */
    private Map<String, ModelDispatchPolicy> modelDispatchPolicies = new LinkedHashMap<>();

    /**
     * Prompt 模板仓库（key=templateId）。
     */
    private Map<String, PromptTemplate> promptTemplates = new LinkedHashMap<>();

    /**
     * 默认模板 ID（当请求未指定 promptTemplateId 时使用）。
     */
    private String defaultPromptTemplateId;

    /**
     * 当请求模板不存在时，是否回退到默认模板。
     */
    private boolean fallbackToDefaultPromptTemplate = true;

    /**
     * tool-calling 最大回合数，防止模型与工具进入无限循环。
     */
    private int maxToolCallIterations = 3;

    /**
     * 是否启用端点熔断。
     */
    private boolean circuitBreakerEnabled = true;

    /**
     * 连续失败达到阈值后打开熔断。
     */
    private int circuitFailureThreshold = 3;

    /**
     * 熔断打开后冷却秒数。
     */
    private int circuitOpenSeconds = 30;

    /**
     * 单次模型调用超时时间（毫秒），<=0 表示不设置超时。
     */
    private long invokeTimeoutMs = 60_000;

    /**
     * 单端点失败后的最大重试次数（不含首次调用）。
     */
    private int maxRetries = 1;

    /**
     * 重试退避时间（毫秒）。
     */
    private long retryBackoffMs = 300;

    /**
     * 运行时增强器链（按 order 排序执行）。
     */
    private List<AgentRuntimeAdvisor> runtimeAdvisors = new ArrayList<>();

    @Data
    public static class PromptTemplate {
        /**
         * 模板版本（可选，仅用于观测与管理）。
         */
        private String version;

        /**
         * 系统提示模板。
         */
        private String systemPrompt;

        /**
         * 用户消息模板，支持 {{message}} 占位符。
         */
        private String userMessage;
    }
}
