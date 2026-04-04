package com.zhiyan.redbookbackend.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.ScrollResult;
import com.zhiyan.redbookbackend.dto.req.NoteSaveDTO;
import com.zhiyan.redbookbackend.entity.Note;
import com.zhiyan.redbookbackend.entity.NoteMedia;
import com.zhiyan.redbookbackend.entity.NoteProduct;
import com.zhiyan.redbookbackend.entity.User;
import com.zhiyan.redbookbackend.entity.UserFollow;
import com.zhiyan.redbookbackend.entity.NoteLike;
import com.zhiyan.redbookbackend.mapper.NoteLikeMapper;
import com.zhiyan.redbookbackend.mapper.NoteMapper;
import com.zhiyan.redbookbackend.mapper.NoteMediaMapper;
import com.zhiyan.redbookbackend.mapper.NoteProductMapper;
import com.zhiyan.redbookbackend.mapper.UserFollowMapper;
import com.zhiyan.redbookbackend.mapper.UserMapper;
import com.zhiyan.redbookbackend.service.INoteService;
import com.zhiyan.redbookbackend.util.RedisConstants;
import com.zhiyan.redbookbackend.util.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements INoteService {

    @Resource
    private NoteMediaMapper noteMediaMapper;
    @Resource
    private NoteProductMapper noteProductMapper;
    @Resource
    private UserFollowMapper userFollowMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private NoteLikeMapper noteLikeMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result save(NoteSaveDTO dto) {
        Long uid = UserHolder.getUser().getId();
        Note note;
        if (dto.getId() != null) {
            note = getById(dto.getId());
            if (note == null || !note.getUserId().equals(uid)) {
                return Result.fail("笔记不存在");
            }
        } else {
            note = new Note();
            note.setUserId(uid);
            note.setStatus(0);
            note.setLikeCount(0);
            note.setCollectCount(0);
            note.setCommentCount(0);
            note.setCreateTime(LocalDateTime.now());
        }
        if (dto.getTitle() != null) {
            note.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            note.setContent(dto.getContent());
        }
        if (dto.getType() != null) {
            note.setType(dto.getType());
        }
        if (dto.getVisibility() != null) {
            note.setVisibility(dto.getVisibility());
        }
        note.setUpdateTime(LocalDateTime.now());
        if (Boolean.TRUE.equals(dto.getPublish())) {
            note.setStatus(1);
        }
        if (dto.getId() == null) {
            save(note);
        } else {
            updateById(note);
            noteMediaMapper.delete(Wrappers.<NoteMedia>lambdaQuery().eq(NoteMedia::getNoteId, note.getId()));
            noteProductMapper.delete(Wrappers.<NoteProduct>lambdaQuery().eq(NoteProduct::getNoteId, note.getId()));
        }
        saveMediaAndProducts(note.getId(), dto);
        if (Boolean.TRUE.equals(dto.getPublish()) && note.getStatus() == 1) {
            pushToFollowersFeed(note);
        }
        return Result.ok(note.getId());
    }

    private void saveMediaAndProducts(Long noteId, NoteSaveDTO dto) {
        if (dto.getMediaUrls() != null) {
            int i = 0;
            for (String url : dto.getMediaUrls()) {
                NoteMedia m = new NoteMedia();
                m.setNoteId(noteId);
                m.setUrl(url);
                m.setSortOrder(i++);
                m.setMediaType(dto.getType() != null && dto.getType() == 1 ? 1 : 0);
                noteMediaMapper.insert(m);
            }
        }
        if (dto.getProductIds() != null) {
            int j = 0;
            for (Long pid : dto.getProductIds()) {
                NoteProduct np = new NoteProduct();
                np.setNoteId(noteId);
                np.setProductId(pid);
                np.setSortOrder(j++);
                noteProductMapper.insert(np);
            }
        }
    }

    private void pushToFollowersFeed(Note note) {
        if (note.getVisibility() != null && note.getVisibility() == 2) {
            return;
        }
        List<UserFollow> fans = userFollowMapper.selectList(
                Wrappers.<UserFollow>lambdaQuery().eq(UserFollow::getFolloweeId, note.getUserId()));
        double score = System.currentTimeMillis();
        String nid = String.valueOf(note.getId());
        for (UserFollow f : fans) {
            stringRedisTemplate.opsForZSet().add(RedisConstants.FEED_KEY + f.getFollowerId(), nid, score);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result publish(Long noteId) {
        Note note = getById(noteId);
        if (note == null || !note.getUserId().equals(UserHolder.getUser().getId())) {
            return Result.fail("笔记不存在");
        }
        note.setStatus(1);
        note.setUpdateTime(LocalDateTime.now());
        updateById(note);
        pushToFollowersFeed(note);
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result delete(Long noteId) {
        Note note = getById(noteId);
        if (note == null || !note.getUserId().equals(UserHolder.getUser().getId())) {
            return Result.fail("笔记不存在");
        }
        removeById(noteId);
        return Result.ok();
    }

    @Override
    public Result detail(Long noteId) {
        Note note = getById(noteId);
        if (note == null) {
            return Result.fail("笔记不存在");
        }
        Long viewer = UserHolder.getUser() != null ? UserHolder.getUser().getId() : null;
        if (!canView(note, viewer)) {
            return Result.fail("无权查看");
        }
        Map<String, Object> vo = buildNoteVo(note, viewer);
        return Result.ok(vo);
    }

    private boolean canView(Note note, Long viewerId) {
        if (note.getVisibility() == null || note.getVisibility() == 0) {
            return true;
        }
        if (viewerId == null) {
            return false;
        }
        if (note.getUserId().equals(viewerId)) {
            return true;
        }
        if (note.getVisibility() == 2) {
            return false;
        }
        Long c = userFollowMapper.selectCount(Wrappers.<UserFollow>lambdaQuery()
                .eq(UserFollow::getFollowerId, viewerId)
                .eq(UserFollow::getFolloweeId, note.getUserId()));
        return c != null && c > 0;
    }

    private Map<String, Object> buildNoteVo(Note note, Long viewerId) {
        Map<String, Object> vo = new HashMap<>();
        vo.put("note", note);
        User author = userMapper.selectById(note.getUserId());
        vo.put("author", author);
        List<NoteMedia> media = noteMediaMapper.selectList(
                Wrappers.<NoteMedia>lambdaQuery().eq(NoteMedia::getNoteId, note.getId()).orderByAsc(NoteMedia::getSortOrder));
        vo.put("media", media);
        List<NoteProduct> nps = noteProductMapper.selectList(
                Wrappers.<NoteProduct>lambdaQuery().eq(NoteProduct::getNoteId, note.getId()).orderByAsc(NoteProduct::getSortOrder));
        vo.put("noteProducts", nps);
        String cnt = stringRedisTemplate.opsForValue().get(RedisConstants.NOTE_LIKE_COUNT_KEY + note.getId());
        if (cnt != null) {
            vo.put("likeCount", Long.parseLong(cnt));
        } else {
            vo.put("likeCount", note.getLikeCount() == null ? 0 : note.getLikeCount());
        }
        if (viewerId != null) {
            boolean liked = Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(
                    RedisConstants.NOTE_LIKE_USERS_SET_KEY + note.getId(), String.valueOf(viewerId)));
            if (!liked) {
                Long lc = noteLikeMapper.selectCount(Wrappers.<NoteLike>lambdaQuery()
                        .eq(NoteLike::getNoteId, note.getId())
                        .eq(NoteLike::getUserId, viewerId));
                liked = lc != null && lc > 0;
            }
            vo.put("liked", liked);
        } else {
            vo.put("liked", false);
        }
        return vo;
    }

    @Override
    public Result myNotes(long current, long size) {
        Long uid = UserHolder.getUser().getId();
        Page<Note> page = Page.of(current, size);
        IPage<Note> p = page(page, Wrappers.<Note>lambdaQuery()
                .eq(Note::getUserId, uid)
                .orderByDesc(Note::getCreateTime));
        return Result.ok(p.getRecords(), p.getTotal());
    }

    @Override
    public Result feed(Long maxTime, Integer offset, int size) {
        Long uid = UserHolder.getUser().getId();
        String key = RedisConstants.FEED_KEY + uid;
        double maxScore = maxTime == null ? Double.POSITIVE_INFINITY : maxTime.doubleValue();
        long off = offset == null ? 0 : offset.longValue();
        Set<String> ids = stringRedisTemplate.opsForZSet().reverseRangeByScore(key, 0, maxScore, off, size);
        if (ids == null || ids.isEmpty()) {
            ScrollResult sr = new ScrollResult();
            sr.setList(List.of());
            sr.setMinTime(0L);
            sr.setOffset(0);
            return Result.ok(sr);
        }
        List<String> idStrs = new ArrayList<>(ids);
        List<Long> idList = idStrs.stream().map(Long::parseLong).collect(Collectors.toList());
        List<Note> notes = listByIds(idList);
        Map<Long, Note> map = notes.stream().collect(Collectors.toMap(Note::getId, n -> n));
        List<Map<String, Object>> list = new ArrayList<>();
        double lastScore = maxScore;
        for (String sid : idStrs) {
            Note n = map.get(Long.parseLong(sid));
            if (n != null) {
                list.add(buildNoteVo(n, uid));
            }
            Double sc = stringRedisTemplate.opsForZSet().score(key, sid);
            if (sc != null) {
                lastScore = sc;
            }
        }
        ScrollResult sr = new ScrollResult();
        sr.setList(list);
        sr.setMinTime((long) lastScore);
        sr.setOffset(idStrs.size());
        return Result.ok(sr);
    }

    @Override
    public Result recommend(long current, long size) {
        Page<Note> page = Page.of(current, size);
        IPage<Note> p = page(page, Wrappers.<Note>lambdaQuery()
                .eq(Note::getStatus, 1)
                .eq(Note::getVisibility, 0)
                .orderByDesc(Note::getCreateTime));
        Long viewer = UserHolder.getUser() != null ? UserHolder.getUser().getId() : null;
        List<Map<String, Object>> vos = p.getRecords().stream()
                .map(n -> buildNoteVo(n, viewer))
                .collect(Collectors.toList());
        return Result.ok(vos, p.getTotal());
    }

    @Override
    public Result related(Long noteId) {
        Note note = getById(noteId);
        if (note == null) {
            return Result.fail("笔记不存在");
        }
        List<Note> list = list(Wrappers.<Note>lambdaQuery()
                .eq(Note::getUserId, note.getUserId())
                .eq(Note::getStatus, 1)
                .ne(Note::getId, noteId)
                .orderByDesc(Note::getCreateTime)
                .last("LIMIT 8"));
        Long viewer = UserHolder.getUser() != null ? UserHolder.getUser().getId() : null;
        return Result.ok(list.stream().map(n -> buildNoteVo(n, viewer)).collect(Collectors.toList()));
    }
}
