<script setup>
import { Delete } from '@element-plus/icons-vue'

defineOptions({ name: 'CommentBlock' })

const DEFAULT_AVATAR = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

defineProps({
  nodes: { type: Array, default: () => [] },
  myUserId: { type: [Number, String], default: null },
  /** 0=楼层主评，1=楼中楼（仅二级，不再嵌套） */
  depth: { type: Number, default: 0 },
})
const emit = defineEmits(['reply', 'delete'])

function commentRow(node) {
  return node?.comment ?? {}
}

/** 兼容 camelCase / snake_case（若后端或网关改过命名策略） */
function displayNick(node) {
  const row = commentRow(node)
  const nick = row.nickName ?? row.nick_name ?? node?.nickName ?? node?.nick_name
  const uid = row.userId ?? row.user_id
  return nick || '用户' + uid
}

function commentIcon(node) {
  const row = commentRow(node)
  const icon = row.icon ?? row.userIcon ?? row.user_icon
  return icon && String(icon).trim() ? icon : DEFAULT_AVATAR
}

function replyToUid(node) {
  const row = commentRow(node)
  return row.replyToUserId ?? row.reply_to_user_id
}

function replyToDisplayNick(node) {
  const row = commentRow(node)
  return row.replyToNickName ?? row.reply_to_nick_name ?? node?.replyToNickName ?? node?.reply_to_nick_name
}

function commentPk(node) {
  return commentRow(node).id
}

function commentUid(node) {
  const row = commentRow(node)
  const uid = row.userId ?? row.user_id
  return uid == null ? null : Number(uid)
}

function isMine(node, myUserId) {
  if (myUserId == null) return false
  const uid = commentUid(node)
  if (uid == null) return false
  return uid === Number(myUserId)
}

/** 后端仅二级：新回复的 parentId 须为楼层根评论 id */
function rootParentId(node) {
  const row = commentRow(node)
  const pid = row.parentId ?? row.parent_id
  const id = row.id
  if (pid == null || pid === 0) return id
  return Number(pid)
}

function replyTarget(node) {
  const row = commentRow(node)
  emit('reply', {
    parentId: rootParentId(node),
    replyToUserId: row.userId ?? row.user_id,
    nick: displayNick(node),
  })
}

function removeComment(node) {
  const row = commentRow(node)
  const id = row.id
  if (id != null) emit('delete', id)
}
</script>

<template>
  <div v-for="node in nodes" :key="commentPk(node)" class="c" :class="{ 'is-reply': depth > 0 }">
    <div class="line" :class="{ 'line-nested': depth > 0 }">
      <img :src="commentIcon(node)" class="avatar" alt="" />
      <div class="main">
        <div class="meta">
          <span v-if="replyToUid(node) != null" class="rt"
            >回复 {{ replyToDisplayNick(node) || '用户' + replyToUid(node) }} ·
          </span>
          <span class="u">{{ displayNick(node) }}</span>
        </div>
        <div class="txt">{{ commentRow(node).content }}</div>
        <div class="ops">
          <el-button link type="primary" size="small" @click="replyTarget(node)">回复</el-button>
          <el-button
            v-if="isMine(node, myUserId)"
            link
            type="danger"
            size="small"
            class="del-btn"
            title="删除"
            @click="removeComment(node)"
          >
            <el-icon :size="16"><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
    <CommentBlock
      v-if="depth === 0 && node.replies && node.replies.length"
      class="replies-wrap"
      :depth="1"
      :nodes="node.replies"
      :my-user-id="myUserId"
      @reply="(x) => emit('reply', x)"
      @delete="(id) => emit('delete', id)"
    />
  </div>
</template>

<style scoped>
.c {
  margin-bottom: 12px;
}
/* 二级及以下：相对父级明显右移，左侧引导线便于扫读层级 */
.c.is-reply {
  margin-left: 0;
  padding-left: 14px;
  border-left: 3px solid #e6e6e6;
  margin-top: 2px;
}
.line {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 14px;
  line-height: 1.5;
}
.line-nested {
  padding-left: 8px;
}
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.line-nested .avatar {
  width: 30px;
  height: 30px;
}
.main {
  flex: 1;
  min-width: 0;
}
.meta {
  margin-bottom: 2px;
}
.rt {
  color: #999;
  font-size: 12px;
  margin-right: 4px;
}
.u {
  color: #333;
  font-weight: 600;
  font-size: 13px;
}
.txt {
  color: #333;
  word-break: break-word;
}
.ops {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
}
.del-btn {
  padding: 4px !important;
  min-height: auto !important;
}
.replies-wrap {
  margin-top: 10px;
  margin-left: 36px;
  padding-left: 4px;
}
</style>
