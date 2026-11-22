USE `datapulse_db`;
SET NAMES utf8mb4;

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

INSERT IGNORE INTO `sys_sql_example` (`id`, `datasource_id`, `question`, `sql_text`, `source`) VALUES
(1, 1, '统计2026年各销售大区的有效销售额与毛利润排行榜（剔除退款单）',
 'SELECT d.region AS `销售大区`, SUM(o.total_amount) AS `有效销售额`, SUM(o.profit_amount) AS `毛利润` FROM fact_order o JOIN dim_department d ON o.dept_id = d.id WHERE o.order_status = ''COMPLETED'' AND YEAR(o.order_date) = 2026 GROUP BY d.region ORDER BY `有效销售额` DESC LIMIT 100;',
 'SEED'),
(2, 1, '按月份统计2026年成交订单量与有效GMV月度趋势',
 'SELECT DATE_FORMAT(o.order_date, ''%Y-%m'') AS `月份`, COUNT(o.id) AS `成交订单数`, SUM(o.total_amount) AS `有效GMV` FROM fact_order o WHERE o.order_status = ''COMPLETED'' AND YEAR(o.order_date) = 2026 GROUP BY DATE_FORMAT(o.order_date, ''%Y-%m'') ORDER BY `月份` ASC LIMIT 100;',
 'SEED'),
(3, 1, '统计各产品品类的有效销售额、毛利润与毛利率对比',
 'SELECT p.category AS `产品品类`, SUM(o.total_amount) AS `有效销售额`, SUM(o.profit_amount) AS `毛利润`, ROUND(SUM(o.profit_amount) / NULLIF(SUM(o.total_amount), 0) * 100, 2) AS `毛利率百分比` FROM fact_order o JOIN dim_product p ON o.product_id = p.id WHERE o.order_status = ''COMPLETED'' GROUP BY p.category ORDER BY `有效销售额` DESC LIMIT 100;',
 'SEED'),
(4, 1, '查出销售额排名前5的畅销商品及销量',
 'SELECT p.product_name AS `产品名称`, p.category AS `产品品类`, SUM(o.quantity) AS `销售数量`, SUM(o.total_amount) AS `销售额` FROM fact_order o JOIN dim_product p ON o.product_id = p.id WHERE o.order_status = ''COMPLETED'' GROUP BY p.id, p.product_name, p.category ORDER BY `销售额` DESC LIMIT 5;',
 'SEED'),
(5, 1, '按客户等级（VIP/黄金/普通）统计客户数与累计消费贡献',
 'SELECT c.tier AS `客户等级`, COUNT(DISTINCT c.id) AS `客户数`, COUNT(o.id) AS `订单数`, SUM(o.total_amount) AS `累计消费金额` FROM fact_order o JOIN dim_customer c ON o.customer_id = c.id WHERE o.order_status = ''COMPLETED'' GROUP BY c.tier ORDER BY `累计消费金额` DESC LIMIT 100;',
 'SEED'),
(6, 1, '分析各产品品类的已审批退款总额、退款单数与金额退款率',
 'SELECT p.category AS `产品品类`, COUNT(DISTINCT o.id) AS `总订单数`, COUNT(DISTINCT CASE WHEN r.refund_status = ''APPROVED'' THEN r.id END) AS `已审批退款单数`, COALESCE(SUM(CASE WHEN r.refund_status = ''APPROVED'' THEN r.refund_amount ELSE 0 END), 0) AS `已退款总额`, ROUND(COALESCE(SUM(CASE WHEN r.refund_status = ''APPROVED'' THEN r.refund_amount ELSE 0 END), 0) / NULLIF(SUM(o.total_amount), 0) * 100, 2) AS `金额退款率百分比` FROM fact_order o JOIN dim_product p ON o.product_id = p.id LEFT JOIN fact_refund r ON r.order_id = o.id GROUP BY p.category ORDER BY `已退款总额` DESC LIMIT 100;',
 'SEED'),
(7, 1, '统计各支付渠道的成交订单笔数与实付总额分布',
 'SELECT o.pay_method AS `支付渠道`, COUNT(o.id) AS `订单笔数`, SUM(o.total_amount) AS `实付总额` FROM fact_order o WHERE o.order_status = ''COMPLETED'' GROUP BY o.pay_method ORDER BY `实付总额` DESC LIMIT 100;',
 'SEED');
