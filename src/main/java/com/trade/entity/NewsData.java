package com.trade.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("news_data")
public class NewsData {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String sourceId;

    private String title;
    private LocalDateTime pubDate;
    private String descText;
    private String tags;
    private String url;
    private String contentHtml;
    private String contentText;
    private String pics;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
