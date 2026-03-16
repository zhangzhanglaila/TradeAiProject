package com.trade.service.kg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Triple {
    private String head;
    private String headType;
    private String relation;
    private String tail;
    private String tailType;
    private Double confidence;
    private String source;
}
