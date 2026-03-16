package com.trade.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trade.common.Result;
import com.trade.dto.AiAskRequest;
import com.trade.entity.AiChatLog;
import com.trade.service.AiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "AI 智能问答")
@RestController
@RequestMapping("/api/ai")
public class AiController {
    @Autowired
    private AiService aiService;

    @ApiOperation("AI 问答")
    @PostMapping("/ask")
    public Result<String> ask(@RequestBody AiAskRequest request) {
        return Result.success(aiService.ask(request));
    }

    @ApiOperation("历史对话")
    @GetMapping("/history")
    public Result<Page<AiChatLog>> getHistory(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(aiService.getHistory(current, size));
    }
}
