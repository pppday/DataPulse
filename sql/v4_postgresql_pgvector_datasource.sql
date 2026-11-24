-- =====================================================================
-- DataPulse v4: 接入 PostgreSQL 16 (pulseticket_rag) 异构数据源与白名单治理
-- =====================================================================

-- 1. 接入 PulseTicket RAG 与智能体轨迹库 (PostgreSQL 16 / pgvector)
INSERT INTO datapulse_db.sys_datasource
    (name, db_type, host, port, database_name, username, password, is_default, status, created_at, updated_at)
SELECT
    'PulseTicket RAG 向量与轨迹库 (PG)',
    'POSTGRESQL',
    '127.0.0.1',
    5432,
    'pulseticket_rag',
    'pulseticket',
    'pulseticket',
    0,
    1,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM datapulse_db.sys_datasource WHERE db_type = 'POSTGRESQL' AND database_name = 'pulseticket_rag'
);

-- 2. 配置 PostgreSQL 业务表白名单与语义口径（隔离 LangGraph 底层二进制 checkpoint 表）
SET @pg_ds := (SELECT id FROM datapulse_db.sys_datasource WHERE db_type = 'POSTGRESQL' AND database_name = 'pulseticket_rag' LIMIT 1);
DELETE FROM datapulse_db.sys_table_config WHERE datasource_id = @pg_ds;
INSERT INTO datapulse_db.sys_table_config (datasource_id, table_name, table_comment, is_whitelisted, custom_prompt_rules) VALUES
(@pg_ds, 'agent_trace_log', '智能体请求追踪与耗时审计表', 1, '记录每次 PulseTicket Agent 问答的 trace_id、user_id、username、user_message、intents_json、guard_blocked(1=安全拦截)、interrupted(1=触发人工确认)、write_executed(1=已执行写操作)、latency_ms(耗时毫秒)、status。'),
(@pg_ds, 'agent_conversation', '智能体多轮会话线程表', 1, '记录每个会话 thread_id、user_id、username、title、status(ACTIVE/INTERRUPTED)、message_count、created_at。'),
(@pg_ds, 'agent_message', '智能体对话消息明细表', 1, '通过 thread_id 关联 agent_conversation.thread_id；role 取值 user/assistant，mode 取值 agent/chat，latency_ms 为回复耗时。'),
(@pg_ds, 'ticket_kb_chunks', '官方票务知识库 RAG 向量分片表', 1, '存储官方规则文档分片：doc_code(如 POL-REFUND-001)、doc_title、source_file、content、embedding_model。统计分片数量或浏览知识库内容时请勿直接 SELECT embedding 向量列。'),
(@pg_ds, 'ticket_kb_meta', '知识库元数据配置表', 1, '记录知识库版本与同步元信息。'),
(@pg_ds, 'checkpoints', 'LangGraph 底层检查点表(二进制状态隔离)', 0, NULL),
(@pg_ds, 'checkpoint_blobs', 'LangGraph 底层二进制对象表(强制隔离)', 0, NULL),
(@pg_ds, 'checkpoint_writes', 'LangGraph 底层写日志表(强制隔离)', 0, NULL),
(@pg_ds, 'checkpoint_migrations', 'LangGraph 迁移版本表(强制隔离)', 0, NULL);
