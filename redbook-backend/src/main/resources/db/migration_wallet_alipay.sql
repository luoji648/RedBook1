-- 钱包、支付宝充值、订单支付字段（在已有库上执行）
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

ALTER TABLE tb_order
    ADD COLUMN pay_cent BIGINT NULL COMMENT '实付（分，从钱包扣）' AFTER total_cent,
    ADD COLUMN discount_cent BIGINT NOT NULL DEFAULT 0 COMMENT '优惠券抵扣（分）' AFTER pay_cent,
    ADD COLUMN user_coupon_id BIGINT NULL COMMENT '使用的用户券 id' AFTER discount_cent;

-- 原 status：0 已创建 1 已支付 2 已取消；新增 3 已退款
-- 若列上已有 COMMENT，可按需手工调整；此处仅追加字段，不修改 status 语义
