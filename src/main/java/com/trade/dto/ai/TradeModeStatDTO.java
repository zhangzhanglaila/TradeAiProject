package com.trade.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeModeStatDTO {

    private String tradeModeName;
    private Long count;
    private BigDecimal totalAmount;
}
