package io.github.alcq77.cqagent.core.rag;

/**
 * 文本 embedding 抽象。
 */
public interface TextEmbeddingModel {

    double[] embed(String text);
}
