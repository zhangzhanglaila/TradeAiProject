package com.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.common.BusinessException;
import com.trade.common.ErrorCode;
import com.trade.entity.GraphHistory;
import com.trade.entity.NewsData;
import com.trade.mapper.GraphHistoryMapper;
import com.trade.mapper.NewsDataMapper;
import com.trade.service.KnowledgeGraphService;
import com.trade.service.kg.KgAiExtractService;
import com.trade.service.kg.Neo4jGraphRepository;
import com.trade.service.kg.Triple;
import com.trade.util.SecurityUtil;
import com.trade.vo.kg.GraphHistoryVO;
import com.trade.vo.kg.KgGraphVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private static final int MAX_NEWS_IDS = 20;

    private final GraphHistoryMapper graphHistoryMapper;
    private final NewsDataMapper newsDataMapper;
    private final KgAiExtractService kgAiExtractService;
    private final Neo4jGraphRepository neo4jGraphRepository;
    private final com.trade.service.kg.KgBuildProgressStore progressStore;
    private final com.trade.service.kg.KgBuildAsyncService buildAsyncService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public KnowledgeGraphServiceImpl(GraphHistoryMapper graphHistoryMapper,
                                    NewsDataMapper newsDataMapper,
                                    KgAiExtractService kgAiExtractService,
                                    Neo4jGraphRepository neo4jGraphRepository,
                                    com.trade.service.kg.KgBuildProgressStore progressStore,
                                    com.trade.service.kg.KgBuildAsyncService buildAsyncService) {
        this.graphHistoryMapper = graphHistoryMapper;
        this.newsDataMapper = newsDataMapper;
        this.kgAiExtractService = kgAiExtractService;
        this.neo4jGraphRepository = neo4jGraphRepository;
        this.progressStore = progressStore;
        this.buildAsyncService = buildAsyncService;
    }

    @Override
    @Transactional
    public Long generateGraph(List<Long> newsIds) {
        if (newsIds == null || newsIds.isEmpty() || newsIds.size() > MAX_NEWS_IDS) {
            throw new BusinessException(ErrorCode.KG_PARAM_INVALID, "newsIds 不能为空且数量不超过 " + MAX_NEWS_IDS);
        }

        // 去重、保持顺序
        List<Long> dedup = new ArrayList<>(new LinkedHashSet<>(newsIds));

        List<NewsData> newsList = newsDataMapper.selectBatchIds(dedup);
        if (newsList == null || newsList.isEmpty()) {
            throw new BusinessException(ErrorCode.KG_PARAM_INVALID, "未查询到对应新闻");
        }
        // 过滤逻辑删除
        newsList.removeIf(n -> n == null || (n.getDeleted() != null && n.getDeleted() == 1));
        if (newsList.isEmpty()) {
            throw new BusinessException(ErrorCode.KG_PARAM_INVALID, "新闻均已删除或不可用");
        }

        Long userId = SecurityUtil.getCurrentUserId();
        String graphKey = UUID.randomUUID().toString().replace("-", "");

        GraphHistory history = new GraphHistory();
        history.setUserId(userId);
        history.setGraphName(null);
        history.setGraphKey(graphKey);
        history.setNewsIds(toJson(dedup));
        history.setCreatedAt(java.time.LocalDateTime.now());
        graphHistoryMapper.insert(history);

        Long historyId = history.getId();
        progressStore.init(historyId);
        progressStore.update(historyId, 5, "历史已创建，准备构建图谱");

        // 异步构建：避免接口 30s 超时
        buildAsyncService.build(historyId, graphKey, userId, newsList);

        return historyId;
    }



    @Override
    public com.trade.vo.kg.KgBuildProgressVO getProgress(Long historyId) {
        if (historyId == null) {
            throw new BusinessException(ErrorCode.KG_PARAM_INVALID, "historyId 不能为空");
        }
        com.trade.vo.kg.KgBuildProgressVO vo = progressStore.get(historyId);
        if (vo == null) {
            // 可能是服务重启导致内存进度丢失
            throw new BusinessException(ErrorCode.KG_NOT_FOUND, "未找到构建进度（可能服务已重启）");
        }
        return vo;
    }

    @Override
    public void saveHistoryName(Long historyId, String graphName) {
        if (historyId == null) {
            throw new BusinessException(ErrorCode.KG_PARAM_INVALID, "historyId 不能为空");
        }

        GraphHistory history = graphHistoryMapper.selectById(historyId);
        if (history == null) {
            throw new BusinessException(ErrorCode.KG_NOT_FOUND);
        }

        Long userId = SecurityUtil.getCurrentUserId();
        if (!Objects.equals(history.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.KG_FORBIDDEN);
        }

        GraphHistory update = new GraphHistory();
        update.setId(historyId);
        update.setGraphName(graphName);
        graphHistoryMapper.updateById(update);
    }

    @Override
    public Page<GraphHistoryVO> pageHistory(Integer current, Integer size) {
        int c = (current == null || current <= 0) ? 1 : current;
        int s = (size == null || size <= 0) ? 10 : size;

        Long userId = SecurityUtil.getCurrentUserId();

        Page<GraphHistory> page = new Page<>(c, s);
        LambdaQueryWrapper<GraphHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GraphHistory::getUserId, userId);
        wrapper.orderByDesc(GraphHistory::getCreatedAt);
        Page<GraphHistory> p = graphHistoryMapper.selectPage(page, wrapper);

        Page<GraphHistoryVO> out = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        List<GraphHistoryVO> records = new ArrayList<>();
        if (p.getRecords() != null) {
            for (GraphHistory h : p.getRecords()) {
                records.add(toVO(h));
            }
        }
        out.setRecords(records);
        return out;
    }

    @Override
    @Transactional
    public void deleteHistory(Long id, boolean deleteGraph) {
        if (id == null) {
            throw new BusinessException(ErrorCode.KG_PARAM_INVALID, "id 不能为空");
        }

        GraphHistory history = graphHistoryMapper.selectById(id);
        if (history == null) {
            throw new BusinessException(ErrorCode.KG_NOT_FOUND);
        }

        Long userId = SecurityUtil.getCurrentUserId();
        if (!Objects.equals(history.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.KG_FORBIDDEN);
        }

        if (deleteGraph) {
            try {
                neo4jGraphRepository.deleteGraph(history.getGraphKey());
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.KG_NEO4J_FAILED, "删除 Neo4j 图谱失败：" + e.getMessage());
            }
        }

        graphHistoryMapper.deleteById(id);
    }

    @Override
    public KgGraphVO loadGraph(Long historyId) {
        if (historyId == null) {
            throw new BusinessException(ErrorCode.KG_PARAM_INVALID, "historyId 不能为空");
        }

        com.trade.vo.kg.KgBuildProgressVO p = progressStore.get(historyId);
        if (p != null && "BUILDING".equals(p.getStatus())) {
            throw new BusinessException(ErrorCode.KG_BUILDING, "图谱构建中，请稍后重试");
        }
        if (p != null && "FAILED".equals(p.getStatus())) {
            throw new BusinessException(ErrorCode.KG_BUILD_FAILED, p.getMessage());
        }

        GraphHistory history = graphHistoryMapper.selectById(historyId);
        if (history == null) {
            throw new BusinessException(ErrorCode.KG_NOT_FOUND);
        }

        Long userId = SecurityUtil.getCurrentUserId();
        if (!Objects.equals(history.getUserId(), userId)) {
            throw new BusinessException(ErrorCode.KG_FORBIDDEN);
        }

        Map<String, Object> data;
        try {
            data = neo4jGraphRepository.loadGraph(history.getGraphKey());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.KG_NEO4J_FAILED, "查询 Neo4j 失败：" + e.getMessage());
        }

        return toGraphVO(data);
    }

    private GraphHistoryVO toVO(GraphHistory h) {
        GraphHistoryVO vo = new GraphHistoryVO();
        vo.setId(h.getId());
        vo.setGraphName(h.getGraphName());
        vo.setCreatedAt(h.getCreatedAt());
        vo.setNewsIds(parseNewsIds(h.getNewsIds()));
        return vo;
    }

    private List<Long> parseNewsIds(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {
            });
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
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

    @SuppressWarnings("unchecked")
    private KgGraphVO toGraphVO(Map<String, Object> data) {
        KgGraphVO vo = new KgGraphVO();

        List<Map<String, Object>> news = (List<Map<String, Object>>) data.getOrDefault("news", Collections.emptyList());
        List<Map<String, Object>> entities = (List<Map<String, Object>>) data.getOrDefault("entities", Collections.emptyList());
        List<Map<String, Object>> edges = (List<Map<String, Object>>) data.getOrDefault("edges", Collections.emptyList());

        List<KgGraphVO.Node> nodesOut = new ArrayList<>();
        List<KgGraphVO.Edge> edgesOut = new ArrayList<>();

        for (Map<String, Object> n : news) {
            KgGraphVO.Node node = new KgGraphVO.Node();
            Object newsId = n.get("newsId");
            node.setId("news:" + String.valueOf(newsId));
            node.setLabel(Objects.toString(n.get("title"), "news:" + newsId));
            node.setType("News");
            node.setProperties(n);
            nodesOut.add(node);
        }

        for (Map<String, Object> e : entities) {
            KgGraphVO.Node node = new KgGraphVO.Node();
            String name = Objects.toString(e.get("name"), "");
            node.setId("entity:" + name);
            node.setLabel(name);
            node.setType(Objects.toString(e.get("type"), "Entity"));
            node.setProperties(e);
            nodesOut.add(node);
        }

        int i = 0;
        for (Map<String, Object> r : edges) {
            KgGraphVO.Edge edge = new KgGraphVO.Edge();
            String source = Objects.toString(r.get("source"), "");
            String target = Objects.toString(r.get("target"), "");
            edge.setId("e" + (++i));
            edge.setSource("entity:" + source);
            edge.setTarget("entity:" + target);
            edge.setLabel(Objects.toString(r.get("relationType"), ""));
            edge.setProperties(r);
            edgesOut.add(edge);
        }

        vo.setNodes(nodesOut);
        vo.setEdges(edgesOut);
        return vo;
    }
}
