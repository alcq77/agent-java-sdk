package io.github.alcq77.cqagent.spi.rag;

import java.util.List;

/**
 * 向量存储 SPI —— 抽象向量数据库的存储与检索操作。
 *
 * <p>用户可通过实现此接口接入任意向量数据库（Milvus、PgVector、Elasticsearch、
 * Pinecone 等），Spring Boot 场景下只需注册为 {@code @Bean} 即可自动替换默认实现。</p>
 *
 * <p>核心操作：
 * <ul>
 *   <li>{@link #upsert} — 写入/更新向量（含元数据）</li>
 *   <li>{@link #search} — 向量相似度检索</li>
 *   <li>{@link #deleteByDocumentId} — 按文档 ID 批量删除</li>
 *   <li>{@link #count} — 当前存储的 chunk 总数</li>
 * </ul>
 */
public interface VectorStore {

    /**
     * 写入或更新一组向量 chunk。
     * <p>如果 chunkId 已存在则覆盖（upsert 语义）。</p>
     *
     * @param chunks 待写入的 chunk 列表
     */
    void upsert(List<VectorChunk> chunks);

    /**
     * 向量相似度检索。
     *
     * @param queryEmbedding 查询向量
     * @param topK           返回条数上限
     * @param filter         可选的元数据过滤条件（null 表示不过滤）
     * @return 按相似度降序排列的 chunk 列表
     */
    List<ScoredChunk> search(double[] queryEmbedding, int topK, MetadataFilter filter);

    /**
     * 按文档 ID 删除所有关联的 chunk。
     *
     * @param documentId 文档 ID
     */
    void deleteByDocumentId(String documentId);

    /**
     * 清空所有数据。
     */
    void clear();

    /**
     * 当前存储的 chunk 总数。
     */
    long count();
}
