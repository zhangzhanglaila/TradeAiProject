package com.trade.service.kg;

import com.trade.common.BusinessException;
import com.trade.common.ErrorCode;
import com.trade.entity.NewsData;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KgBuildAsyncService {

    private final KgAiExtractService kgAiExtractService;
    private final Neo4jGraphRepository neo4jGraphRepository;
    private final KgBuildProgressStore progressStore;

    public KgBuildAsyncService(KgAiExtractService kgAiExtractService,
                              Neo4jGraphRepository neo4jGraphRepository,
                              KgBuildProgressStore progressStore) {
        this.kgAiExtractService = kgAiExtractService;
        this.neo4jGraphRepository = neo4jGraphRepository;
        this.progressStore = progressStore;
    }

    @Async
    public void build(Long historyId, String graphKey, Long userId, List<NewsData> newsList) {
        try {
            progressStore.update(historyId, 20, "正在抽取三元组");
            List<Triple> triples = kgAiExtractService.extractTriples(newsList);

            progressStore.update(historyId, 80, "正在写入图数据库");
            neo4jGraphRepository.upsertGraph(graphKey, userId, toNewsMaps(newsList), triples);

            progressStore.succeed(historyId);
        } catch (BusinessException e) {
            progressStore.fail(historyId, e.getMessage());
        } catch (Exception e) {
            progressStore.fail(historyId, "构建失败：" + e.getMessage());
        }
    }

    private List<Map<String, Object>> toNewsMaps(List<NewsData> list) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (NewsData n : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("newsId", n.getId());
            m.put("title", n.getTitle());
            m.put("pubDate", n.getPubDate() == null ? null : n.getPubDate().toString());
            String content = n.getContentText() != null ? n.getContentText() : n.getDescText();
            if (content != null && content.length() > 200) {
                content = content.substring(0, 200);
            }
            m.put("contentSnippet", content);
            out.add(m);
        }
        return out;
    }
}
