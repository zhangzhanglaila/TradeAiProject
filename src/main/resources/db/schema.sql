CREATE DATABASE IF NOT EXISTS trade_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE trade_db;

CREATE TABLE IF NOT EXISTS trade_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    data_year_month VARCHAR(7) COMMENT '数据年月，如 202501',
    partner_code VARCHAR(20) COMMENT '贸易伙伴编码',
    partner_name VARCHAR(100) COMMENT '贸易伙伴名称',
    reg_code VARCHAR(20) COMMENT '注册地编码',
    reg_name VARCHAR(100) COMMENT '注册地名称',
    product_code VARCHAR(20) COMMENT '商品编码',
    product_name VARCHAR(200) COMMENT '商品名称',
    trade_mode_code VARCHAR(20) COMMENT '贸易方式编码',
    trade_mode_name VARCHAR(100) COMMENT '贸易方式名称',
    first_quantity DECIMAL(20,2) COMMENT '第一数量',
    first_unit VARCHAR(20) COMMENT '第一计量单位',
    second_quantity DECIMAL(20,2) COMMENT '第二数量',
    second_unit VARCHAR(20) COMMENT '第二计量单位',
    amount_rmb DECIMAL(20,2) COMMENT '人民币金额',
    remark TEXT COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除（0-未删，1-已删）',
    UNIQUE KEY uk_trade_ym_hs_country_mode (data_year_month, product_code, partner_code, trade_mode_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贸易数据表';

CREATE TABLE IF NOT EXISTS news_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    source_id VARCHAR(64) COMMENT '外部数据源 ID',
    title VARCHAR(500) COMMENT '新闻标题',
    pub_date DATETIME COMMENT '发布日期',
    desc_text TEXT COMMENT '描述',
    tags VARCHAR(255) COMMENT '标签',
    url VARCHAR(500) COMMENT '来源链接',
    content_html LONGTEXT COMMENT 'HTML 内容',
    content_text LONGTEXT COMMENT '纯文本内容',
    pics TEXT COMMENT '图片列表（逗号分隔）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_news_source_id (source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻数据表';

CREATE TABLE IF NOT EXISTS news_text_segment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    news_id BIGINT NOT NULL COMMENT '关联 news_data.id',
    chunk_index INT NOT NULL COMMENT '切块序号，从 0 开始',
    content TEXT COMMENT '切块文本',
    embedding LONGTEXT COMMENT '向量（JSON float 数组）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除（预留）',
    KEY idx_news_id (news_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻向量切块表';

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username VARCHAR(50) UNIQUE COMMENT '用户名（唯一）',
    password VARCHAR(100) COMMENT 'BCrypt 加密密码',
    role VARCHAR(20) DEFAULT 'USER' COMMENT '角色：ADMIN / USER',
    status TINYINT(1) DEFAULT 1 COMMENT '状态（0禁用，1启用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_login_time DATETIME COMMENT '最后登录时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除（预留）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT COMMENT '操作用户 ID',
    username VARCHAR(50) COMMENT '用户名',
    operation VARCHAR(200) COMMENT '操作描述',
    ip VARCHAR(50) COMMENT '请求 IP',
    params TEXT COMMENT '请求参数',
    result TEXT COMMENT '操作结果',
    duration INT COMMENT '耗时（毫秒）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

CREATE TABLE IF NOT EXISTS ai_chat_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT COMMENT '用户 ID',
    question TEXT COMMENT '用户问题',
    answer TEXT COMMENT 'AI 回答',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提问时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话记录表';

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

UPDATE sys_user
SET password = '$2a$10$6gNEt.GLZZqX7nS1swTws.9PcjLit98sOjE8VyZG4RbQUE/GrP7eK',
    role = 'ADMIN',
    status = 1,
    deleted = 0
WHERE username = 'admin';
