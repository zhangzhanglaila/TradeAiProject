package com.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("news_text_segment")
public class NewsTextSegment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long newsId;

    private Integer chunkIndex;

    private String content;

    /** JSON float 数组，形如 [0.1,0.2,...] */
    private String embedding;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
