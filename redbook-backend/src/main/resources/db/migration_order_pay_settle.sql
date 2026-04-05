-- 订单支付时间、卖家结算标记（支付满 1 小时后将实付按卖家拆分入账）
ALTER TABLE tb_order
    ADD COLUMN pay_time DATETIME NULL COMMENT '支付成功时间' AFTER status,
    ADD COLUMN seller_settled TINYINT NOT NULL DEFAULT 0 COMMENT '1 实付已结算给卖家' AFTER pay_time;

-- 历史已支付订单：用 update_time 近似支付时间（便于 1 小时窗口与结算任务）
UPDATE tb_order
SET pay_time = update_time
WHERE status = 1 AND pay_time IS NULL;
