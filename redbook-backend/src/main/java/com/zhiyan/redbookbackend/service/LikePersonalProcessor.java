package com.zhiyan.redbookbackend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhiyan.redbookbackend.dto.mq.LikePersonalMessage;
import com.zhiyan.redbookbackend.entity.LikeOutbox;
import com.zhiyan.redbookbackend.entity.NoteLike;
import com.zhiyan.redbookbackend.mapper.LikeOutboxMapper;
import com.zhiyan.redbookbackend.mapper.NoteLikeMapper;
import com.zhiyan.redbookbackend.util.MapperSqlLogMute;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LikePersonalProcessor {

    private final NoteLikeMapper noteLikeMapper;
    private final LikeOutboxMapper likeOutboxMapper;
    private final MapperSqlLogMute mapperSqlLogMute;

    @Transactional(rollbackFor = Exception.class)
    public void process(LikePersonalMessage msg) {
        mapperSqlLogMute.runMuted(() -> doProcess(msg),
                MapperSqlLogMute.NOTE_LIKE_MAPPER,
                MapperSqlLogMute.LIKE_OUTBOX_MAPPER);
    }

    private void doProcess(LikePersonalMessage msg) {
        if (msg.getAction() == 1) {
            Long cnt = noteLikeMapper.selectCount(Wrappers.<NoteLike>lambdaQuery()
                    .eq(NoteLike::getUserId, msg.getUserId())
                    .eq(NoteLike::getNoteId, msg.getNoteId()));
            if (cnt != null && cnt > 0) {
                return;
            }
            NoteLike row = new NoteLike();
            row.setUserId(msg.getUserId());
            row.setNoteId(msg.getNoteId());
            row.setCreateTime(LocalDateTime.now());
            noteLikeMapper.insert(row);
            LikeOutbox ob = new LikeOutbox();
            ob.setUserId(msg.getUserId());
            ob.setNoteId(msg.getNoteId());
            ob.setEventType(1);
            ob.setPublished(0);
            ob.setCreatedTime(LocalDateTime.now());
            likeOutboxMapper.insert(ob);
        } else if (msg.getAction() == -1) {
            int deleted = noteLikeMapper.delete(Wrappers.<NoteLike>lambdaQuery()
                    .eq(NoteLike::getUserId, msg.getUserId())
                    .eq(NoteLike::getNoteId, msg.getNoteId()));
            if (deleted == 0) {
                return;
            }
            LikeOutbox ob = new LikeOutbox();
            ob.setUserId(msg.getUserId());
            ob.setNoteId(msg.getNoteId());
            ob.setEventType(2);
            ob.setPublished(0);
            ob.setCreatedTime(LocalDateTime.now());
            likeOutboxMapper.insert(ob);
        }
    }
}

