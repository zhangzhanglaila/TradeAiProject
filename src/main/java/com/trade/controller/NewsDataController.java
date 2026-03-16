package com.trade.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trade.common.Result;
import com.trade.common.ErrorCode;
import com.trade.entity.NewsData;
import com.trade.service.NewsDataService;
import com.trade.util.ResponseUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

@Api(tags = "新闻数据管理")
@RestController
@RequestMapping("/api/news")
public class NewsDataController {
    @Autowired
    private NewsDataService newsDataService;

    @ApiOperation("分页查询新闻数据")
    @GetMapping("/page")
    public Result<Page<NewsData>> getPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(newsDataService.getPage(current, size, keyword, startDate, endDate));
    }

    @ApiOperation("导出CSV")
    @GetMapping("/export")
    public void export(HttpServletResponse response, @RequestParam(required = false) String keyword) throws Exception {
        ResponseUtil.setCsvDownloadHeader(response, "news_data.csv");
        newsDataService.writeExportCsv(response.getOutputStream(), keyword);
    }

    @ApiOperation("上传CSV文件")
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "50000") Integer maxRows,
            @RequestParam(required = false, defaultValue = "500") Integer batchSize,
            @RequestParam(required = false, defaultValue = "GB18030") String charset) throws Exception {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            return Result.error(ErrorCode.PARAM_INVALID.getCode(), "上传文件为空或未选择文件");
        }
        newsDataService.importCsv(file, maxRows, batchSize, charset);
        return Result.success();
    }

    @ApiOperation("获取新闻详情")
    @GetMapping("/{id}")
    public Result<NewsData> getById(@PathVariable Long id) {
        return Result.success(newsDataService.getById(id));
    }

    @ApiOperation("新增新闻数据")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> save(@RequestBody NewsData newsData) {
        newsDataService.save(newsData);
        return Result.success();
    }

    @ApiOperation("修改新闻数据")
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@RequestBody NewsData newsData) {
        newsDataService.update(newsData);
        return Result.success();
    }

    @ApiOperation("删除新闻数据")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        newsDataService.delete(id);
        return Result.success();
    }
}
