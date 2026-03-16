package com.trade.dto.kg;

import lombok.Data;

import java.util.List;

@Data
public class KgGenerateRequest {
    private List<Long> newsIds;
}
