package com.trade.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trade.ai.rag.NewsRagIndexService;
import com.trade.ai.rag.SemanticNewsHit;
import com.trade.dto.ai.TradeSummaryDTO;
import com.trade.entity.AiChatLog;
import com.trade.entity.NewsData;
import com.trade.entity.TradeData;
import com.trade.mapper.AiChatLogMapper;
import com.trade.mapper.TradeDataMapper;
import com.trade.service.NewsDataService;
import com.trade.service.TradeDataService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TradeAiTools {

    @Autowired
    private TradeDataService tradeDataService;

    @Autowired
    private NewsDataService newsDataService;

    @Autowired
    private AiChatLogMapper aiChatLogMapper;

    @Autowired
    private TradeDataMapper tradeDataMapper;

    @Autowired
    private NewsRagIndexService newsRagIndexService;

    /**
     * 查询贸易明细（只读，仅用于查看样例明细）。
     */
    @Tool("查询贸易明细")
    public List<TradeData> queryTradeData(
            @P("关键词，可匹配商品名/伙伴名") String keyword,
            @P("起始年月，如 202501") String startMonth,
            @P("结束年月，如 202512") String endMonth,
            @P("最多返回多少条（默认10，最大50）") Integer limit) {
        int size = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);
        return tradeDataService.getPage(1, size, keyword, startMonth, endMonth).getRecords();
    }

    /**
     * AI：贸易汇总（记录数 + 总金额）。
     */
    @Tool("贸易汇总")
    public TradeSummaryDTO getTradeSummary(
            @P("起始年月，如 202501") String startMonth,
            @P("结束年月，如 202512") String endMonth,
            @P("关键词，可匹配商品名/伙伴名（可选）") String keyword) {
        return tradeDataMapper.getTradeSummary(startMonth, endMonth, keyword);
    }

    /**
     * AI：贸易金额月度趋势。
     */
    @Tool("贸易金额趋势")
    public List<Map<String, Object>> getTradeTrend(
            @P("起始年月，如 202501") String startMonth,
            @P("结束年月，如 202512") String endMonth,
            @P("关键词，可匹配商品名/伙伴名（可选）") String keyword) {
        return tradeDataMapper.groupTrendByMonth(startMonth, endMonth, keyword);
    }

    /**
     * AI：贸易方式统计。
     */
    @Tool("贸易方式统计")
    public List<Map<String, Object>> getTradeModeStats(
            @P("起始年月，如 202501") String startMonth,
            @P("结束年月，如 202512") String endMonth,
            @P("关键词，可匹配商品名/伙伴名（可选）") String keyword) {
        return tradeDataMapper.groupByTradeMode(startMonth, endMonth, keyword);
    }

    /**
     * AI：Top 伙伴。
     */
    @Tool("贸易伙伴Top")
    public List<Map<String, Object>> getTopPartners(
            @P("起始年月，如 202501") String startMonth,
            @P("结束年月，如 202512") String endMonth,
            @P("关键词，可匹配商品名/伙伴名（可选）") String keyword,
            @P("返回条数（默认10，最大50）") Integer limit) {
        int topN = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);
        return tradeDataMapper.groupAmountByPartnerName(startMonth, endMonth, keyword, topN);
    }

    /**
     * 查询新闻（只读，标题 keyword）。
     */
    @Tool("查询新闻")
    public List<NewsData> queryNews(
            @P("关键词，匹配标题") String keyword,
            @P("最多返回多少条") Integer limit) {
        int size = (limit == null || limit <= 0) ? 20 : Math.min(limit, 200);
        return newsDataService.getPage(1, size, keyword).getRecords();
    }

    /**
     * AI：新闻语义检索（用于新闻内容问答/总结/引用）。
     */
    @Tool("新闻语义检索")
    public List<SemanticNewsHit> searchNewsBySemantic(
            @P("要检索的问题/主题") String query,
            @P("开始日期 YYYY-MM-DD（可选）") String startDate,
            @P("结束日期 YYYY-MM-DD（可选）") String endDate,
            @P("返回条数（默认8，最大20）") Integer limit) {
        return newsRagIndexService.search(query, startDate, endDate, limit);
    }

    /**
     * 查询对话历史。
     */
    @Tool("查询对话历史")
    public List<AiChatLog> queryChatHistory(
            @ToolMemoryId Object memoryId,
            @P("最近多少条") Integer limit) {
        Long userId;
        if (memoryId instanceof Long) {
            userId = (Long) memoryId;
        } else if (memoryId instanceof String) {
            try {
                userId = Long.parseLong((String) memoryId);
            } catch (NumberFormatException e) {
                userId = 0L;
            }
        } else {
            userId = 0L;
        }
        int size = (limit == null || limit <= 0) ? 10 : Math.min(limit, 100);
        LambdaQueryWrapper<AiChatLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatLog::getUserId, userId);
        wrapper.orderByDesc(AiChatLog::getCreateTime);
        wrapper.last("LIMIT " + size);
        return aiChatLogMapper.selectList(wrapper);
    }
}
