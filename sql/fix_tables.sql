USE datapulse_db;

DROP TABLE IF EXISTS sys_table_config;
CREATE TABLE sys_table_config (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  datasource_id BIGINT NOT NULL COMMENT '关联数据源ID',
  table_name VARCHAR(128) NOT NULL COMMENT '物理表名',
  table_comment VARCHAR(255) DEFAULT NULL COMMENT '业务表别名/描述',
  is_whitelisted TINYINT NOT NULL DEFAULT 1 COMMENT '是否纳入Agent分析白名单: 1是 0否',
  custom_prompt_rules TEXT DEFAULT NULL COMMENT '该表特殊的业务计算规则说明',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ds_table (datasource_id, table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数仓表动态白名单与业务语义配置表';

INSERT INTO sys_table_config (datasource_id, table_name, table_comment, is_whitelisted)
VALUES 
(1, 'dim_customer', '客户多维画像字典表', 1),
(1, 'dim_product', '产品主数据与定价表', 1),
(1, 'dim_department', '组织架构销售部门表', 1),
(1, 'fact_order', '企业核心销售订单交易事实表', 1),
(1, 'fact_refund', '售后退款流水事实表', 1);
