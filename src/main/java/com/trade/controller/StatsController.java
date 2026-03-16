package com.trade.controller;

import com.trade.common.Result;
import com.trade.service.StatsService;
import com.trade.vo.StatsOverviewVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "统计分析")
@RestController
@RequestMapping("/api/stats")
public class StatsController {
    @Autowired
    private StatsService statsService;

    @ApiOperation("总览统计")
    @GetMapping("/overview")
    public Result<StatsOverviewVO> getOverview() {
        return Result.success(statsService.getOverview());
    }

    @ApiOperation("月度趋势")
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getTrend(
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth) {
        return Result.success(statsService.getTrend(startMonth, endMonth));
    }

    @ApiOperation("贸易方式占比")
    @GetMapping("/trade-mode-ratio")
    public Result<List<Map<String, Object>>> getTradeModeRatio() {
        return Result.success(statsService.getTradeModeRatio());
    }

    @ApiOperation("国家分布")
    @GetMapping("/country-ratio")
    public Result<List<Map<String, Object>>> getCountryRatio() {
        return Result.success(statsService.getCountryRatio());
    }
}
