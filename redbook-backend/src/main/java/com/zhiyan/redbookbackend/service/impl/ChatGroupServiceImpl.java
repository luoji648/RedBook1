package com.zhiyan.redbookbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyan.redbookbackend.constant.GroupChatConstants;
import com.zhiyan.redbookbackend.dto.Result;
import com.zhiyan.redbookbackend.dto.UserDTO;
import com.zhiyan.redbookbackend.dto.req.GroupCreateDTO;
import com.zhiyan.redbookbackend.dto.req.GroupUpdateDTO;
import com.zhiyan.redbookbackend.entity.ChatGroup;
import com.zhiyan.redbookbackend.entity.ChatGroupJoinRequest;
import com.zhiyan.redbookbackend.entity.ChatGroupMember;
import com.zhiyan.redbookbackend.entity.GroupMessage;
import com.zhiyan.redbookbackend.entity.User;
import com.zhiyan.redbookbackend.mapper.ChatGroupJoinRequestMapper;
import com.zhiyan.redbookbackend.mapper.ChatGroupMapper;
import com.zhiyan.redbookbackend.mapper.ChatGroupMemberMapper;
import com.zhiyan.redbookbackend.mapper.GroupMessageMapper;
import com.zhiyan.redbookbackend.mapper.UserMapper;
import com.zhiyan.redbookbackend.service.IChatGroupService;
import com.zhiyan.redbookbackend.service.IChatService;
import com.zhiyan.redbookbackend.util.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
@Service
public class ChatGroupServiceImpl implements IChatGroupService {

    public static final int ROLE_MEMBER = 0;
    public static final int ROLE_OWNER = 1;
    /** 在群，可收发消息 */
    public static final int MEMBER_IN_GROUP = 0;
    /** 已移出，仅保留会话与历史 */
    public static final int MEMBER_REMOVED = 1;
    public static final int JOIN_OPEN = 0;
    public static final int JOIN_NEED_APPROVAL = 1;
    public static final int REQ_PENDING = 0;
    public static final int REQ_APPROVED = 1;
    public static final int REQ_REJECTED = 2;

    @Resource
    private ChatGroupMapper chatGroupMapper;
    @Resource
    private ChatGroupMemberMapper chatGroupMemberMapper;
    @Resource
    private ChatGroupJoinRequestMapper chatGroupJoinRequestMapper;
    @Resource
    private GroupMessageMapper groupMessageMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private IChatService chatService;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result create(GroupCreateDTO dto) {
        Long me = UserHolder.getUser().getId();
        if (dto == null || dto.getName() == null || dto.getName().isBlank()) {
            return Result.fail("群名称不能为空");
        }
        String name = dto.getName().trim();
        if (name.length() > 128) {
            return Result.fail("群名称过长");
        }
        Integer jm = dto.getJoinMode();
        if (jm == null || (jm != JOIN_OPEN && jm != JOIN_NEED_APPROVAL)) {
            return Result.fail("加群方式无效");
        }
        String avatarUrl = emptyToNull(dto.getAvatar() != null ? dto.getAvatar().trim() : null);
        if (avatarUrl != null && avatarUrl.length() > 512) {
            return Result.fail("头像地址过长");
        }
        ChatGroup g = new ChatGroup();
        g.setOwnerId(me);
        g.setName(name);
        g.setAvatar(avatarUrl);
        g.setJoinMode(jm);
        g.setCreateTime(LocalDateTime.now());
        g.setLastMsgTime(LocalDateTime.now());
        chatGroupMapper.insert(g);
        addMember(g.getId(), me, ROLE_OWNER);
        return Result.ok(g.getId());
    }

    @Override
    public Result listByOwner(Long ownerUserId) {
        if (ownerUserId == null) {
            return Result.fail("用户不存在");
        }
        List<ChatGroup> list = chatGroupMapper.selectList(
                Wrappers.<ChatGroup>lambdaQuery()
                        .eq(ChatGroup::getOwnerId, ownerUserId)
                        .orderByDesc(ChatGroup::getCreateTime));
        Long viewerId = Optional.ofNullable(UserHolder.getUser()).map(UserDTO::getId).orElse(null);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ChatGroup g : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", g.getId());
            row.put("name", g.getName());
            row.put("joinMode", g.getJoinMode());
            row.put("createTime", g.getCreateTime());
            row.put("inGroup", viewerId != null && isActiveMember(g.getId(), viewerId));
            rows.add(row);
        }
        return Result.ok(rows);
    }

    @Override
    public Result myGroups() {
        Long me = UserHolder.getUser().getId();
        List<ChatGroupMember> ms = chatGroupMemberMapper.selectList(
                Wrappers.<ChatGroupMember>lambdaQuery().eq(ChatGroupMember::getUserId, me));
        if (ms.isEmpty()) {
            return Result.ok(List.of(), 0L);
        }
        Map<Long, Long> groupIdToLastRead = new LinkedHashMap<>();
        for (ChatGroupMember m : ms) {
            groupIdToLastRead.put(m.getGroupId(), m.getLastReadMsgId());
        }
        List<Long> ids = ms.stream().map(ChatGroupMember::getGroupId).distinct().toList();
        List<ChatGroup> groups = chatGroupMapper.selectList(
                Wrappers.<ChatGroup>lambdaQuery().in(ChatGroup::getId, ids));
        groups.sort(Comparator.comparing(ChatGroup::getLastMsgTime, Comparator.nullsLast(Comparator.reverseOrder())));
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<Long, ChatGroupMember> memByGid = ms.stream()
                .collect(Collectors.toMap(ChatGroupMember::getGroupId, m -> m, (a, b) -> a));
        for (ChatGroup g : groups) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", g.getId());
            row.put("name", g.getName());
            row.put("ownerId", g.getOwnerId());
            row.put("joinMode", g.getJoinMode());
            row.put("createTime", g.getCreateTime());
            row.put("lastMsgTime", g.getLastMsgTime());
            row.put("avatar", resolveGroupAvatar(g));
            ChatGroupMember myMem = memByGid.get(g.getId());
            boolean active = isActiveMemberRow(myMem);
            row.put("removedFromGroup", myMem != null && !active);
            row.put("unreadCount", active
                    ? countUnreadInGroup(g.getId(), me, groupIdToLastRead.get(g.getId()))
                    : 0L);
            rows.add(row);
        }
        return Result.ok(rows, (long) rows.size());
    }

    private long countUnreadInGroup(Long groupId, Long me, Long lastReadMsgId) {
        var q = Wrappers.<GroupMessage>lambdaQuery()
                .eq(GroupMessage::getGroupId, groupId)
                .ne(GroupMessage::getSenderId, me);
        if (lastReadMsgId != null) {
            q.gt(GroupMessage::getId, lastReadMsgId);
        }
        return groupMessageMapper.selectCount(q);
    }

    @Override
    public long sumGroupUnreadForCurrentUser() {
        Long me = UserHolder.getUser().getId();
        List<ChatGroupMember> ms = chatGroupMemberMapper.selectList(
                Wrappers.<ChatGroupMember>lambdaQuery().eq(ChatGroupMember::getUserId, me));
        long sum = 0;
        for (ChatGroupMember m : ms) {
            if (!isActiveMemberRow(m)) {
                continue;
            }
            sum += countUnreadInGroup(m.getGroupId(), me, m.getLastReadMsgId());
        }
        return sum;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result markGroupRead(Long groupId) {
        Long me = UserHolder.getUser().getId();
        if (!isActiveMember(groupId, me)) {
            return Result.fail("无权操作该群");
        }
        ChatGroupMember mem = chatGroupMemberMapper.selectOne(
                Wrappers.<ChatGroupMember>lambdaQuery()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, me));
        if (mem == null) {
            return Result.fail("成员记录不存在");
        }
        GroupMessage last = groupMessageMapper.selectOne(
                Wrappers.<GroupMessage>lambdaQuery()
                        .eq(GroupMessage::getGroupId, groupId)
                        .orderByDesc(GroupMessage::getId)
                        .last("LIMIT 1"));
        if (last == null) {
            return Result.ok();
        }
        mem.setLastReadMsgId(last.getId());
        chatGroupMemberMapper.updateById(mem);
        return Result.ok();
    }

    private Long maxGroupMessageId(Long groupId) {
        GroupMessage one = groupMessageMapper.selectOne(
                Wrappers.<GroupMessage>lambdaQuery()
                        .eq(GroupMessage::getGroupId, groupId)
                        .orderByDesc(GroupMessage::getId)
                        .last("LIMIT 1"));
        return one != null ? one.getId() : null;
    }

    private void advanceMemberReadAfterSend(Long groupId, Long userId, Long msgId) {
        ChatGroupMember mem = chatGroupMemberMapper.selectOne(
                Wrappers.<ChatGroupMember>lambdaQuery()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, userId));
        if (mem == null) {
            return;
        }
        Long cur = mem.getLastReadMsgId();
        if (cur == null || msgId > cur) {
            mem.setLastReadMsgId(msgId);
            chatGroupMemberMapper.updateById(mem);
        }
    }

    private ChatGroupMember getMembership(Long groupId, Long userId) {
        return chatGroupMemberMapper.selectOne(
                Wrappers.<ChatGroupMember>lambdaQuery()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, userId));
    }

    private boolean isActiveMemberRow(ChatGroupMember m) {
        if (m == null) {
            return false;
        }
        Integer s = m.getMemberStatus();
        return s == null || s == MEMBER_IN_GROUP;
    }

    private boolean isActiveMember(Long groupId, Long userId) {
        return isActiveMemberRow(getMembership(groupId, userId));
    }

    /** 含已移出成员：可进会话看历史 */
    private boolean hasGroupThreadAccess(Long groupId, Long userId) {
        return getMembership(groupId, userId) != null;
    }

    private void addMember(Long groupId, Long userId, int role) {
        ChatGroupMember m = new ChatGroupMember();
        m.setGroupId(groupId);
        m.setUserId(userId);
        m.setRole(role);
        m.setMemberStatus(MEMBER_IN_GROUP);
        m.setCreateTime(LocalDateTime.now());
        m.setLastReadMsgId(maxGroupMessageId(groupId));
        chatGroupMemberMapper.insert(m);
    }

    private String buildJoinDmPayload(long requestId, long groupId, String groupName, long applicantId) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("requestId", requestId);
            map.put("groupId", groupId);
            map.put("groupName", groupName);
            map.put("applicantId", applicantId);
            return GroupChatConstants.JOIN_MSG_PREFIX + objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result join(Long groupId) {
        Long me = UserHolder.getUser().getId();
        ChatGroup g = chatGroupMapper.selectById(groupId);
        if (g == null) {
            return Result.fail("群不存在");
        }
        if (isActiveMember(groupId, me)) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("status", "already");
            return Result.ok(m);
        }
        if (g.getJoinMode() == null || g.getJoinMode() == JOIN_OPEN) {
            ChatGroupMember existed = getMembership(groupId, me);
            if (existed != null && !isActiveMemberRow(existed)) {
                existed.setMemberStatus(MEMBER_IN_GROUP);
                existed.setRole(ROLE_MEMBER);
                existed.setLastReadMsgId(maxGroupMessageId(groupId));
                chatGroupMemberMapper.updateById(existed);
                Map<String, String> m = new LinkedHashMap<>();
                m.put("status", "joined");
                return Result.ok(m);
            }
            addMember(groupId, me, ROLE_MEMBER);
            Map<String, String> m = new LinkedHashMap<>();
            m.put("status", "joined");
            return Result.ok(m);
        }
        ChatGroupJoinRequest existing = chatGroupJoinRequestMapper.selectOne(
                Wrappers.<ChatGroupJoinRequest>lambdaQuery()
                        .eq(ChatGroupJoinRequest::getGroupId, groupId)
                        .eq(ChatGroupJoinRequest::getApplicantId, me));
        long requestId;
        if (existing == null) {
            ChatGroupJoinRequest req = new ChatGroupJoinRequest();
            req.setGroupId(groupId);
            req.setApplicantId(me);
            req.setStatus(REQ_PENDING);
            req.setCreateTime(LocalDateTime.now());
            req.setUpdateTime(LocalDateTime.now());
            chatGroupJoinRequestMapper.insert(req);
            requestId = req.getId();
        } else {
            if (existing.getStatus() == REQ_PENDING) {
                return Result.fail("已有待处理的入群申请，请等待群主处理");
            }
            if (existing.getStatus() == REQ_APPROVED) {
                ChatGroupMember row = getMembership(groupId, me);
                if (row != null && isActiveMemberRow(row)) {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("status", "already");
                    return Result.ok(m);
                }
                // 曾被移出或无有效在群记录：历史「已通过」不自动生效，需群主再次同意
            }
            existing.setStatus(REQ_PENDING);
            existing.setUpdateTime(LocalDateTime.now());
            chatGroupJoinRequestMapper.updateById(existing);
            requestId = existing.getId();
        }
        String dm = buildJoinDmPayload(requestId, groupId, g.getName(), me);
        chatService.send(g.getOwnerId(), dm);
        Map<String, String> m = new LinkedHashMap<>();
        m.put("status", "pending");
        return Result.ok(m);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result handleJoinRequest(Long requestId, boolean approve) {
        Long me = UserHolder.getUser().getId();
        ChatGroupJoinRequest req = chatGroupJoinRequestMapper.selectById(requestId);
        if (req == null || req.getStatus() != REQ_PENDING) {
            return Result.fail("申请不存在或已处理");
        }
        ChatGroup g = chatGroupMapper.selectById(req.getGroupId());
        if (g == null || !g.getOwnerId().equals(me)) {
            return Result.fail("仅群主可处理入群申请");
        }
        if (approve) {
            ChatGroupMember appMem = getMembership(g.getId(), req.getApplicantId());
            if (appMem == null) {
                addMember(g.getId(), req.getApplicantId(), ROLE_MEMBER);
            } else if (!isActiveMemberRow(appMem)) {
                appMem.setMemberStatus(MEMBER_IN_GROUP);
                appMem.setRole(ROLE_MEMBER);
                appMem.setLastReadMsgId(maxGroupMessageId(g.getId()));
                chatGroupMemberMapper.updateById(appMem);
            }
            req.setStatus(REQ_APPROVED);
            req.setUpdateTime(LocalDateTime.now());
            chatGroupJoinRequestMapper.updateById(req);
            chatService.send(req.getApplicantId(),
                    "群主已同意你的入群申请，欢迎加入群聊「" + g.getName() + "」。");
        } else {
            req.setStatus(REQ_REJECTED);
            req.setUpdateTime(LocalDateTime.now());
            chatGroupJoinRequestMapper.updateById(req);
            chatService.send(req.getApplicantId(),
                    "群主已拒绝你的入群申请（群「" + g.getName() + "」）。");
        }
        return Result.ok();
    }

    @Override
    public Result messages(Long groupId, long current, long size) {
        Long me = UserHolder.getUser().getId();
        if (!hasGroupThreadAccess(groupId, me)) {
            return Result.fail("无权查看该群");
        }
        Page<GroupMessage> page = Page.of(current, size);
        var p = groupMessageMapper.selectPage(page,
                Wrappers.<GroupMessage>lambdaQuery()
                        .eq(GroupMessage::getGroupId, groupId)
                        .orderByDesc(GroupMessage::getCreateTime));
        List<GroupMessage> records = p.getRecords();
        if (records.isEmpty()) {
            return Result.ok(records, p.getTotal());
        }
        Set<Long> senderIds = records.stream()
                .map(GroupMessage::getSenderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userById = senderIds.isEmpty()
                ? Map.of()
                : userMapper.selectBatchIds(senderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (GroupMessage gm : records) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", gm.getId());
            row.put("groupId", gm.getGroupId());
            row.put("senderId", gm.getSenderId());
            row.put("content", gm.getContent());
            row.put("createTime", gm.getCreateTime());
            User u = userById.get(gm.getSenderId());
            row.put("senderNickName", u != null ? u.getNickName() : null);
            row.put("senderIcon", u != null ? emptyToNull(u.getIcon()) : null);
            rows.add(row);
        }
        return Result.ok(rows, p.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result sendGroupMessage(Long groupId, String content) {
        if (content == null || content.isBlank()) {
            return Result.fail("内容不能为空");
        }
        Long me = UserHolder.getUser().getId();
        if (!isActiveMember(groupId, me)) {
            return Result.fail("无权在该群发言");
        }
        ChatGroup g = chatGroupMapper.selectById(groupId);
        if (g == null) {
            return Result.fail("群不存在");
        }
        GroupMessage msg = new GroupMessage();
        msg.setGroupId(groupId);
        msg.setSenderId(me);
        msg.setContent(content.trim());
        msg.setCreateTime(LocalDateTime.now());
        groupMessageMapper.insert(msg);
        g.setLastMsgTime(LocalDateTime.now());
        chatGroupMapper.updateById(g);
        advanceMemberReadAfterSend(groupId, me, msg.getId());
        return Result.ok(msg.getId());
    }

    @Override
    public Result groupMeta(Long groupId) {
        Long me = UserHolder.getUser().getId();
        ChatGroupMember myMem = getMembership(groupId, me);
        if (myMem == null) {
            return Result.fail("无权查看该群");
        }
        ChatGroup g = chatGroupMapper.selectById(groupId);
        if (g == null) {
            return Result.fail("群不存在");
        }
        long memberCount = chatGroupMemberMapper.selectCount(
                Wrappers.<ChatGroupMember>lambdaQuery()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .and(w -> w.isNull(ChatGroupMember::getMemberStatus)
                                .or()
                                .eq(ChatGroupMember::getMemberStatus, MEMBER_IN_GROUP)));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", g.getId());
        map.put("name", g.getName());
        map.put("ownerId", g.getOwnerId());
        map.put("joinMode", g.getJoinMode());
        map.put("memberCount", memberCount);
        map.put("removedFromGroup", !isActiveMemberRow(myMem));
        map.put("avatar", resolveGroupAvatar(g));
        map.put("customAvatarUrl", emptyToNull(g.getAvatar()));
        User ownerUser = userMapper.selectById(g.getOwnerId());
        map.put("ownerAvatarUrl", ownerUser != null ? emptyToNull(ownerUser.getIcon()) : null);
        return Result.ok(map);
    }

    @Override
    public Result groupDetail(Long groupId) {
        Long me = UserHolder.getUser().getId();
        if (!isActiveMember(groupId, me)) {
            return Result.fail("仅群成员可查看成员列表");
        }
        ChatGroup g = chatGroupMapper.selectById(groupId);
        if (g == null) {
            return Result.fail("群不存在");
        }
        List<ChatGroupMember> members = chatGroupMemberMapper.selectList(
                Wrappers.<ChatGroupMember>lambdaQuery()
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .and(w -> w.isNull(ChatGroupMember::getMemberStatus)
                                .or()
                                .eq(ChatGroupMember::getMemberStatus, MEMBER_IN_GROUP))
                        .orderByDesc(ChatGroupMember::getRole)
                        .orderByAsc(ChatGroupMember::getCreateTime));
        Set<Long> uids = members.stream().map(ChatGroupMember::getUserId).collect(Collectors.toSet());
        Map<Long, User> userById = uids.isEmpty()
                ? Map.of()
                : userMapper.selectBatchIds(uids).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        List<Map<String, Object>> memberRows = new ArrayList<>();
        for (ChatGroupMember cm : members) {
            boolean isOwner = cm.getRole() != null && cm.getRole() == ROLE_OWNER;
            User u = userById.get(cm.getUserId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", cm.getUserId());
            row.put("nickName", u != null ? emptyToNull(u.getNickName()) : null);
            row.put("icon", u != null ? emptyToNull(u.getIcon()) : null);
            row.put("isOwner", isOwner);
            memberRows.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", g.getId());
        out.put("name", g.getName());
        out.put("ownerId", g.getOwnerId());
        out.put("joinMode", g.getJoinMode());
        out.put("memberCount", memberRows.size());
        out.put("avatar", resolveGroupAvatar(g));
        out.put("customAvatarUrl", emptyToNull(g.getAvatar()));
        out.put("members", memberRows);
        return Result.ok(out);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result kickMember(Long groupId, Long targetUserId) {
        Long me = UserHolder.getUser().getId();
        if (targetUserId == null || targetUserId.equals(me)) {
            return Result.fail("无法移出该成员");
        }
        ChatGroup g = chatGroupMapper.selectById(groupId);
        if (g == null) {
            return Result.fail("群不存在");
        }
        if (!g.getOwnerId().equals(me)) {
            return Result.fail("仅群主可移出成员");
        }
        ChatGroupMember target = getMembership(groupId, targetUserId);
        if (target == null || !isActiveMemberRow(target)) {
            return Result.fail("该用户不在群内");
        }
        if (target.getRole() != null && target.getRole() == ROLE_OWNER) {
            return Result.fail("无法移出群主");
        }
        target.setMemberStatus(MEMBER_REMOVED);
        chatGroupMemberMapper.updateById(target);
        chatService.send(targetUserId,
                "你已被移出群聊「" + g.getName() + "」，无法再接收或发送群消息，历史记录仍可在消息列表中查看。");
        return Result.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateGroupProfile(Long groupId, GroupUpdateDTO dto) {
        if (dto == null) {
            return Result.fail("参数无效");
        }
        Long me = UserHolder.getUser().getId();
        ChatGroup g = chatGroupMapper.selectById(groupId);
        if (g == null) {
            return Result.fail("群不存在");
        }
        if (!g.getOwnerId().equals(me)) {
            return Result.fail("仅群主可编辑群资料");
        }
        boolean changed = false;
        if (dto.getName() != null) {
            String n = dto.getName().trim();
            if (n.isEmpty()) {
                return Result.fail("群名称不能为空");
            }
            if (n.length() > 128) {
                return Result.fail("群名称过长");
            }
            g.setName(n);
            changed = true;
        }
        if (dto.getAvatar() != null) {
            String a = dto.getAvatar().trim();
            if (a.length() > 512) {
                return Result.fail("头像地址过长");
            }
            g.setAvatar(a.isEmpty() ? null : a);
            changed = true;
        }
        if (!changed) {
            return Result.fail("请至少修改一项");
        }
        chatGroupMapper.updateById(g);
        return Result.ok();
    }

    private String resolveGroupAvatar(ChatGroup g) {
        String custom = emptyToNull(g.getAvatar());
        if (custom != null) {
            return custom;
        }
        User owner = userMapper.selectById(g.getOwnerId());
        return owner != null ? emptyToNull(owner.getIcon()) : null;
    }

    private static String emptyToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s;
    }
}
