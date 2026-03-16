package com.trade.controller;

import com.trade.common.Result;
import com.trade.service.KnowledgeGraphService;
import com.trade.vo.kg.KgBuildProgressVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "新闻知识图谱-进度")
@RestController
@RequestMapping("/api/knowledge-graph")
public class KnowledgeGraphProgressController {

    private final KnowledgeGraphService knowledgeGraphService;

    public KnowledgeGraphProgressController(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    @ApiOperation("获取图谱构建进度")
    @GetMapping("/progress/{historyId}")
    public Result<KgBuildProgressVO> getProgress(@PathVariable Long historyId) {
        return Result.success(knowledgeGraphService.getProgress(historyId));
    }
}
