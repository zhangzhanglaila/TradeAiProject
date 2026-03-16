package com.trade.ai.rag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SemanticNewsHit {

    private Long newsId;
    private String title;
    private LocalDateTime pubDate;
    private String url;

    private Integer chunkIndex;
    private String snippet;

    /** 相似度分数（cosine），越大越相关 */
    private Double score;
}
