package com.trade.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trade.vo.kg.GraphHistoryVO;
import com.trade.vo.kg.KgBuildProgressVO;
import com.trade.vo.kg.KgGraphVO;

import java.util.List;

public interface KnowledgeGraphService {

    Long generateGraph(List<Long> newsIds);

    KgBuildProgressVO getProgress(Long historyId);

    void saveHistoryName(Long historyId, String graphName);

    Page<GraphHistoryVO> pageHistory(Integer current, Integer size);

    void deleteHistory(Long id, boolean deleteGraph);

    KgGraphVO loadGraph(Long historyId);
}
