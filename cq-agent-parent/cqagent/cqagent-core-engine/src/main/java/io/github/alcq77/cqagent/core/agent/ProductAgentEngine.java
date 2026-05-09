package io.github.alcq77.cqagent.core.agent;

import io.github.alcq77.cqagent.core.observability.AgentRuntimeCounters;
import io.github.alcq77.cqagent.spi.session.ProductSessionStore;
import io.github.alcq77.cqagent.spi.tool.ProductTool;

import java.util.List;
import java.util.Map;

/**
 * 兼容旧命名的运行时包装类。
 *
 * @deprecated 请改用 {@link LangChain4jProductAgentRuntime}
 */
@Deprecated
public class ProductAgentEngine extends LangChain4jProductAgentRuntime {

    /**
     * 兼容旧构造参数签名，内部直接委托给 LangChain4j 运行时。
     */
    public ProductAgentEngine(ProductSessionStore sessionStore,
                              List<ProductTool> tools,
                              int maxToolCallIterations,
                              Map<String, PromptTemplate> promptTemplates,
                              String defaultPromptTemplateId,
                              boolean fallbackToDefaultPromptTemplate) {
        super(sessionStore, tools, maxToolCallIterations, promptTemplates, defaultPromptTemplateId,
            fallbackToDefaultPromptTemplate, new AgentRuntimeCounters());
    }
}
