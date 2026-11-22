package com.pulseos.datapulse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulseos.datapulse.common.BusinessException;
import com.pulseos.datapulse.entity.QueryHistory;
import com.pulseos.datapulse.entity.SysSqlExample;
import com.pulseos.datapulse.mapper.QueryHistoryMapper;
import com.pulseos.datapulse.mapper.SysSqlExampleMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SqlExampleRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(SqlExampleRetrievalService.class);
    private static final double MIN_COSINE_SCORE = 0.45;
    private static final double MIN_BIGRAM_SCORE = 0.15;

    private final SysSqlExampleMapper exampleMapper;
    private final QueryHistoryMapper queryHistoryMapper;
    private final EmbeddingClient embeddingClient;
    private final SqlSafetySandboxService sqlSafetySandboxService;
    private final SchemaCatalogService schemaCatalogService;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<Long, List<SysSqlExample>> cache = new ConcurrentHashMap<>();

    public List<SysSqlExample> listByDatasource(Long datasourceId) {
        long dsId = datasourceId != null ? datasourceId : 1L;
        return cache.computeIfAbsent(dsId, id -> exampleMapper.selectList(
                new LambdaQueryWrapper<SysSqlExample>()
                        .eq(SysSqlExample::getDatasourceId, id)
                        .orderByDesc(SysSqlExample::getId)
        ));
    }

    public List<SysSqlExample> retrieveTopK(Long datasourceId, String question, int k) {
        if (question == null || question.isBlank() || k <= 0) {
            return List.of();
        }
        long dsId = datasourceId != null ? datasourceId : 1L;
        List<SysSqlExample> examples = listByDatasource(dsId);
        if (examples == null || examples.isEmpty()) {
            return List.of();
        }

        if (embeddingClient.isEnabled()) {
            try {
                return retrieveByEmbedding(examples, question, k);
            } catch (Exception e) {
                log.warn("[Few-Shot Embedding 检索失败，降级 bigram Jaccard] error={}", e.getMessage());
            }
        }
        return retrieveByBigram(examples, question, k);
    }

    public SysSqlExample add(Long datasourceId, String question, String sql) {
        if (question == null || question.isBlank() || sql == null || sql.isBlank()) {
            throw new BusinessException(400, "示例问题与 SQL 均不能为空");
        }
        long dsId = datasourceId != null ? datasourceId : 1L;
        Set<String> allowedTables = schemaCatalogService.getWhitelistedTables(dsId);
        SqlSafetySandboxService.SafetyCheckResult check = sqlSafetySandboxService.inspectAndSanitize(sql, allowedTables);
        if (!check.isSafe()) {
            throw new BusinessException(400, "示例 SQL 未通过安全沙箱校验: " + check.getReason());
        }

        String cleanQuestion = question.trim();
        String embJson = null;
        String embModel = null;
        if (embeddingClient.isEnabled()) {
            try {
                List<float[]> vecs = embeddingClient.embed(List.of(cleanQuestion));
                if (!vecs.isEmpty()) {
                    embJson = objectMapper.writeValueAsString(vecs.get(0));
                    embModel = embeddingClient.model();
                }
            } catch (Exception e) {
                log.warn("[新增示例向量化失败，保留空向量待懒加载] error={}", e.getMessage());
            }
        }

        SysSqlExample ex = SysSqlExample.builder()
                .datasourceId(dsId)
                .question(cleanQuestion)
                .sqlText(check.getSanitizedSql())
                .embedding(embJson)
                .embeddingModel(embModel)
                .source("MANUAL")
                .createdAt(LocalDateTime.now())
                .build();
        exampleMapper.insert(ex);
        cache.remove(dsId);
        return ex;
    }

    public SysSqlExample addFromHistory(Long historyId, Long datasourceId) {
        QueryHistory history = queryHistoryMapper.selectById(historyId);
        if (history == null) {
            throw new BusinessException(404, "未找到对应的查询历史记录");
        }
        if (!Integer.valueOf(1).equals(history.getIsSafe())
                || history.getRowCount() == null
                || history.getRowCount() <= 0) {
            throw new BusinessException(400, "仅支持采纳安全审计通过且返回结果非空的查询记录");
        }

        long dsId = datasourceId != null ? datasourceId : 1L;
        Set<String> allowedTables = schemaCatalogService.getWhitelistedTables(dsId);
        SqlSafetySandboxService.SafetyCheckResult check =
                sqlSafetySandboxService.inspectAndSanitize(history.getGeneratedSql(), allowedTables);
        if (!check.isSafe()) {
            throw new BusinessException(400, "历史记录 SQL 未通过当前数据源白名单校验: " + check.getReason());
        }

        SysSqlExample ex = SysSqlExample.builder()
                .datasourceId(dsId)
                .question(history.getUserPrompt().trim())
                .sqlText(check.getSanitizedSql())
                .source("HISTORY")
                .createdAt(LocalDateTime.now())
                .build();
        exampleMapper.insert(ex);
        cache.remove(dsId);
        return ex;
    }

    public void delete(Long id) {
        SysSqlExample existing = exampleMapper.selectById(id);
        if (existing == null) {
            return;
        }
        exampleMapper.deleteById(id);
        if (existing.getDatasourceId() != null) {
            cache.remove(existing.getDatasourceId());
        } else {
            cache.clear();
        }
    }

    private List<SysSqlExample> retrieveByEmbedding(List<SysSqlExample> examples, String question, int k) throws Exception {
        String currentModel = embeddingClient.model();
        List<SysSqlExample> missing = new ArrayList<>();
        for (SysSqlExample ex : examples) {
            if (ex.getEmbedding() == null || ex.getEmbedding().isBlank() || !currentModel.equals(ex.getEmbeddingModel())) {
                missing.add(ex);
            }
        }

        if (!missing.isEmpty()) {
            List<String> texts = missing.stream().map(SysSqlExample::getQuestion).toList();
            List<float[]> computed = embeddingClient.embed(texts);
            for (int i = 0; i < missing.size(); i++) {
                SysSqlExample ex = missing.get(i);
                String json = objectMapper.writeValueAsString(computed.get(i));
                ex.setEmbedding(json);
                ex.setEmbeddingModel(currentModel);
                exampleMapper.updateById(ex);
            }
        }

        float[] qVec = embeddingClient.embed(List.of(question)).get(0);
        List<ScoredExample> scored = new ArrayList<>();
        for (SysSqlExample ex : examples) {
            if (ex.getEmbedding() == null || ex.getEmbedding().isBlank()) {
                continue;
            }
            float[] exVec = objectMapper.readValue(ex.getEmbedding(), float[].class);
            double sim = cosine(qVec, exVec);
            if (sim >= MIN_COSINE_SCORE) {
                scored.add(new ScoredExample(ex, sim));
            }
        }
        scored.sort(Comparator.comparingDouble(ScoredExample::score).reversed());
        return scored.stream().limit(k).map(ScoredExample::example).toList();
    }

    private List<SysSqlExample> retrieveByBigram(List<SysSqlExample> examples, String question, int k) {
        List<ScoredExample> scored = new ArrayList<>();
        for (SysSqlExample ex : examples) {
            double sim = bigramJaccard(question, ex.getQuestion());
            if (sim >= MIN_BIGRAM_SCORE) {
                scored.add(new ScoredExample(ex, sim));
            }
        }
        scored.sort(Comparator.comparingDouble(ScoredExample::score).reversed());
        return scored.stream().limit(k).map(ScoredExample::example).toList();
    }

    static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom <= 1e-12 ? 0.0 : dot / denom;
    }

    static double bigramJaccard(String a, String b) {
        Set<String> setA = extractBigrams(a);
        Set<String> setB = extractBigrams(b);
        if (setA.isEmpty() || setB.isEmpty()) {
            return 0.0;
        }
        int intersection = 0;
        for (String gram : setA) {
            if (setB.contains(gram)) {
                intersection++;
            }
        }
        int union = setA.size() + setB.size() - intersection;
        return union == 0 ? 0.0 : (double) intersection / union;
    }

    private static Set<String> extractBigrams(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        String normalized = text.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsHan}\\p{IsAlphabetic}\\p{IsDigit}]", "");
        if (normalized.isEmpty()) {
            return Set.of();
        }
        if (normalized.length() == 1) {
            return Set.of(normalized);
        }
        Set<String> grams = new HashSet<>();
        for (int i = 0; i < normalized.length() - 1; i++) {
            grams.add(normalized.substring(i, i + 2));
        }
        return grams;
    }

    private record ScoredExample(SysSqlExample example, double score) {
    }
}
