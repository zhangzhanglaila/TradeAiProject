-- 已有库升级 SQL（从旧版本升级到本次版本）

-- 1) trade_data: trade_mode_name 扩容 + 增量更新唯一索引
ALTER TABLE trade_data
    MODIFY COLUMN trade_mode_name VARCHAR(100) COMMENT '贸易方式名称',
    ADD UNIQUE KEY uk_trade_ym_hs_country_mode (data_year_month, product_code, partner_code, trade_mode_code);

-- 2) news_data: 增加 source_id + 唯一索引
ALTER TABLE news_data
    ADD COLUMN source_id VARCHAR(64) COMMENT '外部数据源 ID' AFTER id,
    ADD UNIQUE KEY uk_news_source_id (source_id);

-- 3) graph_history: 新增知识图谱历史表
CREATE TABLE IF NOT EXISTS graph_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户 ID',
  graph_name VARCHAR(255) NULL COMMENT '图谱名称',
  graph_key VARCHAR(64) NOT NULL COMMENT '图谱隔离 Key（UUID）',
  news_ids JSON NOT NULL COMMENT '新闻 ID 列表（JSON 数组）',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY idx_user_created (user_id, created_at),
  UNIQUE KEY uk_graph_key (graph_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识图谱历史表';
