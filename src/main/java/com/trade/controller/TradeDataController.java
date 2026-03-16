package com.trade.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trade.annotation.Log;
import com.trade.common.Result;
import com.trade.entity.TradeData;
import com.trade.service.TradeDataService;
import com.trade.util.ResponseUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Api(tags = "贸易数据管理")
@RestController
@RequestMapping("/api/trade")
public class TradeDataController {
    @Autowired
    private TradeDataService tradeDataService;

    @ApiOperation("分页查询贸易数据")
    @GetMapping("/page")
    public Result<Page<TradeData>> getPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth) {
        return Result.success(tradeDataService.getPage(current, size, keyword, startMonth, endMonth));
    }

    @ApiOperation("导出CSV")
    @GetMapping("/export")
    public void export(
            HttpServletResponse response,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth) throws Exception {
        ResponseUtil.setCsvDownloadHeader(response, "trade_data.csv");
        tradeDataService.writeExportCsv(response.getOutputStream(), keyword, startMonth, endMonth);
    }

    @ApiOperation("获取贸易数据详情")
    @GetMapping("/{id}")
    public Result<TradeData> getById(@PathVariable Long id) {
        return Result.success(tradeDataService.getById(id));
    }

    @ApiOperation("新增贸易数据")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> save(@RequestBody TradeData tradeData) {
        tradeDataService.save(tradeData);
        return Result.success();
    }

    @ApiOperation("修改贸易数据")
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@RequestBody TradeData tradeData) {
        tradeDataService.update(tradeData);
        return Result.success();
    }

    @ApiOperation("删除贸易数据")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        tradeDataService.delete(id);
        return Result.success();
    }

    @ApiOperation("批量删除贸易数据")
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        tradeDataService.deleteBatch(ids);
        return Result.success();
    }

    @Log("上传CSV")
    @ApiOperation("上传CSV文件")
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "50000") Integer maxRows,
            @RequestParam(required = false, defaultValue = "500") Integer batchSize,
            @RequestParam(required = false, defaultValue = "GB18030") String charset) throws Exception {
        tradeDataService.importCsv(file, maxRows, batchSize, charset);
        return Result.success();
    }
}
