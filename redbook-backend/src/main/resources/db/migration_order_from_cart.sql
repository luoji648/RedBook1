-- 区分订单是否由购物车结算创建：支付成功后才从购物车扣减对应商品
ALTER TABLE tb_order
ADD COLUMN from_cart TINYINT NOT NULL DEFAULT 0 COMMENT '1 购物车结算 0 立即购买等' AFTER seller_settled;
