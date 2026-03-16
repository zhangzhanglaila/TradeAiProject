package com.trade.ai.rag;

import com.trade.ai.AiProperties;
import com.trade.entity.NewsData;
import com.trade.entity.NewsTextSegment;
import com.trade.mapper.NewsDataMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class NewsRagIndexService {

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private NewsDataMapper newsDataMapper;

    @Autowired
    private NewsTextSegmentRepository newsTextSegmentRepository;

    private EmbeddingModel embeddingModel() {
        return QwenEmbeddingModel.builder()
                .apiKey(aiProperties.getApiKey())
                .modelName(aiProperties.getEmbeddingModelName())
                .build();
    }

    /**
     * 为指定 newsId 构建/重建索引。
     */
    public int buildIndexForNewsId(Long newsId) {
        if (newsId == null) {
            return 0;
        }
        NewsData news = newsDataMapper.selectById(newsId);
        if (news == null || (news.getDeleted() != null && news.getDeleted() == 1)) {
            return 0;
        }
        String content = news.getContentText();
        if (content == null || content.isBlank()) {
            return 0;
        }

        // 先删除旧索引，再写入新索引（最小可行实现）
        newsTextSegmentRepository.deleteByNewsId(newsId);

        List<String> chunks = chunk(content, aiProperties.getNewsChunkSize(), aiProperties.getNewsChunkOverlap());
        if (chunks.isEmpty()) {
            return 0;
        }

        EmbeddingModel model = embeddingModel();
        List<TextSegment> segments = chunks.stream().map(TextSegment::from).collect(Collectors.toList());
        Response<List<Embedding>> resp = model.embedAll(segments);
        List<Embedding> embeddings = resp.content();
        if (embeddings == null || embeddings.size() != chunks.size()) {
            // 理论不应发生；防御一下
            return 0;
        }

        List<NewsTextSegment> rows = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            NewsTextSegment row = new NewsTextSegment();
            row.setNewsId(newsId);
            row.setChunkIndex(i);
            row.setContent(chunks.get(i));
            row.setEmbedding(toJsonArray(embeddings.get(i).vector()));
            row.setCreateTime(LocalDateTime.now());
            row.setUpdateTime(LocalDateTime.now());
            row.setDeleted(0);
            rows.add(row);
        }
        if (!rows.isEmpty()) {
            newsTextSegmentRepository.insertBatch(rows);
        }
        return rows.size();
    }

    /**
     * 确保某条新闻已建立索引：若未建立则构建。
     */
    public void ensureIndexed(Long newsId) {
        if (newsId == null) {
            return;
        }
        Integer cnt = newsTextSegmentRepository.countByNewsId(newsId);
        if (cnt == null || cnt <= 0) {
            buildIndexForNewsId(newsId);
        }
    }

    /**
     * 语义检索：返回 TopK chunk（含 title/url/pubDate 等元信息）。
     *
     * 说明：为避免引入向量库，这里采用“从 MySQL 拉候选 chunk → Java 侧计算 cosine”最小实现。
     */
    public List<SemanticNewsHit> search(String query, String startDate, String endDate, Integer limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        int topK = (limit == null || limit <= 0) ? 8 : Math.min(limit, 20);

        LocalDateTime start = parseDateStart(startDate);
        LocalDateTime end = parseDateEnd(endDate);

        int candidateLimit = Math.max(200, aiProperties.getNewsRagCandidateLimit());
        List<NewsTextSegmentWithNews> candidates = newsTextSegmentRepository.selectCandidates(start, end, candidateLimit);

        // 按需构建：如果当前没有任何 chunk，先对最近一批新闻补建索引，再检索一次
        if (candidates == null || candidates.isEmpty()) {
            indexRecentNewsIfNeeded(start, end);
            candidates = newsTextSegmentRepository.selectCandidates(start, end, candidateLimit);
        }

        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        EmbeddingModel model = embeddingModel();
        Embedding q = model.embed(query).content();
        if (q == null) {
            return List.of();
        }
        float[] qv = q.vector();

        return candidates.stream()
                .map(c -> {
                    float[] dv = parseEmbedding(c.getEmbedding());
                    double score = cosine(qv, dv);
                    String snippet = buildSnippet(c.getContent(), query);
                    return new SemanticNewsHit(
                            c.getNewsId(),
                            c.getTitle(),
                            c.getPubDate(),
                            c.getUrl(),
                            c.getChunkIndex(),
                            snippet,
                            score
                    );
                })
                .sorted(Comparator.comparingDouble(SemanticNewsHit::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    private void indexRecentNewsIfNeeded(LocalDateTime start, LocalDateTime end) {
        List<Long> recentIds = newsTextSegmentRepository.selectRecentNewsIds(start, end, 50);
        if (recentIds == null || recentIds.isEmpty()) {
            return;
        }
        for (Long id : recentIds) {
            try {
                ensureIndexed(id);
            } catch (Exception ignored) {
            }
        }
    }

    private LocalDateTime parseDateStart(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            LocalDate d = LocalDate.parse(s.trim());
            return d.atStartOfDay();
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDateTime parseDateEnd(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            LocalDate d = LocalDate.parse(s.trim());
            return d.atTime(23, 59, 59);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> chunk(String text, int chunkSize, int overlap) {
        String t = (text == null) ? "" : text.trim();
        if (t.isEmpty()) {
            return List.of();
        }
        int size = (chunkSize <= 0) ? 600 : chunkSize;
        int ov = Math.max(0, overlap);
        if (ov >= size) {
            ov = 0;
        }

        List<String> out = new ArrayList<>();
        int step = Math.max(1, size - ov);
        for (int start = 0; start < t.length(); start += step) {
            int end = Math.min(t.length(), start + size);
            String c = t.substring(start, end).trim();
            if (!c.isEmpty()) {
                out.add(c);
            }
            if (end >= t.length()) {
                break;
            }
        }
        return out;
    }

    private String toJsonArray(float[] vec) {
        if (vec == null) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(vec.length * 6);
        sb.append('[');
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(String.format(Locale.US, "%.6f", vec[i]));
        }
        sb.append(']');
        return sb.toString();
    }

    private float[] parseEmbedding(String json) {
        if (json == null || json.isBlank()) {
            return new float[0];
        }
        String s = json.trim();
        if (s.length() < 2 || s.charAt(0) != '[' || s.charAt(s.length() - 1) != ']') {
            return new float[0];
        }
        s = s.substring(1, s.length() - 1).trim();
        if (s.isEmpty()) {
            return new float[0];
        }
        String[] parts = s.split(",");
        float[] v = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                v[i] = Float.parseFloat(parts[i].trim());
            } catch (Exception e) {
                v[i] = 0f;
            }
        }
        return v;
    }

    private double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0) {
            return 0d;
        }
        int n = Math.min(a.length, b.length);
        double dot = 0d;
        double na = 0d;
        double nb = 0d;
        for (int i = 0; i < n; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        if (na == 0d || nb == 0d) {
            return 0d;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private String buildSnippet(String content, String query) {
        if (content == null) {
            return null;
        }
        String t = content.replaceAll("\\s+", " ").trim();
        if (t.length() <= 220) {
            return t;
        }
        String q = (query == null) ? "" : query.trim();
        int idx = (q.isEmpty()) ? -1 : t.toLowerCase().indexOf(q.toLowerCase());
        if (idx < 0) {
            return t.substring(0, 220) + "...";
        }
        int start = Math.max(0, idx - 60);
        int end = Math.min(t.length(), start + 220);
        String prefix = (start > 0) ? "..." : "";
        String suffix = (end < t.length()) ? "..." : "";
        return prefix + t.substring(start, end) + suffix;
    }
}
