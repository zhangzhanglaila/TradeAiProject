package com.trade.ai.rag;

import com.trade.entity.NewsTextSegment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 面向 RAG 的新闻切块持久化：自定义 SQL Mapper。
 */
@Component
public class NewsTextSegmentRepository {

    @Autowired
    private NewsTextSegmentSqlMapper newsTextSegmentSqlMapper;

    public void insertBatch(List<NewsTextSegment> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        newsTextSegmentSqlMapper.insertBatch(list);
    }

    public int deleteByNewsId(Long newsId) {
        if (newsId == null) {
            return 0;
        }
        return newsTextSegmentSqlMapper.deleteByNewsId(newsId);
    }

    public Integer countByNewsId(Long newsId) {
        if (newsId == null) {
            return 0;
        }
        return newsTextSegmentSqlMapper.countByNewsId(newsId);
    }

    public List<NewsTextSegmentWithNews> selectCandidates(LocalDateTime start, LocalDateTime end, Integer limit) {
        return newsTextSegmentSqlMapper.selectCandidates(start, end, limit);
    }

    public List<Long> selectRecentNewsIds(LocalDateTime start, LocalDateTime end, Integer limit) {
        return newsTextSegmentSqlMapper.selectRecentNewsIds(start, end, limit);
    }
}
