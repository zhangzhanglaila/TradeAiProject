package com.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trade.dto.ai.TradeSummaryDTO;
import com.trade.entity.TradeData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface TradeDataMapper extends BaseMapper<TradeData> {
    int insertBatch(@Param("list") List<TradeData> list);

    Long countAllNotDeleted();

    BigDecimal sumAmountRmbNotDeleted();

    List<Map<String, Object>> groupByTradeModeNotDeleted();

    List<Map<String, Object>> groupTrendByMonthNotDeleted(@Param("startMonth") String startMonth,
                                                          @Param("endMonth") String endMonth);

    List<Map<String, Object>> groupCountByTradeModeNotDeleted();

    List<Map<String, Object>> groupAmountByPartnerNameNotDeleted();

    // ====== AI 分析型工具（带过滤条件） ======

    TradeSummaryDTO getTradeSummary(@Param("startMonth") String startMonth,
                                    @Param("endMonth") String endMonth,
                                    @Param("keyword") String keyword);

    List<Map<String, Object>> groupTrendByMonth(@Param("startMonth") String startMonth,
                                                @Param("endMonth") String endMonth,
                                                @Param("keyword") String keyword);

    List<Map<String, Object>> groupByTradeMode(@Param("startMonth") String startMonth,
                                               @Param("endMonth") String endMonth,
                                               @Param("keyword") String keyword);

    List<Map<String, Object>> groupAmountByPartnerName(@Param("startMonth") String startMonth,
                                                       @Param("endMonth") String endMonth,
                                                       @Param("keyword") String keyword,
                                                       @Param("limit") Integer limit);
}
