package io.github.alcq77.cqagent.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.alcq77.cqagent.core.rag.*;
import io.github.alcq77.cqagent.spi.rag.VectorStore;
import io.github.alcq77.cqagent.core.runtime.advisor.AgentRuntimeAdvisor;
import io.github.alcq77.cqagent.core.runtime.advisor.RagContextAdvisor;
import io.github.alcq77.cqagent.core.session.InMemoryProductSessionStore;
import io.github.alcq77.cqagent.sdk.AgentClient;
import io.github.alcq77.cqagent.sdk.AgentClientBuilder;
import io.github.alcq77.cqagent.sdk.internal.AgentRuntimeMetricsProvider;
import io.github.alcq77.cqagent.sdk.internal.CircuitBreakerSnapshotProvider;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqagent.spi.model.ProductModelProvider;
import io.github.alcq77.cqagent.spi.session.ProductSessionStore;
import io.github.alcq77.cqagent.spi.tool.ProductTool;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.file.Path;

@AutoConfiguration
@ConditionalOnClass(AgentClient.class)
@EnableConfigurationProperties(ProductStarterProperties.class)
public class ProductAgentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AgentClient productAgentClient(ProductStarterProperties properties,
                                          ObjectProvider<ProductModelProvider> modelProviders,
                                          ObjectProvider<ProductSessionStore> sessionStores,
                                          ObjectProvider<ProductTool> tools,
                                          ObjectProvider<AgentRuntimeAdvisor> advisors) {
        // 统一构建入口：把配置、SPI 扩展和增强链组装成一个可直接注入的 AgentClient。
        AgentClientBuilder builder = AgentClientBuilder.create()
                .logicalModel(properties.getLogicalModel())
                .maxHistoryMessages(properties.getMaxHistoryMessages())
                .invokeTimeoutMillis(properties.getInvokeTimeoutMs())
                .retry(properties.getMaxRetries(), properties.getRetryBackoffMs())
                .fallbackToDefaultPromptTemplate(properties.getPrompts().isFallbackToDefault());
        if (properties.getPrompts().getDefaultTemplateId() != null && !properties.getPrompts().getDefaultTemplateId().isBlank()) {
            builder.defaultPromptTemplate(properties.getPrompts().getDefaultTemplateId());
        }
        properties.getPrompts().getTemplates().forEach((templateId, template) ->
                builder.promptTemplate(templateId, template.getSystemPrompt(), template.getUserMessage()));
        properties.getRouting().forEach(builder::route);
        properties.getRoutePolicies().forEach(builder::routePolicy);
        properties.getModelDispatchPolicies().forEach(builder::modelDispatchPolicy);
        properties.getEndpoints().forEach((id, ep) -> builder.endpoint(ProductEndpointConfig.builder()
                .id(id)
                .provider(ep.getProvider())
                .baseUrl(ep.getBaseUrl())
                .apiKey(ep.getApiKey())
                .defaultModel(ep.getDefaultModel())
                .headers(ep.getHeaders())
                .connectTimeout(ep.getConnectTimeout())
                .readTimeout(ep.getReadTimeout())
                .build()));
        ProductSessionStore sessionStore = sessionStores.getIfAvailable();
        if (sessionStore != null) {
            builder.sessionStore(sessionStore);
        }
        modelProviders.orderedStream().forEach(builder::modelProvider);
        tools.orderedStream().forEach(builder::tool);
        advisors.orderedStream().forEach(builder::advisor);
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.product.session", name = "store", havingValue = "in_memory", matchIfMissing = true)
    @ConditionalOnMissingBean(ProductSessionStore.class)
    public ProductSessionStore inMemoryProductSessionStore(ProductStarterProperties properties) {
        return new InMemoryProductSessionStore(properties.getMaxHistoryMessages());
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.product.session", name = "store", havingValue = "filesystem")
    @ConditionalOnMissingBean(ProductSessionStore.class)
    public ProductSessionStore fileSystemProductSessionStore(ProductStarterProperties properties,
                                                             ObjectMapper objectMapper) {
        return new FileSystemProductSessionStore(
                objectMapper,
                properties.getSession().getFilesystemDirectory(),
                properties.getMaxHistoryMessages()
        );
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.product.session", name = "store", havingValue = "redis")
    @ConditionalOnMissingBean(ProductSessionStore.class)
    public ProductSessionStore redisProductSessionStore(ProductStarterProperties properties,
                                                        StringRedisTemplate redisTemplate,
                                                        ObjectMapper objectMapper) {
        return new RedisProductSessionStore(
                redisTemplate,
                objectMapper,
                properties.getSession().getRedisKeyPrefix(),
                properties.getSession().getRedisTtl(),
                properties.getMaxHistoryMessages()
        );
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.product.session", name = "store", havingValue = "jdbc")
    @ConditionalOnMissingBean(ProductSessionStore.class)
    public ProductSessionStore jdbcProductSessionStore(ProductStarterProperties properties,
                                                       JdbcTemplate jdbcTemplate,
                                                       PlatformTransactionManager transactionManager,
                                                       ObjectMapper objectMapper) {
        return new JdbcProductSessionStore(
                jdbcTemplate,
                objectMapper,
                transactionManager,
                properties.getSession().getJdbcTable(),
                properties.getMaxHistoryMessages(),
                properties.getSession().isJdbcAutoInit()
        );
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(ProductStarterSanityChecker.class)
    public ProductStarterSanityChecker productStarterSanityChecker(ProductStarterProperties properties) {
        return new ProductStarterSanityChecker(properties);
    }

    @Bean
    @ConditionalOnMissingBean(ProductWorkspaceInitializer.class)
    public ProductWorkspaceInitializer productWorkspaceInitializer(ProductStarterProperties properties) {
        return new ProductWorkspaceInitializer(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.product.plugin", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public ProductPluginDirectoryToolRegistry productPluginDirectoryToolRegistry(ProductStarterProperties properties,
                                                                                ObjectMapper objectMapper) {
        return new ProductPluginDirectoryToolRegistry(properties, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.product.plugin", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public ProductTool productPluginDispatcherTool(ProductPluginDirectoryToolRegistry registry) {
        return new ProductPluginDispatcherTool(registry);
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.product.skills", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public WorkspaceSkillsRegistry workspaceSkillsRegistry(ProductStarterProperties properties) {
        return new WorkspaceSkillsRegistry(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.product.skills", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "workspaceSkillsTool")
    public ProductTool workspaceSkillsTool(WorkspaceSkillsRegistry registry) {
        return new WorkspaceSkillsTool(registry);
    }

    @Bean
    @ConditionalOnMissingBean(name = "productStarterHealthIndicator")
    public HealthIndicator productStarterHealthIndicator(ProductStarterProperties properties,
                                                         AgentClient agentClient,
                                                         ObjectProvider<ProductSessionStore> sessionStoreProvider,
                                                         ObjectProvider<RagIndexRefresher> ragIndexRefresherProvider) {
        return () -> {
            if (properties.getEndpoints().isEmpty()) {
                return Health.down().withDetail("reason", "agent.product.endpoints is empty").build();
            }
            Health.Builder health = Health.up()
                    .withDetail("logicalModel", properties.getLogicalModel())
                    .withDetail("workspace", properties.getWorkspace())
                    .withDetail("sessionStoreConfig", properties.getSession().getStore());
            ProductSessionStore store = sessionStoreProvider.getIfAvailable();
            if (store != null) {
                health.withDetail("sessionStore", store.getClass().getSimpleName());
                if (store instanceof SessionStoreHealthProbe probe) {
                    boolean healthy = probe.healthy();
                    health.withDetail("sessionStoreHealthy", healthy)
                            .withDetail("sessionStoreDetail", probe.detail());
                    if (!healthy) {
                        return Health.down()
                                .withDetails(health.build().getDetails())
                                .withDetail("reason", "session store is unhealthy")
                                .build();
                    }
                }
            }
            if (agentClient instanceof CircuitBreakerSnapshotProvider provider) {
                health.withDetail("circuitBreakers", provider.circuitBreakerSnapshot());
            }
            if (agentClient instanceof AgentRuntimeMetricsProvider metricsProvider) {
                health.withDetail("runtimeMetrics", metricsProvider.runtimeMetrics());
            }
            RagIndexRefresher ragRefresher = ragIndexRefresherProvider.getIfAvailable();
            if (ragRefresher != null) {
                health.withDetail("ragIndex", ragRefresher.snapshot());
                // strict 模式下，RAG 刷新失败将作为健康降级条件；非 strict 仅暴露告警信息。
                if (!ragRefresher.healthy() && properties.getRag().isHealthStrict()) {
                    return Health.down()
                            .withDetails(health.build().getDetails())
                            .withDetail("reason", "rag index refresh failed")
                            .build();
                }
            }
            return health.build();
        };
    }

    @Bean
    @ConditionalOnClass(Endpoint.class)
    @ConditionalOnMissingBean(ProductSessionStoreEndpoint.class)
    public ProductSessionStoreEndpoint productSessionStoreEndpoint(ProductStarterProperties properties,
                                                                   ObjectProvider<ProductSessionStore> sessionStoreProvider) {
        ProductSessionStore store = sessionStoreProvider.getIfAvailable();
        return new ProductSessionStoreEndpoint(properties, store);
    }

    @Bean
    @ConditionalOnMissingBean(ProductAgentExceptionHandler.class)
    public ProductAgentExceptionHandler productAgentExceptionHandler() {
        return new ProductAgentExceptionHandler();
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.product.rag", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public TextEmbeddingModel ragEmbeddingModel() {
        return new SimpleHashEmbeddingModel(128);
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.product.rag", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public VectorStore vectorStore() {
        return new InMemoryVectorStore();
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.product.rag", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public RagLocalFileImporter ragLocalFileImporter() {
        return new RagLocalFileImporter();
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.product.rag", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public RagIndexer ragIndexer(ProductStarterProperties properties,
                                 TextEmbeddingModel embeddingModel,
                                 VectorStore vectorStore) {
        RagChunkSplitter splitter = new RagChunkSplitter(
            properties.getRag().getChunkSize(),
            properties.getRag().getOverlap()
        );
        return new RagIndexer(splitter, embeddingModel, vectorStore);
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.product.rag", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public RagIndexMetadataStore ragIndexMetadataStore(ProductStarterProperties properties) {
        return new RagIndexMetadataStore(Path.of(properties.getRag().getManifestPath()));
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(prefix = "agent.product.rag", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public RagIndexRefresher ragIndexRefresher(ProductStarterProperties properties,
                                               RagIndexer indexer,
                                               RagLocalFileImporter importer,
                                               RagIndexMetadataStore metadataStore) {
        // 启动后自动定时增量同步，避免人工触发索引刷新。
        return new RagIndexRefresher(
                indexer,
                importer,
                metadataStore,
                Path.of(properties.getRag().getKnowledgeDirectory()),
                properties.getRag().getRefreshIntervalSeconds()
        );
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.product.rag", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public RagRetriever ragRetriever(VectorStore vectorStore, TextEmbeddingModel embeddingModel) {
        return new RagRetriever(vectorStore, embeddingModel);
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.product.rag", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(name = "ragContextAdvisor")
    public AgentRuntimeAdvisor ragContextAdvisor(ProductStarterProperties properties,
                                                 RagRetriever retriever,
                                                 RagIndexRefresher ignoredRefresher) {
        RagRetrievalFilter filter = new RagRetrievalFilter()
            .allowedSources(properties.getRag().getAllowedSources())
            .equalsMetadata(properties.getRag().getMetadataEquals());
        return new RagContextAdvisor(
            retriever,
            properties.getRag().getTopK(),
            properties.getRag().getAdvisorOrder(),
            filter
        );
    }
}
