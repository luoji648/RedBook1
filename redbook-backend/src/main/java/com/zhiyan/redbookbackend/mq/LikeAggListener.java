package com.zhiyan.redbookbackend.mq;

import com.zhiyan.redbookbackend.dto.mq.LikeAggMessage;
import com.zhiyan.redbookbackend.entity.Note;
import com.zhiyan.redbookbackend.entity.NoteLikeAgg;
import com.zhiyan.redbookbackend.mapper.NoteLikeAggMapper;
import com.zhiyan.redbookbackend.mapper.NoteMapper;
import com.zhiyan.redbookbackend.util.RedisConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeAggListener {

    private static final String LUA_TRIM = """
            local key = KEYS[1]
            local cap = tonumber(ARGV[1])
            local n = redis.call('SCARD', key)
            if n > cap then
              local members = redis.call('SMEMBERS', key)
              local half = math.floor(n / 2)
              for i = 1, half do
                redis.call('SREM', key, members[i])
              end
            end
            return redis.call('SCARD', key)
            """;

    private final NoteLikeAggMapper noteLikeAggMapper;
    private final NoteMapper noteMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final PlatformTransactionManager transactionManager;

    @RabbitListener(queues = "#{@likeNoteQueueNames}")
    public void onAgg(LikeAggMessage msg) {
        String doneKey = "like:agg:done:" + msg.getOutboxId();
        Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(doneKey, "1", 1, java.util.concurrent.TimeUnit.DAYS);
        if (Boolean.FALSE.equals(first)) {
            return;
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        try {
            tx.executeWithoutResult(status -> {
                NoteLikeAgg agg = noteLikeAggMapper.selectById(msg.getNoteId());
                long newCount;
                if (agg == null) {
                    newCount = Math.max(0, msg.getDelta());
                    agg = new NoteLikeAgg();
                    agg.setNoteId(msg.getNoteId());
                    agg.setLikeCount(newCount);
                    agg.setUpdateTime(LocalDateTime.now());
                    noteLikeAggMapper.insert(agg);
                } else {
                    newCount = Math.max(0, agg.getLikeCount() + msg.getDelta());
                    agg.setLikeCount(newCount);
                    agg.setUpdateTime(LocalDateTime.now());
                    noteLikeAggMapper.updateById(agg);
                }
                Note note = noteMapper.selectById(msg.getNoteId());
                if (note != null) {
                    note.setLikeCount((int) Math.min(Integer.MAX_VALUE, newCount));
                    note.setUpdateTime(LocalDateTime.now());
                    noteMapper.updateById(note);
                }
            });
        } catch (Exception e) {
            stringRedisTemplate.delete(doneKey);
            log.error("agg failed {}", msg, e);
            throw e;
        }

        NoteLikeAgg latest = noteLikeAggMapper.selectById(msg.getNoteId());
        long c = latest != null ? latest.getLikeCount() : 0;
        stringRedisTemplate.opsForValue().set(RedisConstants.NOTE_LIKE_COUNT_KEY + msg.getNoteId(), String.valueOf(c));

        if (msg.getUserId() != null) {
            String setKey = RedisConstants.NOTE_LIKE_USERS_SET_KEY + msg.getNoteId();
            if (msg.getDelta() > 0) {
                stringRedisTemplate.opsForSet().add(setKey, String.valueOf(msg.getUserId()));
            } else {
                stringRedisTemplate.opsForSet().remove(setKey, String.valueOf(msg.getUserId()));
            }
            try {
                DefaultRedisScript<Long> script = new DefaultRedisScript<>();
                script.setScriptText(LUA_TRIM);
                script.setResultType(Long.class);
                stringRedisTemplate.execute(script, Collections.singletonList(setKey), "2000");
            } catch (Exception ignored) {
            }
        }
    }
}
