package com.trade.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class StatsOverviewVO {
    private Long totalRecords;
    private BigDecimal totalAmount;
    private List<Map<String, Object>> tradeModeStats;
}
