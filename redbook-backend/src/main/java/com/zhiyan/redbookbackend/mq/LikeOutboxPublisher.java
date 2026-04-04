package com.zhiyan.redbookbackend.mq;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhiyan.redbookbackend.config.RedbookProperties;
import com.zhiyan.redbookbackend.dto.mq.LikeAggMessage;
import com.zhiyan.redbookbackend.entity.LikeOutbox;
import com.zhiyan.redbookbackend.mapper.LikeOutboxMapper;
import com.zhiyan.redbookbackend.util.MapperSqlLogMute;
import com.zhiyan.redbookbackend.util.RabbitMqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeOutboxPublisher {

    private final LikeOutboxMapper likeOutboxMapper;
    private final RabbitTemplate rabbitTemplate;
    private final RedbookProperties redbookProperties;
    private final MapperSqlLogMute mapperSqlLogMute;

    @Scheduled(fixedDelay = 500)
    public void publishPending() {
        mapperSqlLogMute.runMuted(this::publishPendingUnmuted, MapperSqlLogMute.LIKE_OUTBOX_MAPPER);
    }

    private void publishPendingUnmuted() {
        List<LikeOutbox> list = likeOutboxMapper.selectList(
                Wrappers.<LikeOutbox>lambdaQuery()
                        .eq(LikeOutbox::getPublished, 0)
                        .orderByAsc(LikeOutbox::getId)
                        .last("LIMIT 200"));
        for (LikeOutbox ob : list) {
            int updated = likeOutboxMapper.update(null,
                    Wrappers.<LikeOutbox>lambdaUpdate()
                            .set(LikeOutbox::getPublished, 1)
                            .set(LikeOutbox::getPublishTime, LocalDateTime.now())
                            .eq(LikeOutbox::getId, ob.getId())
                            .eq(LikeOutbox::getPublished, 0));
            if (updated == 0) {
                continue;
            }
            long delta = ob.getEventType() == 1 ? 1L : -1L;
            int sh = redbookProperties.getLikeNoteShards();
            int shard = (int) Math.floorMod(ob.getNoteId(), (long) sh);
            LikeAggMessage agg = new LikeAggMessage(ob.getNoteId(), delta, ob.getId(), ob.getUserId());
            try {
                rabbitTemplate.convertAndSend(RabbitMqConstants.LIKE_NOTE_EXCHANGE, String.valueOf(shard), agg);
            } catch (Exception e) {
                log.error("outbox publish mq failed id={}", ob.getId(), e);
                likeOutboxMapper.update(null,
                        Wrappers.<LikeOutbox>lambdaUpdate()
                                .set(LikeOutbox::getPublished, 0)
                                .set(LikeOutbox::getPublishTime, null)
                                .eq(LikeOutbox::getId, ob.getId()));
            }
        }
    }
}

