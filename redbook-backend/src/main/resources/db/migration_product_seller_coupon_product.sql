-- 商品卖家、优惠券关联商品（可重复执行需注意：列已存在会报错，按需手动跳过）
-- MySQL 8

ALTER TABLE tb_product
    ADD COLUMN seller_id BIGINT NULL COMMENT '上架卖家用户 id' AFTER status,
    ADD INDEX idx_product_seller (seller_id),
    ADD CONSTRAINT fk_product_seller FOREIGN KEY (seller_id) REFERENCES tb_user(id) ON DELETE SET NULL;

ALTER TABLE tb_user_coupon
    ADD COLUMN product_id BIGINT NULL COMMENT '适用商品（关注卖家券等）' AFTER user_id,
    ADD INDEX idx_uc_product (product_id),
    ADD INDEX idx_uc_user_product (user_id, product_id),
    ADD CONSTRAINT fk_uc_product FOREIGN KEY (product_id) REFERENCES tb_product(id) ON DELETE SET NULL;
