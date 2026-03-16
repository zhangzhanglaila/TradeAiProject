package com.trade.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 贸易数据实体类
 * 
 * 对应数据库表：trade_data
 * 
 * @Data: Lombok 注解，自动生成 getter/setter/toString 等方法
 * @TableName: 指定对应的数据库表名
 */
@Data
@TableName("trade_data")
public class TradeData {
    
    /**
     * 主键 ID
     * @TableId: 标识主键字段，type = IdType.AUTO 表示自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 数据年月，如 202501
     */
    private String dataYearMonth;
    
    /**
     * 贸易伙伴编码
     */
    private String partnerCode;
    
    /**
     * 贸易伙伴名称
     */
    private String partnerName;
    
    /**
     * 注册地编码
     */
    private String regCode;
    
    /**
     * 注册地名称
     */
    private String regName;
    
    /**
     * 商品编码
     */
    private String productCode;
    
    /**
     * 商品名称
     */
    private String productName;
    
    /**
     * 贸易方式编码
     */
    private String tradeModeCode;
    
    /**
     * 贸易方式名称
     */
    private String tradeModeName;
    
    /**
     * 第一数量
     */
    private BigDecimal firstQuantity;
    
    /**
     * 第一计量单位
     */
    private String firstUnit;
    
    /**
     * 第二数量
     */
    private BigDecimal secondQuantity;
    
    /**
     * 第二计量单位
     */
    private String secondUnit;
    
    /**
     * 人民币金额
     */
    private BigDecimal amountRmb;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 逻辑删除标识（0-未删，1-已删）
     * @TableLogic: 标识逻辑删除字段
     */
    @TableLogic
    private Integer deleted;
}
