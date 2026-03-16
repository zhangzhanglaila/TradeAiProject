package com.trade.vo.kg;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class GraphHistoryVO {
    private Long id;
    private String graphName;
    private List<Long> newsIds;
    private LocalDateTime createdAt;
}
