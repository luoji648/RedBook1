-- 仅创建钱包与充值流水表（不修改 tb_order）。在已有库 redbook 上执行即可消除「tb_wallet_recharge 不存在」错误。
-- 用法示例（按本机账号密码调整）：
--   mysql -h127.0.0.1 -uroot -p redbook < src/main/resources/db/migration_wallet_tables_only.sql

USE redbook;

CREATE TABLE IF NOT EXISTS tb_user_wallet (
    user_id BIGINT PRIMARY KEY,
    balance_cent BIGINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tb_wallet_recharge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    out_trade_no VARCHAR(64) NOT NULL,
    amount_cent BIGINT NOT NULL,
    alipay_trade_no VARCHAR(64) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0 待支付 1 成功 2 关闭',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pay_time DATETIME DEFAULT NULL,
    UNIQUE KEY uk_wr_out_trade (out_trade_no),
    INDEX idx_wr_user (user_id),
    CONSTRAINT fk_wr_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE
) ENGINE=InnoDB;
