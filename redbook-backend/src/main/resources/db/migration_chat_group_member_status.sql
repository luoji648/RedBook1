-- 群成员状态：0 在群 1 已移出（保留会话，不可收发新消息）
ALTER TABLE tb_chat_group_member
    ADD COLUMN member_status TINYINT NOT NULL DEFAULT 0 COMMENT '0 在群 1 已移出' AFTER role;
