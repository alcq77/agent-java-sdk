package io.github.alcq77.cqagent.sdk.example;

import io.github.alcq77.cqagent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqagent.core.rag.*;
import io.github.alcq77.cqagent.core.runtime.advisor.RagContextAdvisor;
import io.github.alcq77.cqagent.sdk.AgentClient;
import io.github.alcq77.cqagent.sdk.AgentClientBuilder;
import io.github.alcq77.cqagent.spi.model.ProductEndpointConfig;

import java.nio.file.Path;
import java.util.List;

/**
 * 本地知识库问答最小示例。
 */
public final class RagQuickStartExample {

    private RagQuickStartExample() {
    }

    public static void main(String[] args) {
        InMemoryRagStore store = new InMemoryRagStore();
        TextEmbeddingModel embeddingModel = new SimpleHashEmbeddingModel(128);
        RagIndexer indexer = new RagIndexer(new RagChunkSplitter(400, 80), embeddingModel, store);
        Path kbDir = args.length > 0 ? Path.of(args[0]) : Path.of("./workspace/knowledge");
        RagLocalFileImporter importer = new RagLocalFileImporter();
        List<RagDocument> docs = importer.load(kbDir);
        if (docs.isEmpty()) {
            throw new IllegalStateException("knowledge directory is empty: " + kbDir.toAbsolutePath());
        }
        indexer.syncIncremental(kbDir, importer, new RagIndexMetadataStore(Path.of("./workspace/rag/index-manifest.json")));

        RagRetriever retriever = new RagRetriever(store, embeddingModel);
        RagContextAdvisor advisor = new RagContextAdvisor(retriever, 2, -100);

        AgentClient client = AgentClientBuilder.create()
            .logicalModel("primary-llm")
            .endpoint(ProductEndpointConfig.builder()
                .id("primary")
                .provider("openai_compat")
                .baseUrl("http://127.0.0.1:11434")
                .defaultModel("llama3.2")
                .build())
            .route("primary-llm", "primary")
            .advisor(advisor)
            .build();

        AgentChatRequest request = AgentChatRequest.builder()
            .message("我的订单已经 9 天了，还能直接退款吗？")
            .taskType("qa")
            .tags(List.of("domain:ecommerce"))
            .build();
        System.out.println(client.chat(request).getReply());
    }
}
