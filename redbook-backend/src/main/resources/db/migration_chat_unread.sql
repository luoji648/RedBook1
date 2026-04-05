-- 私信/群聊未读：已读游标（执行一次即可，与 schema 增量一致）

ALTER TABLE tb_chat_thread
    ADD COLUMN user_low_read_msg_id BIGINT DEFAULT NULL COMMENT 'user_low 侧已读至该消息 id',
    ADD COLUMN user_high_read_msg_id BIGINT DEFAULT NULL COMMENT 'user_high 侧已读至该消息 id';

ALTER TABLE tb_chat_group_member
    ADD COLUMN last_read_msg_id BIGINT DEFAULT NULL COMMENT '成员已读至该群消息 id';
