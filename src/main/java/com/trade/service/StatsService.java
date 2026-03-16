package com.trade.service;

import com.trade.mapper.TradeDataMapper;
import com.trade.vo.StatsOverviewVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class StatsService {
    @Autowired
    private TradeDataMapper tradeDataMapper;

    public StatsOverviewVO getOverview() {
        StatsOverviewVO vo = new StatsOverviewVO();
        vo.setTotalRecords(tradeDataMapper.countAllNotDeleted());
        vo.setTotalAmount(tradeDataMapper.sumAmountRmbNotDeleted());
        vo.setTradeModeStats(tradeDataMapper.groupByTradeModeNotDeleted());
        return vo;
    }

    public List<Map<String, Object>> getTrend(String startMonth, String endMonth) {
        return tradeDataMapper.groupTrendByMonthNotDeleted(startMonth, endMonth);
    }

    public List<Map<String, Object>> getTradeModeRatio() {
        return tradeDataMapper.groupCountByTradeModeNotDeleted();
    }

    public List<Map<String, Object>> getCountryRatio() {
        return tradeDataMapper.groupAmountByPartnerNameNotDeleted();
    }
}
