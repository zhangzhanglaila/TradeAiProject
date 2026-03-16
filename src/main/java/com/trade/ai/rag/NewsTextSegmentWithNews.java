package com.trade.ai.rag;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NewsTextSegmentWithNews {

    private Long newsId;
    private Integer chunkIndex;

    private String content;
    private String embedding;

    private String title;
    private LocalDateTime pubDate;
    private String url;
}
