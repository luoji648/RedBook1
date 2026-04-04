package com.zhiyan.redbookbackend.util;

public class RabbitMqConstants {
    public static final String LIKE_USER_EXCHANGE = "like.user.direct";
    public static final String LIKE_NOTE_EXCHANGE = "like.note.direct";
    public static final String LIKE_USER_QUEUE_PREFIX = "like.user.queue.";
    public static final String LIKE_NOTE_QUEUE_PREFIX = "like.note.queue.";

    public static final String SECKILL_DIRECT_EXCHANGE = "seckill.direct";
    public static final String SECKILL_ORDER_KEY = "seckill.order";
    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";

    public static final String ERROR_DIRECT_EXCHANGE = "error.direct";
    public static final String ERROR_QUEUE = "error.queue";
    public static final String ERROR_KEY = "error";
}
