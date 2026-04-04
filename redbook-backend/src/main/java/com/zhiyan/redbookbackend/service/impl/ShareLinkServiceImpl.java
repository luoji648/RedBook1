package com.zhiyan.redbookbackend.service.impl;

import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.entity.Note;
import com.zhiyan.redbookbackend.entity.ShareLink;
import com.zhiyan.redbookbackend.mapper.NoteMapper;
import com.zhiyan.redbookbackend.mapper.ShareLinkMapper;
import com.zhiyan.redbookbackend.service.IShareLinkService;
import com.zhiyan.redbookbackend.util.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class ShareLinkServiceImpl implements IShareLinkService {

    private static final char[] BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom RND = new SecureRandom();

    @Resource
    private ShareLinkMapper shareLinkMapper;
    @Resource
    private NoteMapper noteMapper;

    @Override
    public Result createForNote(Long noteId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            return Result.fail("笔记不存在");
        }
        Long me = UserHolder.getUser().getId();
        if (!note.getUserId().equals(me)) {
            return Result.fail("只能分享自己的笔记");
        }
        String code;
        for (int i = 0; i < 8; i++) {
            code = randomCode(8);
            ShareLink existing = shareLinkMapper.selectById(code);
            if (existing == null) {
                ShareLink sl = new ShareLink();
                sl.setShortCode(code);
                sl.setNoteId(noteId);
                sl.setCreatorUserId(me);
                sl.setExpireTime(null);
                sl.setCreateTime(LocalDateTime.now());
                shareLinkMapper.insert(sl);
                Map<String, String> m = new HashMap<>();
                m.put("shortCode", code);
                m.put("path", "/s/" + code);
                return Result.ok(m);
            }
        }
        return Result.fail("请重试");
    }

    private String randomCode(int len) {
        char[] buf = new char[len];
        for (int i = 0; i < len; i++) {
            buf[i] = BASE62[RND.nextInt(BASE62.length)];
        }
        return new String(buf);
    }
}
