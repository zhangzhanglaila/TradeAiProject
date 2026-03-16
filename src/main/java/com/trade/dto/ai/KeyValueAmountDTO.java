package com.trade.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyValueAmountDTO {

    /** name/month 等 */
    private String key;

    private BigDecimal value;
}
