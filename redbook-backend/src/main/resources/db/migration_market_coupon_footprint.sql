-- 集市：用户优惠券、商品足迹（已有库升级，可重复执行时用 IF NOT EXISTS）
CREATE TABLE IF NOT EXISTS tb_user_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL DEFAULT '',
    discount_cent BIGINT NOT NULL DEFAULT 0 COMMENT '满减金额（分）',
    min_order_cent BIGINT NOT NULL DEFAULT 0 COMMENT '订单满额门槛（分）',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0 未使用 1 已使用 2 已过期',
    expire_time DATETIME NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_uc_user_status_exp (user_id, status, expire_time),
    CONSTRAINT fk_uc_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tb_product_footprint (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    viewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fp_user_product (user_id, product_id),
    INDEX idx_fp_user_viewed (user_id, viewed_at DESC),
    CONSTRAINT fk_fp_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_fp_product FOREIGN KEY (product_id) REFERENCES tb_product(id) ON DELETE CASCADE
) ENGINE=InnoDB;
