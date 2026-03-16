package com.trade.dto.ai;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TradeSummaryDTO {

    private String startMonth;
    private String endMonth;
    private String keyword;

    private Long recordCount;
    private BigDecimal totalAmountRmb;
}
