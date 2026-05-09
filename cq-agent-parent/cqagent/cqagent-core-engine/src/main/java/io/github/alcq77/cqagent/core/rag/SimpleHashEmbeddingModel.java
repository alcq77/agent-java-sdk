package io.github.alcq77.cqagent.core.rag;

/**
 * 轻量 embedding 实现：基于 token hash 的桶向量。
 * 仅用于本地可用版，不依赖外部向量服务。
 */
public class SimpleHashEmbeddingModel implements TextEmbeddingModel {

    private final int dimensions;

    public SimpleHashEmbeddingModel() {
        this(128);
    }

    public SimpleHashEmbeddingModel(int dimensions) {
        this.dimensions = Math.max(16, dimensions);
    }

    @Override
    public double[] embed(String text) {
        double[] vector = new double[dimensions];
        if (text == null || text.isBlank()) {
            return vector;
        }
        String[] tokens = text.toLowerCase().split("\\s+");
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            int bucket = Math.floorMod(token.hashCode(), dimensions);
            vector[bucket] += 1.0;
        }
        normalize(vector);
        return vector;
    }

    private static void normalize(double[] vector) {
        double norm = 0.0;
        for (double v : vector) {
            norm += v * v;
        }
        if (norm <= 0.0) {
            return;
        }
        double scale = Math.sqrt(norm);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / scale;
        }
    }
}
