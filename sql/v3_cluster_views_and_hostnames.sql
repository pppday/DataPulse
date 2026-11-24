-- =====================================================================
-- DataPulse v3: 跨微服务只读分析宽表视图 + VPC 内网集群服务发现域名配置
-- =====================================================================

-- 1. PulseTicket 演出票务库 (ticket_show) 跨库客户消费宽表视图（自动关联 ticket_user.user_account 显示真实昵称与用户名，剥离敏感密码与手机号）
CREATE OR REPLACE VIEW ticket_show.v_ticket_sales_customer AS
SELECT
    si.id AS seat_id,
    si.locked_user_id AS user_id,
    COALESCE(NULLIF(ua.nickname, ''), ua.username, CONCAT('用户#', si.locked_user_id)) AS customer_name,
    ua.username AS username,
    ua.vip_level AS vip_level,
    sh.id AS show_id,
    sh.title AS show_title,
    sh.category AS show_category,
    sh.city AS city,
    sh.venue_name AS venue_name,
    ss.id AS session_id,
    ss.session_title AS session_title,
    ss.start_time AS start_time,
    si.seat_label AS seat_label,
    si.zone_name AS zone_name,
    si.price AS price,
    si.status AS seat_status,
    si.locked_order_no AS order_no,
    si.sold_at AS sold_at
FROM ticket_show.seat_inventory si
LEFT JOIN ticket_show.show_session ss ON si.session_id = ss.id
LEFT JOIN ticket_show.show_info sh ON si.show_id = sh.id
LEFT JOIN ticket_user.user_account ua ON si.locked_user_id = ua.id;

-- 2. PulseTicket 订单交易库 (ticket_order) 跨库订单与客户宽表视图
CREATE OR REPLACE VIEW ticket_order.v_order_customer_detail AS
SELECT
    o.id AS order_id,
    o.order_no AS order_no,
    o.user_id AS user_id,
    COALESCE(NULLIF(ua.nickname, ''), o.username, CONCAT('用户#', o.user_id)) AS customer_name,
    COALESCE(ua.username, o.username) AS username,
    ua.vip_level AS vip_level,
    ua.points_balance AS points_balance,
    o.show_id AS show_id,
    o.show_title AS show_title,
    o.venue_name AS venue_name,
    o.session_id AS session_id,
    o.session_title AS session_title,
    o.start_time AS start_time,
    o.seat_count AS seat_count,
    o.total_amount AS total_amount,
    o.status AS order_status,
    o.close_reason AS close_reason,
    o.paid_at AS paid_at,
    o.created_at AS created_at
FROM ticket_order.ticket_order o
LEFT JOIN ticket_user.user_account ua ON o.user_id = ua.id;

-- 3. PulseTicket 用户会员库 (ticket_user) 脱敏会员画像视图（严禁暴露 password_hash，手机号中间脱敏）
CREATE OR REPLACE VIEW ticket_user.v_member_profile AS
SELECT
    id AS user_id,
    username,
    COALESCE(NULLIF(nickname, ''), username) AS customer_name,
    CASE
        WHEN phone IS NOT NULL AND CHAR_LENGTH(phone) >= 7
        THEN CONCAT(LEFT(phone, 3), '****', RIGHT(phone, 4))
        ELSE '***'
    END AS phone_masked,
    vip_level,
    role,
    points_balance,
    total_orders,
    created_at
FROM ticket_user.user_account;

-- 4. 将 DataPulse 数据源升级为真实可解析的 VPC 内网集群域名（配合 /etc/hosts 内部服务发现）
UPDATE datapulse_db.sys_datasource SET host = 'dw-master.datapulse.internal' WHERE id = 1 AND database_name = 'datapulse_db';
UPDATE datapulse_db.sys_datasource SET host = 'mysql-show.pulseticket.internal' WHERE id = 2 AND database_name = 'ticket_show';
UPDATE datapulse_db.sys_datasource SET host = 'mysql-order.pulseticket.internal' WHERE id = 3 AND database_name = 'ticket_order';
UPDATE datapulse_db.sys_datasource SET host = 'mysql-risk.pulseflow.internal' WHERE id = 4 AND database_name = 'pulseflow_db';

-- 如尚未挂载 PulseTicket 用户会员库，则自动接入（复用同实例密码）
INSERT INTO datapulse_db.sys_datasource (name, db_type, host, port, database_name, username, password, is_default, status, created_at, updated_at)
SELECT 'PulseTicket 用户会员异构库', 'MYSQL', 'mysql-user.pulseticket.internal', 3306, 'ticket_user',
       username, password, 0, 1, NOW(), NOW()
FROM datapulse_db.sys_datasource
WHERE id = 3
  AND NOT EXISTS (SELECT 1 FROM datapulse_db.sys_datasource WHERE database_name = 'ticket_user');

-- 5. 校正并完善 sys_table_config 白名单与跨库视图语义规则
-- ds=2 (ticket_show)
DELETE FROM datapulse_db.sys_table_config WHERE datasource_id = 2 AND table_name IN ('show_event', 'v_ticket_sales_customer', 'show_info', 'show_session', 'seat_inventory', 'venue_dict');
INSERT INTO datapulse_db.sys_table_config (datasource_id, table_name, table_comment, is_whitelisted, custom_prompt_rules) VALUES
(2, 'v_ticket_sales_customer', '【跨库宽表视图】演出售票与客户消费明细', 1, '【首选分析视图】已跨库关联 ticket_show 演出/场次/座位与 ticket_user 真实客户昵称/用户名(customer_name/username/vip_level)。分析谁是最大客户、客户消费排行时请优先查本视图，务必过滤 seat_status = ''SOLD'' AND user_id IS NOT NULL，并直接展示 customer_name 与 username 而非裸 user_id；统计各演出/场馆/城市票房请过滤 seat_status = ''SOLD''。'),
(2, 'show_info', '演出基础信息表', 1, 'status 取值 ON_SALE(在售) / SOLD_OUT(售罄)；category 为 CONCERT/MUSICAL/IMAX_MOVIE/LIVEHOUSE。'),
(2, 'show_session', '演出场次与库存汇总表', 1, '通过 show_id 关联 show_info.id；available_seats 为可售余座，sold_seats 为已售座位数。'),
(2, 'seat_inventory', '物理座位库存明细表', 1, 'status 取值 AVAILABLE(可售)/LOCKED(锁定)/SOLD(已售)；若需展示购票用户名请优先改查 v_ticket_sales_customer 视图。'),
(2, 'venue_dict', '演出场馆字典表', 1, '按 city / venue_name 关联场馆信息。');

-- ds=3 (ticket_order)
DELETE FROM datapulse_db.sys_table_config WHERE datasource_id = 3 AND table_name IN ('ticket_order_item', 'seata_state_audit', 'v_order_customer_detail', 'ticket_order', 'seata_tx_audit', 'mq_event_log');
INSERT INTO datapulse_db.sys_table_config (datasource_id, table_name, table_comment, is_whitelisted, custom_prompt_rules) VALUES
(3, 'v_order_customer_detail', '【跨库宽表视图】票务订单与会员客户明细', 1, '【首选分析视图】已跨库关联 ticket_order 与 ticket_user 会员昵称/等级/积分(customer_name/username/vip_level/points_balance)。查询谁是最大客户、客户订单与实付排行时请优先查本视图，直接展示 customer_name 与 username；统计有效成交请过滤 order_status = ''PAID''，退票订单为 order_status = ''CLOSED'' AND close_reason = ''USER_REFUND''。'),
(3, 'ticket_order', '票务核心订单表', 1, 'status 为字符串：UNPAID(待支付)、PAID(已支付出票)、CLOSED(已关闭，close_reason=USER_REFUND表示退票/USER_CANCEL表示取消/TIMEOUT_UNPAID表示超时关单)；total_amount 为订单实付金额。'),
(3, 'seata_tx_audit', 'Seata 分布式事务审计表(底层隔离)', 0, NULL),
(3, 'mq_event_log', 'RabbitMQ 事件投递日志(底层隔离)', 0, NULL);

-- ds=5 (ticket_user)
SET @uid_ds := (SELECT id FROM datapulse_db.sys_datasource WHERE database_name = 'ticket_user' LIMIT 1);
DELETE FROM datapulse_db.sys_table_config WHERE datasource_id = @uid_ds;
INSERT INTO datapulse_db.sys_table_config (datasource_id, table_name, table_comment, is_whitelisted, custom_prompt_rules) VALUES
(@uid_ds, 'v_member_profile', '【脱敏宽表视图】PulseTicket 会员画像与积分总览', 1, '【首选分析视图】已脱敏隐藏 password_hash 与手机号中间位，包含 customer_name(昵称)、username、vip_level(NORMAL/GOLD/BLACK_DIAMOND)、points_balance(积分余额)、total_orders(累计单数)。'),
(@uid_ds, 'points_log', '会员观演积分收支流水表', 1, 'biz_type=ORDER_PAID_REWARD 为购票发分，ORDER_REFUND_DEDUCT 为退票冲正扣分；通过 user_id 关联 v_member_profile.user_id 展示用户名。'),
(@uid_ds, 'user_message', '电子票与退票站内信通知表', 1, '通过 user_id 关联 v_member_profile.user_id。'),
(@uid_ds, 'user_account', '底层原始用户认证表(含密码哈希，强制隔离)', 0, NULL),
(@uid_ds, 'undo_log', 'Seata AT 事务回滚日志(底层隔离)', 0, NULL);
