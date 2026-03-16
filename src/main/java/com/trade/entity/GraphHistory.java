package com.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("graph_history")
public class GraphHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String graphName;

    private String graphKey;

    /**
     * 新闻 ID 列表（JSON 数组字符串），例如：[1,2,3]
     */
    private String newsIds;

    private LocalDateTime createdAt;
}
