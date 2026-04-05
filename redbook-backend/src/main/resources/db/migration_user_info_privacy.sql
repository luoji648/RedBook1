-- 已有库升级：收藏/赞过是否对他人公开（列已存在时跳过执行）
ALTER TABLE tb_user_info ADD COLUMN collect_public TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=公开收藏列表';
ALTER TABLE tb_user_info ADD COLUMN like_public TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=公开赞过列表';
