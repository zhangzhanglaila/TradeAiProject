package com.trade.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trade.common.Result;
import com.trade.dto.kg.KgGenerateRequest;
import com.trade.dto.kg.KgHistorySaveRequest;
import com.trade.service.KnowledgeGraphService;
import com.trade.vo.kg.GraphHistoryVO;
import com.trade.vo.kg.KgGenerateVO;
import com.trade.vo.kg.KgGraphVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

@Api(tags = "新闻知识图谱")
@RestController
@RequestMapping("/api/knowledge-graph")
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;

    public KnowledgeGraphController(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    @ApiOperation("生成知识图谱")
    @PostMapping("/generate")
    public Result<KgGenerateVO> generate(@RequestBody KgGenerateRequest request) {
        Long historyId = knowledgeGraphService.generateGraph(request == null ? null : request.getNewsIds());
        return Result.success(new KgGenerateVO(historyId));
    }

    @ApiOperation("保存/命名图谱历史")
    @PostMapping("/history")
    public Result<Void> saveHistory(@RequestBody KgHistorySaveRequest request) {
        if (request == null) {
            return Result.success();
        }
        knowledgeGraphService.saveHistoryName(request.getHistoryId(), request.getGraphName());
        return Result.success();
    }

    @ApiOperation("分页查询当前用户图谱历史")
    @GetMapping("/history")
    public Result<Page<GraphHistoryVO>> pageHistory(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(knowledgeGraphService.pageHistory(current, size));
    }

    @ApiOperation("删除图谱历史")
    @DeleteMapping("/history/{id}")
    public Result<Void> deleteHistory(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean deleteGraph) {
        knowledgeGraphService.deleteHistory(id, deleteGraph);
        return Result.success();
    }

    @ApiOperation("加载图谱数据")
    @GetMapping("/{historyId}")
    public Result<KgGraphVO> loadGraph(@PathVariable Long historyId) {
        return Result.success(knowledgeGraphService.loadGraph(historyId));
    }
}
