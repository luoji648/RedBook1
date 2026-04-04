package com.zhiyan.redbookbackend.util;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    /** session hash keyed by JWT jti */
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 36000L;

    public static final String JWT_BLACKLIST_KEY = "jwt:blacklist:";
    public static final String SEND_CODE_LIMIT_KEY = "SEND_CODE:";
    public static final String SMS_DAY_COUNT_KEY = "SMS_DAY:";

    /** 用户 token 版本，改密后递增使旧 JWT 失效 */
    public static final String USER_TOKEN_VERSION_KEY = "user:tv:";

    public static final Long CACHE_NULL_TTL = 2L;

    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";

    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;

    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    public static final String FEED_KEY = "feed:";

    public static final String NOTE_LIKE_COUNT_KEY = "note:like:count:";
    /** 某笔记点赞用户集合（可裁剪防 big key） */
    public static final String NOTE_LIKE_USERS_SET_KEY = "note:like:users:";
    public static final String SHOP_GEO_KEY = "shop:geo:";
    public static final String USER_SIGN_KEY = "sign:";
}
