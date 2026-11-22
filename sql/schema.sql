-- ========================================================
-- DataPulse Enterprise Text-to-SQL & Analytics Agent Platform
-- Database Schema Definition (MySQL 8.0)
-- Database: datapulse_db
-- ========================================================

CREATE DATABASE IF NOT EXISTS `datapulse_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `datapulse_db`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------------
-- 1. 系统权限与用户表 (sys_user)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(64) NOT NULL COMMENT '登录用户名',
  `password` VARCHAR(128) NOT NULL COMMENT 'BCrypt哈希加密密码',
  `real_name` VARCHAR(64) NOT NULL COMMENT '真实姓名',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '工作邮箱',
  `role` VARCHAR(32) NOT NULL DEFAULT 'ROLE_ANALYST' COMMENT '角色权限: ROLE_ADMIN(管理员), ROLE_ANALYST(数据分析师), ROLE_BUSINESS(业务人员)',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态: 1正常 0禁用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业系统用户表';

-- --------------------------------------------------------
-- 2. 自然语言查询审计与生成历史表 (sys_query_history)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `sys_query_history`;
CREATE TABLE `sys_query_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '发起提问用户ID',
  `user_prompt` TEXT NOT NULL COMMENT '用户自然语言输入提问',
  `generated_sql` TEXT NOT NULL COMMENT 'AI Agent生成的标准SQL',
  `is_safe` TINYINT NOT NULL DEFAULT 1 COMMENT 'AST安全审计结果: 1通过 0被拦截',
  `safety_reason` TEXT DEFAULT NULL COMMENT '安全审计说明/拦截原因',
  `execution_time_ms` BIGINT DEFAULT 0 COMMENT 'SQL底层实际执行耗时(毫秒)',
  `row_count` INT DEFAULT 0 COMMENT '返回数据行数',
  `chart_type` VARCHAR(32) DEFAULT 'table' COMMENT '自动推断图表类型: table, bar, line, pie',
  `insight_summary` TEXT DEFAULT NULL COMMENT 'AI生成的业务洞察与决策建议',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '查询发起时间',
  PRIMARY KEY (`id`),
  KEY `idx_query_user` (`user_id`),
  KEY `idx_query_time` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Text-to-SQL查询历史与审计日志表';

-- --------------------------------------------------------
-- 3. 客户维度表 (dim_customer)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `dim_customer`;
CREATE TABLE `dim_customer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '客户ID',
  `customer_code` VARCHAR(64) NOT NULL COMMENT '客户编号',
  `customer_name` VARCHAR(128) NOT NULL COMMENT '客户姓名/企业名称',
  `region` VARCHAR(32) NOT NULL COMMENT '所属大区: 华东, 华南, 华北, 西南, 华中',
  `province` VARCHAR(64) NOT NULL COMMENT '所在省份',
  `city` VARCHAR(64) NOT NULL COMMENT '所在城市',
  `tier` VARCHAR(32) NOT NULL DEFAULT 'REGULAR' COMMENT '客户等级: VIP(大客户), GOLD(黄金企业), REGULAR(普通客户)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_code` (`customer_code`),
  KEY `idx_cust_region` (`region`),
  KEY `idx_cust_tier` (`tier`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业数仓-客户维度表';

-- --------------------------------------------------------
-- 4. 商品维度表 (dim_product)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `dim_product`;
CREATE TABLE `dim_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '产品ID',
  `product_code` VARCHAR(64) NOT NULL COMMENT '产品编码',
  `product_name` VARCHAR(128) NOT NULL COMMENT '产品名称',
  `category` VARCHAR(64) NOT NULL COMMENT '产品品类: 云计算服务, 智能硬件, 企业SaaS, 外设办公, 技术咨询',
  `unit_price` DECIMAL(10,2) NOT NULL COMMENT '标准销售单价(元)',
  `cost_price` DECIMAL(10,2) NOT NULL COMMENT '采购成本单价(元)',
  `stock_quantity` INT NOT NULL DEFAULT 0 COMMENT '当前可用库存数量',
  `status` VARCHAR(32) NOT NULL DEFAULT 'ON_SALE' COMMENT '销售状态: ON_SALE(在售), OFF_SHELF(下架)',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上架时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_code` (`product_code`),
  KEY `idx_prod_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业数仓-产品维度表';

-- --------------------------------------------------------
-- 5. 销售部门维度表 (dim_department)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `dim_department`;
CREATE TABLE `dim_department` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `dept_code` VARCHAR(64) NOT NULL COMMENT '部门编码',
  `dept_name` VARCHAR(64) NOT NULL COMMENT '部门名称: 华东大客户事业部, 华南直销一部, 华北大区政企组, 华中渠道拓展部, 西南创新业务部',
  `manager_name` VARCHAR(64) NOT NULL COMMENT '部门负责人',
  `region` VARCHAR(32) NOT NULL COMMENT '所属大区',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '设立时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_code` (`dept_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业数仓-部门维度表';

-- --------------------------------------------------------
-- 6. 销售订单事实表 (fact_order)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `fact_order`;
CREATE TABLE `fact_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单主键ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单流水号',
  `customer_id` BIGINT NOT NULL COMMENT '关联客户ID(外键关联dim_customer)',
  `product_id` BIGINT NOT NULL COMMENT '关联产品ID(外键关联dim_product)',
  `dept_id` BIGINT NOT NULL COMMENT '归属销售部门ID(外键关联dim_department)',
  `quantity` INT NOT NULL DEFAULT 1 COMMENT '购买数量',
  `unit_price` DECIMAL(10,2) NOT NULL COMMENT '成交单价(元)',
  `discount_rate` DECIMAL(4,2) NOT NULL DEFAULT 1.00 COMMENT '折扣率: 1.00无折扣, 0.85为八五折',
  `total_amount` DECIMAL(12,2) NOT NULL COMMENT '实付总销售金额(元)',
  `profit_amount` DECIMAL(12,2) NOT NULL COMMENT '销售毛利润(元)',
  `order_status` VARCHAR(32) NOT NULL DEFAULT 'COMPLETED' COMMENT '订单状态: COMPLETED(已履约), REFUNDED(已退款), PENDING_PAY(待付款)',
  `pay_method` VARCHAR(32) NOT NULL DEFAULT 'ALIPAY' COMMENT '支付渠道: ALIPAY(支付宝), WECHAT(微信支付), BANK_TRANSFER(对公转账)',
  `order_date` DATE NOT NULL COMMENT '业务订单日期',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_order_date` (`order_date`),
  KEY `idx_order_cust` (`customer_id`),
  KEY `idx_order_prod` (`product_id`),
  KEY `idx_order_dept` (`dept_id`),
  KEY `idx_order_status` (`order_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业数仓-销售订单事实表';

-- --------------------------------------------------------
-- 7. 售后退款事实表 (fact_refund)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `fact_refund`;
CREATE TABLE `fact_refund` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '退款主键ID',
  `refund_no` VARCHAR(64) NOT NULL COMMENT '退款流水号',
  `order_id` BIGINT NOT NULL COMMENT '关联原始订单ID(外键关联fact_order)',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID(外键关联dim_customer)',
  `refund_amount` DECIMAL(12,2) NOT NULL COMMENT '实际退款金额(元)',
  `refund_reason` VARCHAR(64) NOT NULL COMMENT '退款原因分类: 质量缺陷, 物流破损, 客户需求变更, 误购无理由, 服务不符',
  `refund_status` VARCHAR(32) NOT NULL DEFAULT 'APPROVED' COMMENT '审批状态: APPROVED(已退款), REJECTED(驳回), PROCESSING(处理中)',
  `refund_date` DATE NOT NULL COMMENT '退款申请日期',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_refund_order` (`order_id`),
  KEY `idx_refund_date` (`refund_date`),
  KEY `idx_refund_reason` (`refund_reason`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='企业数仓-售后退款事实表';

-- --------------------------------------------------------
-- 8. 动态多数据源连接池配置表 (sys_datasource)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `sys_datasource`;
CREATE TABLE `sys_datasource` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(128) NOT NULL COMMENT '数据源名称',
  `db_type` VARCHAR(32) NOT NULL DEFAULT 'MYSQL' COMMENT '数据库类型: MYSQL, POSTGRESQL',
  `host` VARCHAR(128) NOT NULL DEFAULT '127.0.0.1' COMMENT '主机地址',
  `port` INT NOT NULL DEFAULT 3306 COMMENT '端口号',
  `database_name` VARCHAR(128) NOT NULL COMMENT '目标数据库名称',
  `username` VARCHAR(64) NOT NULL COMMENT '连接账号',
  `password` VARCHAR(128) NOT NULL COMMENT '连接密码',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认数仓: 1是 0否',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1在线 0离线',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态多数据源连接池配置表';

-- --------------------------------------------------------
-- 9. 数仓表动态白名单与业务语义配置表 (sys_table_config)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `sys_table_config`;
CREATE TABLE `sys_table_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `datasource_id` BIGINT NOT NULL COMMENT '关联数据源ID',
  `table_name` VARCHAR(128) NOT NULL COMMENT '物理表名',
  `table_comment` VARCHAR(255) DEFAULT NULL COMMENT '业务表别名/描述',
  `is_whitelisted` TINYINT NOT NULL DEFAULT 1 COMMENT '是否纳入Agent分析白名单: 1是 0否',
  `custom_prompt_rules` TEXT DEFAULT NULL COMMENT '该表特殊的业务计算规则说明',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ds_table` (`datasource_id`, `table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数仓表动态白名单与业务语义配置表';

-- --------------------------------------------------------
-- 10. Text-to-SQL Few-Shot 示例向量检索库 (sys_sql_example)
-- --------------------------------------------------------
DROP TABLE IF EXISTS `sys_sql_example`;
CREATE TABLE IF NOT EXISTS `sys_sql_example` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `datasource_id` BIGINT NOT NULL DEFAULT 1,
  `question` VARCHAR(500) NOT NULL,
  `sql_text` TEXT NOT NULL,
  `embedding` MEDIUMTEXT NULL COMMENT 'JSON float array',
  `embedding_model` VARCHAR(100) NULL,
  `source` VARCHAR(20) NOT NULL DEFAULT 'SEED',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_ds` (`datasource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Text-to-SQL Few-Shot 问答对示例表';

SET FOREIGN_KEY_CHECKS = 1;
