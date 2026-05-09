package io.github.alcq77.cqagent.core.rag;

import java.util.Locale;

/**
 * 轻量 embedding 实现：基于 token hash 的桶向量。
 * <p>
 * 分词策略：
 * <ul>
 *   <li>英文等拉丁文字：按非字母字符切分，过滤停用词</li>
 *   <li>CJK 字符：使用 bigram（相邻两字符）作为 token，确保中文文本有效分词</li>
 *   <li>标点符号：被剥离，不参与哈希</li>
 * </ul>
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
        String lower = text.toLowerCase(Locale.ROOT);
        // Extract CJK bigrams
        extractCjkBigrams(lower, vector);
        // Extract Latin tokens
        extractLatinTokens(lower, vector);
        normalize(vector);
        return vector;
    }

    /**
     * Extracts CJK bigrams: consecutive pairs of CJK characters.
     * For "退款申请流程" produces: "退款", "款申", "申请", "请流", "流程"
     */
    private void extractCjkBigrams(String text, double[] vector) {
        int len = text.length();
        for (int i = 0; i < len - 1; i++) {
            char c1 = text.charAt(i);
            char c2 = text.charAt(i + 1);
            if (isCjk(c1) && isCjk(c2)) {
                String bigram = text.substring(i, i + 2);
                int bucket = Math.floorMod(bigram.hashCode(), dimensions);
                vector[bucket] += 1.0;
            }
        }
    }

    /**
     * Extracts Latin/ASCII tokens by splitting on non-letter characters.
     */
    private void extractLatinTokens(String text, double[] vector) {
        int start = -1;
        for (int i = 0; i <= text.length(); i++) {
            boolean isLetter = i < text.length() && text.charAt(i) >= 'a' && text.charAt(i) <= 'z';
            if (isLetter) {
                if (start == -1) {
                    start = i;
                }
            } else {
                if (start != -1 && i - start > 1) {
                    // Only use tokens with length > 1 to skip single letters
                    String token = text.substring(start, i);
                    int bucket = Math.floorMod(token.hashCode(), dimensions);
                    vector[bucket] += 1.0;
                }
                start = -1;
            }
        }
    }

    private static boolean isCjk(char c) {
        return (c >= '一' && c <= '鿿')   // CJK Unified Ideographs
            || (c >= '㐀' && c <= '䶿')   // CJK Extension A
            || (c >= '豈' && c <= '﫿')   // CJK Compatibility Ideographs
            || (c >= '぀' && c <= 'ゟ')   // Hiragana
            || (c >= '゠' && c <= 'ヿ')   // Katakana
            || (c >= '가' && c <= '힯');   // Korean Hangul Syllables
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