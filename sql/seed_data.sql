-- ========================================================
-- DataPulse Enterprise Text-to-SQL & Analytics Agent Platform
-- Seed Data Definition (Realistic Enterprise Data Warehouse)
-- Database: datapulse_db
-- ========================================================

USE `datapulse_db`;
SET NAMES utf8mb4;

-- --------------------------------------------------------
-- 1. 用户种子数据 (密码: admin123 / data123，BCrypt加盐)
-- --------------------------------------------------------
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `email`, `role`, `avatar`, `status`) VALUES
(1, 'admin', '$2b$12$2IoV8XqEyvtD4sh/mwUDeOUzmBffUVImQz7adY955FDoQ5Psldnk.', '赵岚 (企业总架构师)', '3512473224@qq.com', 'ROLE_ADMIN', 'https://api.dicebear.com/7.x/notionists/svg?seed=admin', 1),
(2, 'analyst', '$2b$12$xMlm/aJG7gQ5YPX8G4F6KuKXLM3TlGTGnENy6D24TGC2E/MQtyT8G', '李晨 (资深数据分析师)', 'ppday666@gmail.com', 'ROLE_ANALYST', 'https://api.dicebear.com/7.x/notionists/svg?seed=analyst', 1);

-- --------------------------------------------------------
-- 2. 部门维度数据 (dim_department)
-- --------------------------------------------------------
INSERT INTO `dim_department` (`id`, `dept_code`, `dept_name`, `manager_name`, `region`, `created_at`) VALUES
(1, 'DEPT-EAST-01', '华东大客户事业部', '张明 (华东总监)', '华东', '2024-01-15 09:00:00'),
(2, 'DEPT-SOUTH-01', '华南直销一部', '林志华 (华南总监)', '华南', '2024-01-15 09:00:00'),
(3, 'DEPT-NORTH-01', '华北大区政企组', '郭建国 (华北总监)', '华北', '2024-02-01 09:00:00'),
(4, 'DEPT-CENTRAL-01', '华中渠道拓展部', '周雨欣 (华中总监)', '华中', '2024-03-10 09:00:00'),
(5, 'DEPT-WEST-01', '西南创新业务部', '赵静 (西南总监)', '西南', '2024-04-05 09:00:00');

-- --------------------------------------------------------
-- 3. 产品维度数据 (dim_product)
-- --------------------------------------------------------
INSERT INTO `dim_product` (`id`, `product_code`, `product_name`, `category`, `unit_price`, `cost_price`, `stock_quantity`, `status`, `created_at`) VALUES
(1, 'PROD-CLOUD-01', '云原生微服务治理套件', '云计算服务', 12800.00, 4200.00, 500, 'ON_SALE', '2025-01-01 10:00:00'),
(2, 'PROD-HARD-01', '企业级 AI 知识库一体机', '智能硬件', 35800.00, 16800.00, 120, 'ON_SALE', '2025-01-15 10:00:00'),
(3, 'PROD-SAAS-01', '敏捷协作与工单 SaaS 年卡', '企业SaaS', 4800.00, 800.00, 9999, 'ON_SALE', '2025-02-01 10:00:00'),
(4, 'PROD-HARD-02', '4K 超清智能交互会议屏', '智能硬件', 18900.00, 9500.00, 85, 'ON_SALE', '2025-03-01 10:00:00'),
(5, 'PROD-HARD-03', '边缘计算物联网网关 Pro', '智能硬件', 6200.00, 2800.00, 340, 'ON_SALE', '2025-03-15 10:00:00'),
(6, 'PROD-CLOUD-02', '分布式数据库中间件授权', '云计算服务', 24000.00, 6500.00, 300, 'ON_SALE', '2025-04-01 10:00:00'),
(7, 'PROD-OFFICE-01', '人体工学机械升降会议桌', '外设办公', 3200.00, 1400.00, 210, 'ON_SALE', '2025-05-01 10:00:00'),
(8, 'PROD-OFFICE-02', '高保真全向降噪麦克风', '外设办公', 1580.00, 620.00, 650, 'ON_SALE', '2025-05-15 10:00:00'),
(9, 'PROD-CONS-01', '金牌技术驻场架构咨询服务', '技术咨询', 50000.00, 22000.00, 50, 'ON_SALE', '2025-06-01 10:00:00'),
(10, 'PROD-CONS-02', '安全合规与等保三级审计服务', '技术咨询', 38000.00, 15000.00, 80, 'ON_SALE', '2025-06-15 10:00:00');

-- --------------------------------------------------------
-- 4. 客户维度数据 (dim_customer)
-- --------------------------------------------------------
INSERT INTO `dim_customer` (`id`, `customer_code`, `customer_name`, `region`, `province`, `city`, `tier`, `created_at`) VALUES
(1, 'CUST-001', '蔚蓝云创智能科技有限公司', '华东', '上海市', '上海', 'VIP', '2025-01-10 11:00:00'),
(2, 'CUST-002', '极光互联物流股份有限公司', '华东', '浙江省', '杭州', 'VIP', '2025-01-15 11:00:00'),
(3, 'CUST-003', '广汇智能制造装备集团', '华南', '广东省', '深圳', 'VIP', '2025-02-05 11:00:00'),
(4, 'CUST-004', '鼎泰数字金融研究院', '华北', '北京市', '北京', 'VIP', '2025-02-20 11:00:00'),
(5, 'CUST-005', '楚天楚誉现代医药连锁', '华中', '湖北省', '武汉', 'GOLD', '2025-03-01 11:00:00'),
(6, 'CUST-006', '天府文旅数智化创新实验室', '西南', '四川省', '成都', 'GOLD', '2025-03-12 11:00:00'),
(7, 'CUST-007', '苏宁智慧零售供应链', '华东', '江苏省', '南京', 'GOLD', '2025-04-01 11:00:00'),
(8, 'CUST-008', '南粤新能源车联网运营中心', '华南', '广东省', '广州', 'GOLD', '2025-04-18 11:00:00'),
(9, 'CUST-009', '渤海重工高端工业软件部', '华北', '天津市', '天津', 'REGULAR', '2025-05-02 11:00:00'),
(10, 'CUST-010', '三江数字农业生态示范基地', '华中', '湖南省', '长沙', 'REGULAR', '2025-05-20 11:00:00'),
(11, 'CUST-011', '西南电力信息安全检测中心', '西南', '重庆市', '重庆', 'REGULAR', '2025-06-08 11:00:00'),
(12, 'CUST-012', '钱塘跨境电商贸易综合体', '华东', '浙江省', '宁波', 'REGULAR', '2025-06-25 11:00:00');

-- --------------------------------------------------------
-- 5. 销售订单事实表数据 (fact_order) - 跨2026年真实经营数据
-- --------------------------------------------------------
INSERT INTO `fact_order` (`id`, `order_no`, `customer_id`, `product_id`, `dept_id`, `quantity`, `unit_price`, `discount_rate`, `total_amount`, `profit_amount`, `order_status`, `pay_method`, `order_date`, `created_at`) VALUES
(1, 'ORD-20260110-001', 1, 2, 1, 2, 35800.00, 0.90, 64440.00, 30840.00, 'COMPLETED', 'BANK_TRANSFER', '2026-01-10', '2026-01-10 14:20:00'),
(2, 'ORD-20260115-002', 2, 1, 1, 3, 12800.00, 0.95, 36480.00, 23880.00, 'COMPLETED', 'ALIPAY', '2026-01-15', '2026-01-15 16:35:00'),
(3, 'ORD-20260122-003', 3, 6, 2, 1, 24000.00, 1.00, 24000.00, 17500.00, 'COMPLETED', 'BANK_TRANSFER', '2026-01-22', '2026-01-22 10:12:00'),
(4, 'ORD-20260205-004', 4, 9, 3, 1, 50000.00, 0.85, 42500.00, 20500.00, 'COMPLETED', 'BANK_TRANSFER', '2026-02-05', '2026-02-05 09:40:00'),
(5, 'ORD-20260218-005', 5, 3, 4, 5, 4800.00, 0.90, 21600.00, 17600.00, 'COMPLETED', 'WECHAT', '2026-02-18', '2026-02-18 11:25:00'),
(6, 'ORD-20260302-006', 6, 4, 5, 2, 18900.00, 0.95, 35910.00, 16910.00, 'COMPLETED', 'BANK_TRANSFER', '2026-03-02', '2026-03-02 15:50:00'),
(7, 'ORD-20260315-007', 7, 5, 1, 10, 6200.00, 0.88, 54560.00, 26560.00, 'COMPLETED', 'BANK_TRANSFER', '2026-03-15', '2026-03-15 13:10:00'),
(8, 'ORD-20260328-008', 8, 2, 2, 1, 35800.00, 1.00, 35800.00, 19000.00, 'COMPLETED', 'ALIPAY', '2026-03-28', '2026-03-28 17:05:00'),
(9, 'ORD-20260408-009', 1, 10, 1, 1, 38000.00, 0.90, 34200.00, 19200.00, 'COMPLETED', 'BANK_TRANSFER', '2026-04-08', '2026-04-08 10:30:00'),
(10, 'ORD-20260420-010', 9, 7, 3, 4, 3200.00, 0.95, 12160.00, 6560.00, 'COMPLETED', 'WECHAT', '2026-04-20', '2026-04-20 14:15:00'),
(11, 'ORD-20260506-011', 10, 8, 4, 6, 1580.00, 1.00, 9480.00, 5760.00, 'COMPLETED', 'ALIPAY', '2026-05-06', '2026-05-06 11:45:00'),
(12, 'ORD-20260519-012', 11, 5, 5, 3, 6200.00, 0.92, 17112.00, 8712.00, 'COMPLETED', 'BANK_TRANSFER', '2026-05-19', '2026-05-19 16:20:00'),
(13, 'ORD-20260601-013', 2, 6, 1, 2, 24000.00, 0.88, 42240.00, 29240.00, 'COMPLETED', 'BANK_TRANSFER', '2026-06-01', '2026-06-01 09:15:00'),
(14, 'ORD-20260615-014', 3, 1, 2, 5, 12800.00, 0.85, 54400.00, 33400.00, 'COMPLETED', 'BANK_TRANSFER', '2026-06-15', '2026-06-15 15:40:00'),
(15, 'ORD-20260628-015', 12, 4, 1, 1, 18900.00, 1.00, 18900.00, 9400.00, 'REFUNDED', 'ALIPAY', '2026-06-28', '2026-06-28 17:30:00'),
(16, 'ORD-20260705-016', 4, 2, 3, 3, 35800.00, 0.85, 91290.00, 40890.00, 'COMPLETED', 'BANK_TRANSFER', '2026-07-05', '2026-07-05 10:50:00'),
(17, 'ORD-20260718-017', 5, 7, 4, 8, 3200.00, 0.90, 23040.00, 11840.00, 'COMPLETED', 'BANK_TRANSFER', '2026-07-18', '2026-07-18 14:00:00'),
(18, 'ORD-20260729-018', 6, 1, 5, 2, 12800.00, 0.95, 24320.00, 15920.00, 'COMPLETED', 'ALIPAY', '2026-07-29', '2026-07-29 16:45:00'),
(19, 'ORD-20260810-019', 7, 9, 1, 1, 50000.00, 0.90, 45000.00, 23000.00, 'COMPLETED', 'BANK_TRANSFER', '2026-08-10', '2026-08-10 11:10:00'),
(20, 'ORD-20260822-020', 8, 3, 2, 10, 4800.00, 0.80, 38400.00, 30400.00, 'COMPLETED', 'WECHAT', '2026-08-22', '2026-08-22 15:20:00'),
(21, 'ORD-20260902-021', 1, 4, 1, 2, 18900.00, 0.90, 34020.00, 15020.00, 'COMPLETED', 'BANK_TRANSFER', '2026-09-02', '2026-09-02 09:30:00'),
(22, 'ORD-20260914-022', 2, 5, 1, 5, 6200.00, 0.95, 29450.00, 15450.00, 'COMPLETED', 'ALIPAY', '2026-09-14', '2026-09-14 14:35:00'),
(23, 'ORD-20260925-023', 3, 2, 2, 2, 35800.00, 0.92, 65872.00, 32272.00, 'COMPLETED', 'BANK_TRANSFER', '2026-09-25', '2026-09-25 16:50:00'),
(24, 'ORD-20261001-024', 4, 6, 3, 2, 24000.00, 0.90, 43200.00, 30200.00, 'COMPLETED', 'BANK_TRANSFER', '2026-10-01', '2026-10-01 10:15:00'),
(25, 'ORD-20261002-025', 9, 8, 3, 10, 1580.00, 0.85, 13430.00, 7230.00, 'COMPLETED', 'WECHAT', '2026-10-02', '2026-10-02 11:40:00');

-- --------------------------------------------------------
-- 6. 售后退款事实表数据 (fact_refund)
-- --------------------------------------------------------
INSERT INTO `fact_refund` (`id`, `refund_no`, `order_id`, `customer_id`, `refund_amount`, `refund_reason`, `refund_status`, `refund_date`, `created_at`) VALUES
(1, 'REF-20260630-001', 15, 12, 18900.00, '物流破损', 'APPROVED', '2026-06-30', '2026-06-30 10:00:00'),
(2, 'REF-20260710-002', 10, 9, 3040.00, '客户需求变更', 'APPROVED', '2026-07-10', '2026-07-10 14:30:00'),
(3, 'REF-20260815-003', 11, 10, 1580.00, '质量缺陷', 'APPROVED', '2026-08-15', '2026-08-15 16:00:00'),
(4, 'REF-20260920-004', 18, 6, 2000.00, '误购无理由', 'REJECTED', '2026-09-20', '2026-09-20 11:20:00');

-- --------------------------------------------------------
-- 7. 历史查询审计种子数据 (sys_query_history)
-- --------------------------------------------------------
INSERT INTO `sys_query_history` (`id`, `user_id`, `user_prompt`, `generated_sql`, `is_safe`, `safety_reason`, `execution_time_ms`, `row_count`, `chart_type`, `insight_summary`, `created_at`) VALUES
(1, 1, '统计 2026 年各销售大区的总销售额与利润，按销售额降序排列', 
'SELECT d.region AS 大区, SUM(o.total_amount) AS 总销售额, SUM(o.profit_amount) AS 总利润 FROM fact_order o JOIN dim_department d ON o.dept_id = d.id WHERE o.order_status = \'COMPLETED\' GROUP BY d.region ORDER BY 总销售额 DESC LIMIT 100;', 
1, 'PASS', 12, 5, 'bar', 
'2026年华东与华南大区业绩处于领先地位，其中华东贡献超35%总营收；华中大区毛利率最高达到71.2%，可加大高毛利SaaS软件在华中的渠道拓展。', 
'2026-10-03 10:00:00'),

(2, 1, '查询各产品品类的退款总金额和退款单数', 
'SELECT p.category AS 产品品类, COUNT(r.id) AS 退款笔数, SUM(r.refund_amount) AS 退款总额 FROM fact_refund r JOIN fact_order o ON r.order_id = o.id JOIN dim_product p ON o.product_id = p.id WHERE r.refund_status = \'APPROVED\' GROUP BY p.category ORDER BY 退款总额 DESC LIMIT 100;', 
1, 'PASS', 15, 3, 'pie', 
'智能硬件由于长途物流运输破损率偏高，退款总额达1.89万元居首位；建议优化硬件出厂减震包材与物流合作商考核标准。', 
'2026-10-03 10:30:00');

-- --------------------------------------------------------
-- 8. 动态数据源与表级语义规则种子数据
-- --------------------------------------------------------
INSERT INTO `sys_datasource` (`id`, `name`, `db_type`, `host`, `port`, `database_name`, `username`, `password`, `is_default`, `status`) VALUES
(1, '企业核心经营数仓 (Star Schema)', 'MYSQL', '127.0.0.1', 3306, 'datapulse_db', 'root', '123456', 1, 1),
(2, 'PulseTicket 演出票务异构库', 'MYSQL', '127.0.0.1', 3306, 'ticket_show', 'root', '123456', 0, 1),
(3, 'PulseTicket 订单交易异构库', 'MYSQL', '127.0.0.1', 3306, 'ticket_order', 'root', '123456', 0, 1);

INSERT INTO `sys_table_config` (`datasource_id`, `table_name`, `table_comment`, `is_whitelisted`, `custom_prompt_rules`) VALUES
(1, 'dim_customer', '客户多维画像字典表', 1, 'tier字段枚举：VIP=大客户，GOLD=黄金企业，REGULAR=普通客户；region代表华东/华南/华北/华中/西南五大区'),
(1, 'dim_product', '产品主数据与定价表', 1, 'status=ON_SALE为在售商品；毛利率计算公式：(unit_price - cost_price) / unit_price'),
(1, 'dim_department', '组织架构销售部门表', 1, '通过 id 与 fact_order.dept_id 关联，统计部门与大区业绩'),
(1, 'fact_order', '企业核心销售订单交易事实表', 1, '有效GMV与营收统计需满足 order_status = COMPLETED（剔除 REFUNDED 已退款订单）；total_amount 为实付总金额，profit_amount 为销售毛利润'),
(1, 'fact_refund', '售后退款流水事实表', 1, '实际发生退款需筛选 refund_status = APPROVED；通过 order_id 关联 fact_order.id'),
(2, 'show_event', '演出项目主表', 1, 'status=1表示在售演出；category为演出分类'),
(2, 'show_session', '演出场次与票档库存表', 1, '通过 show_id 关联 show_event.id；available_seats 为当前可售余票'),
(2, 'seat_inventory', '场馆座位级实时库存表', 1, 'status枚举：AVAILABLE=可售，LOCKED=已锁定待支付，SOLD=已售出；通过 session_id 关联 show_session.id'),
(2, 'undo_log', 'Seata分布式事务回滚日志表(内部系统表)', 0, NULL),
(3, 'ticket_order', '演出票务主订单表', 1, 'status枚举：0=待支付(UNPAID)，1=已支付(PAID)，2=超时自动关单(CLOSED)；total_amount为订单总金额'),
(3, 'ticket_order_item', '订单座位明细表', 1, '通过 order_id 关联 ticket_order.id'),
(3, 'seata_state_audit', 'Seata事务状态审计表(内部系统表)', 0, NULL),
(3, 'undo_log', 'Seata分布式事务回滚日志表(内部系统表)', 0, NULL);

-- --------------------------------------------------------
-- 9. Text-to-SQL Few-Shot 示例种子数据 (sys_sql_example)
-- --------------------------------------------------------
INSERT INTO `sys_sql_example` (`datasource_id`, `question`, `sql_text`, `source`) VALUES
(1, '统计2026年各销售大区的有效销售额与毛利润排行榜（剔除退款单）',
 'SELECT d.region AS `销售大区`, SUM(o.total_amount) AS `有效销售额`, SUM(o.profit_amount) AS `毛利润` FROM fact_order o JOIN dim_department d ON o.dept_id = d.id WHERE o.order_status = ''COMPLETED'' AND YEAR(o.order_date) = 2026 GROUP BY d.region ORDER BY `有效销售额` DESC LIMIT 100;',
 'SEED'),
(1, '按月份统计2026年成交订单量与有效GMV月度趋势',
 'SELECT DATE_FORMAT(o.order_date, ''%Y-%m'') AS `月份`, COUNT(o.id) AS `成交订单数`, SUM(o.total_amount) AS `有效GMV` FROM fact_order o WHERE o.order_status = ''COMPLETED'' AND YEAR(o.order_date) = 2026 GROUP BY DATE_FORMAT(o.order_date, ''%Y-%m'') ORDER BY `月份` ASC LIMIT 100;',
 'SEED'),
(1, '统计各产品品类的有效销售额、毛利润与毛利率对比',
 'SELECT p.category AS `产品品类`, SUM(o.total_amount) AS `有效销售额`, SUM(o.profit_amount) AS `毛利润`, ROUND(SUM(o.profit_amount) / NULLIF(SUM(o.total_amount), 0) * 100, 2) AS `毛利率百分比` FROM fact_order o JOIN dim_product p ON o.product_id = p.id WHERE o.order_status = ''COMPLETED'' GROUP BY p.category ORDER BY `有效销售额` DESC LIMIT 100;',
 'SEED'),
(1, '查出销售额排名前5的畅销商品及销量',
 'SELECT p.product_name AS `产品名称`, p.category AS `产品品类`, SUM(o.quantity) AS `销售数量`, SUM(o.total_amount) AS `销售额` FROM fact_order o JOIN dim_product p ON o.product_id = p.id WHERE o.order_status = ''COMPLETED'' GROUP BY p.id, p.product_name, p.category ORDER BY `销售额` DESC LIMIT 5;',
 'SEED'),
(1, '按客户等级（VIP/黄金/普通）统计客户数与累计消费贡献',
 'SELECT c.tier AS `客户等级`, COUNT(DISTINCT c.id) AS `客户数`, COUNT(o.id) AS `订单数`, SUM(o.total_amount) AS `累计消费金额` FROM fact_order o JOIN dim_customer c ON o.customer_id = c.id WHERE o.order_status = ''COMPLETED'' GROUP BY c.tier ORDER BY `累计消费金额` DESC LIMIT 100;',
 'SEED'),
(1, '分析各产品品类的已审批退款总额、退款单数与金额退款率',
 'SELECT p.category AS `产品品类`, COUNT(DISTINCT o.id) AS `总订单数`, COUNT(DISTINCT CASE WHEN r.refund_status = ''APPROVED'' THEN r.id END) AS `已审批退款单数`, COALESCE(SUM(CASE WHEN r.refund_status = ''APPROVED'' THEN r.refund_amount ELSE 0 END), 0) AS `已退款总额`, ROUND(COALESCE(SUM(CASE WHEN r.refund_status = ''APPROVED'' THEN r.refund_amount ELSE 0 END), 0) / NULLIF(SUM(o.total_amount), 0) * 100, 2) AS `金额退款率百分比` FROM fact_order o JOIN dim_product p ON o.product_id = p.id LEFT JOIN fact_refund r ON r.order_id = o.id GROUP BY p.category ORDER BY `已退款总额` DESC LIMIT 100;',
 'SEED'),
(1, '统计各支付渠道的成交订单笔数与实付总额分布',
 'SELECT o.pay_method AS `支付渠道`, COUNT(o.id) AS `订单笔数`, SUM(o.total_amount) AS `实付总额` FROM fact_order o WHERE o.order_status = ''COMPLETED'' GROUP BY o.pay_method ORDER BY `实付总额` DESC LIMIT 100;',
 'SEED');

