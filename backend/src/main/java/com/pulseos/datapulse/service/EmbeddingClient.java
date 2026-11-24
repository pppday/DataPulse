package com.pulseos.datapulse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本向量化客户端（封装 Spring AI EmbeddingModel，默认调用 BAAI/bge-m3）。
 * 支持按 16 条分批向量化并做 L2 归一化，供 Few-Shot SQL 示例相似度检索使用；未配 Key 时优雅降级。
 */
@Service
@RequiredArgsConstructor
public class EmbeddingClient {

    private static final int BATCH_SIZE = 16;

    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;

    @Value("${spring.ai.openai.embedding.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.embedding.options.model:BAAI/bge-m3}")
    private String model;

    public boolean isEnabled() {
        return apiKey != null
                && !apiKey.isBlank()
                && !apiKey.startsWith("sk-placeholder")
                && embeddingModelProvider.getIfAvailable() != null;
    }

    public String model() {
        return model;
    }

    public List<float[]> embed(List<String> texts) {
        if (!isEnabled()) {
            throw new IllegalStateException("Embedding API key 未配置或 Spring AI EmbeddingModel 不可用");
        }
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        EmbeddingModel embeddingModel = embeddingModelProvider.getObject();
        List<float[]> allVectors = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += BATCH_SIZE) {
            List<String> batch = texts.subList(start, Math.min(start + BATCH_SIZE, texts.size()));
            EmbeddingResponse response = embeddingModel.embedForResponse(batch);
            List<Embedding> results = response.getResults();
            if (results == null || results.size() != batch.size()) {
                throw new IllegalStateException("Spring AI Embedding 返回数量不匹配");
            }
            for (Embedding emb : results) {
                allVectors.add(l2Normalize(emb.getOutput()));
            }
        }
        return allVectors;
    }

    private static float[] l2Normalize(float[] vec) {
        if (vec == null || vec.length == 0) {
            return vec;
        }
        double sumSq = 0.0;
        for (float v : vec) {
            sumSq += (double) v * v;
        }
        double norm = Math.sqrt(sumSq);
        if (norm <= 1e-12) {
            return vec;
        }
        float[] out = new float[vec.length];
        for (int i = 0; i < vec.length; i++) {
            out[i] = (float) (vec[i] / norm);
        }
        return out;
    }
}
