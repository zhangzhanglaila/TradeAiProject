package com.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trade.ai.AiAssistant;
import com.trade.ai.AiProperties;
import com.trade.dto.AiAskRequest;
import com.trade.entity.AiChatLog;
import com.trade.mapper.AiChatLogMapper;
import com.trade.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AiService {
    @Autowired
    private AiChatLogMapper aiChatLogMapper;

    @Autowired(required = false)
    private AiAssistant aiAssistant;

    @Autowired
    private AiProperties aiProperties;

    public String ask(AiAskRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();

        String answer;
        if (aiProperties.isEnabled() && aiAssistant != null) {
            answer = aiAssistant.chat(userId, request.getQuestion());
        } else {
            answer = "AI 功能未启用，请配置 app.ai.enabled=true 并设置有效的 API Key。您的问题是：" + request.getQuestion();
        }

        AiChatLog log = new AiChatLog();
        log.setUserId(userId);
        log.setQuestion(request.getQuestion());
        log.setAnswer(answer);
        log.setCreateTime(LocalDateTime.now());
        aiChatLogMapper.insert(log);

        return answer;
    }

    public Page<AiChatLog> getHistory(Integer current, Integer size) {
        Long userId = SecurityUtil.getCurrentUserId();

        Page<AiChatLog> page = new Page<>(current, size);
        LambdaQueryWrapper<AiChatLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatLog::getUserId, userId);
        wrapper.orderByDesc(AiChatLog::getCreateTime);
        return aiChatLogMapper.selectPage(page, wrapper);
    }
}
