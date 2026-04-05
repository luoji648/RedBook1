-- 群头像：自定义 URL，为空时展示仍用群主头像
ALTER TABLE tb_chat_group
    ADD COLUMN avatar VARCHAR(512) DEFAULT NULL COMMENT '群头像 OSS URL，空则沿用群主头像' AFTER name;
