-- 已有库升级到 JWT / 踢线方案时执行（若已包含 token_version 可跳过）
ALTER TABLE tb_user ADD COLUMN token_version INT NOT NULL DEFAULT 0;
