package com.zhiyan.redbookbackend.mq;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zhiyan.redbookbackend.dto.mq.LikePersonalMessage;
import com.zhiyan.redbookbackend.service.LikePersonalProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikePersonalListener {

    private static final String DEDUPE_PREFIX = "like:dedupe:personal:";

    private final LikePersonalProcessor likePersonalProcessor;
    private final StringRedisTemplate stringRedisTemplate;

    private final Cache<String, Boolean> localDedupe = Caffeine.newBuilder()
            .maximumSize(200_000)
            .expireAfterWrite(45, TimeUnit.SECONDS)
            .build();

    @RabbitListener(queues = "#{@likeUserQueueNames}")
    public void onMessage(LikePersonalMessage msg) {
        String key = msg.getUserId() + ":" + msg.getNoteId() + ":" + msg.getAction();
        if (localDedupe.getIfPresent(key) != null) {
            return;
        }
        String redisKey = DEDUPE_PREFIX + key;
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, "1", 2, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(ok)) {
            return;
        }
        localDedupe.put(key, Boolean.TRUE);
        try {
            likePersonalProcessor.process(msg);
        } catch (Exception e) {
            log.error("like personal process failed {}", msg, e);
            stringRedisTemplate.delete(redisKey);
            localDedupe.invalidate(key);
            throw e;
        }
    }
}
