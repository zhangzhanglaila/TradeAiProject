package com.trade.vo.kg;

import lombok.Data;

@Data
public class KgBuildProgressVO {

    private Long historyId;

    /**
     * BUILDING / SUCCEEDED / FAILED
     */
    private String status;

    /**
     * 0-100
     */
    private Integer progress;

    private String message;
}
