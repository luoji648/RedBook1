<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, MoreFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  groupMeta,
  groupMessages,
  groupSend,
  groupMarkRead,
  groupDetail,
  groupKick,
  fetchMe,
} from '../api'
import { useMessageUnreadStore } from '../stores/messageUnread'

const DEFAULT_AVATAR =
  'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const route = useRoute()
const router = useRouter()
const msgUnread = useMessageUnreadStore()
const groupId = computed(() => route.params.groupId)

const myId = ref(null)
const meta = ref(null)
const msgs = ref([])
const text = ref('')

const detailOpen = ref(false)
const detailLoading = ref(false)
const detailPayload = ref(null)

const groupName = computed(() => meta.value?.name || `群聊 ${groupId.value}`)
const groupAvatar = computed(() => meta.value?.avatar || DEFAULT_AVATAR)
const memberCountText = computed(() => {
  const n = meta.value?.memberCount
  return typeof n === 'number' ? `${n} 人` : ''
})
const removedFromGroup = computed(() => !!meta.value?.removedFromGroup)
const iAmOwner = computed(
  () => myId.value != null && meta.value?.ownerId === myId.value
)

function senderDisplayName(m) {
  if (m.senderNickName && String(m.senderNickName).trim()) return m.senderNickName
  return m.senderId != null ? `用户 ${m.senderId}` : '用户'
}

async function loadMe() {
  try {
    const { data } = await fetchMe()
    myId.value = data?.user?.id
  } catch {
    myId.value = null
  }
}

async function loadMeta() {
  try {
    const { data } = await groupMeta(groupId.value)
    meta.value = data
  } catch {
    meta.value = null
  }
}

async function loadMsgs() {
  if (!meta.value) {
    msgs.value = []
    return
  }
  try {
    const { data } = await groupMessages(groupId.value, { current: 1, size: 100 })
    const arr = data || []
    msgs.value = arr.slice().reverse()
  } catch {
    msgs.value = []
  }
}

async function markReadAndRefreshUnread() {
  if (!meta.value || removedFromGroup.value) return
  const gid = groupId.value
  if (gid == null || gid === '') return
  try {
    await groupMarkRead(gid)
    await msgUnread.refresh()
  } catch {
    /* 已提示 */
  }
}

async function send() {
  if (removedFromGroup.value) return
  const t = text.value.trim()
  if (!t) return
  try {
    await groupSend(groupId.value, t)
    text.value = ''
    await loadMsgs()
    await markReadAndRefreshUnread()
  } catch {
    /* */
  }
}

async function openDetailPanel() {
  detailOpen.value = true
  detailPayload.value = null
  if (removedFromGroup.value) {
    return
  }
  detailLoading.value = true
  try {
    const { data } = await groupDetail(groupId.value)
    detailPayload.value = data
  } catch {
    detailPayload.value = null
    ElMessage.error('无法加载群成员')
  } finally {
    detailLoading.value = false
  }
}

function displayNick(m) {
  if (m.nickName && String(m.nickName).trim()) return m.nickName
  return m.userId != null ? `用户 ${m.userId}` : '用户'
}

function goEditGroup() {
  if (!iAmOwner.value) return
  detailOpen.value = false
  router.push({ name: 'group-edit', params: { groupId: String(groupId.value) } })
}

async function confirmKick(m) {
  if (!iAmOwner.value || m.isOwner || m.userId === myId.value) return
  try {
    await ElMessageBox.confirm(
      `确定将「${displayNick(m)}」移出群聊？移出后对方将收到通知，且无法再收发本群消息。`,
      '移出群聊',
      {
        type: 'warning',
        confirmButtonText: '移出',
        cancelButtonText: '取消',
      }
    )
  } catch {
    return
  }
  try {
    await groupKick(groupId.value, m.userId)
    ElMessage.success('已移出该成员')
    detailLoading.value = true
    try {
      const { data } = await groupDetail(groupId.value)
      detailPayload.value = data
    } catch {
      detailPayload.value = null
    } finally {
      detailLoading.value = false
    }
    await loadMeta()
  } catch {
    ElMessage.error('移出失败')
  }
}

watch(groupId, async () => {
  await loadMeta()
  await loadMsgs()
  await markReadAndRefreshUnread()
})

watch(removedFromGroup, async () => {
  await loadMsgs()
})

onMounted(async () => {
  await loadMe()
  await loadMeta()
  await loadMsgs()
  await markReadAndRefreshUnread()
})
</script>

<template>
  <div class="thread">
    <header class="bar">
      <el-button text circle title="返回" class="back-btn" @click="router.push({ name: 'chat' })">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <div
        v-if="meta"
        class="group-head"
        role="button"
        tabindex="0"
        @click="openDetailPanel"
        @keydown.enter="openDetailPanel"
      >
        <img class="group-av" :src="groupAvatar" alt="" />
        <div class="title-block">
          <span class="peer-name">{{ groupName }}</span>
          <span class="sub">{{ memberCountText || '群聊' }}</span>
        </div>
      </div>
      <div v-else class="title-block title-block--placeholder">
        <span class="peer-name">群聊</span>
      </div>
      <el-button
        v-if="meta"
        text
        circle
        class="info-btn"
        title="群信息"
        @click.stop="openDetailPanel"
      >
        <el-icon :size="22"><MoreFilled /></el-icon>
      </el-button>
    </header>
    <div v-if="!meta" class="hint">无法加载群聊，请确认已加入该群。</div>
    <template v-else>
      <div v-if="removedFromGroup" class="removed-tip">
        您已被移出群聊，无法接收新消息与发送消息，仍可查看历史记录。
      </div>
      <div class="msgs">
        <div
          v-for="m in msgs"
          :key="m.id"
          :class="['msg-row', m.senderId === myId ? 'me' : 'they']"
        >
          <img
            class="msg-av"
            :src="m.senderIcon || DEFAULT_AVATAR"
            alt=""
          />
          <div class="msg-body">
            <div class="msg-nick">{{ senderDisplayName(m) }}</div>
            <div :class="['bubble', m.senderId === myId ? 'me' : 'they']">
              {{ m.content }}
              <div class="ts">{{ m.createTime }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="input">
        <el-input
          v-model="text"
          placeholder="输入消息"
          :disabled="removedFromGroup"
          @keyup.enter="send"
        />
        <el-button type="primary" :disabled="removedFromGroup" @click="send">发送</el-button>
      </div>
    </template>

    <el-drawer
      v-model="detailOpen"
      direction="btt"
      size="72%"
      class="group-detail-drawer"
      :show-close="true"
      title="群聊信息"
    >
      <div v-if="removedFromGroup" class="detail-body">
        <div class="detail-title">{{ groupName }}</div>
        <p class="detail-muted">您已被移出该群，无法查看成员列表。</p>
      </div>
      <div v-else class="detail-body">
        <div v-loading="detailLoading" class="detail-inner">
          <template v-if="detailPayload">
            <div
              class="detail-summary"
              :class="{ 'detail-summary--owner': iAmOwner }"
              @click="iAmOwner && goEditGroup()"
            >
              <img
                class="detail-summary-av"
                :src="detailPayload.avatar || DEFAULT_AVATAR"
                alt=""
              />
              <div class="detail-summary-text">
                <div class="detail-title">{{ detailPayload.name || '群聊' }}</div>
                <div class="detail-sub">群成员 · {{ detailPayload.memberCount ?? 0 }} 人</div>
                <div v-if="iAmOwner" class="detail-edit-hint">点击编辑群名称与头像</div>
              </div>
            </div>
            <div class="member-list">
              <div
                v-for="m in detailPayload.members"
                :key="m.userId"
                class="member-row"
              >
                <img class="member-av" :src="m.icon || DEFAULT_AVATAR" alt="" />
                <div class="member-meta">
                  <span class="member-name">{{ displayNick(m) }}</span>
                  <span v-if="m.isOwner" class="owner-badge">群主</span>
                </div>
                <button
                  v-if="iAmOwner && !m.isOwner && m.userId !== myId"
                  type="button"
                  class="kick-link"
                  @click="confirmKick(m)"
                >
                  移出
                </button>
              </div>
            </div>
          </template>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.thread {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}
.bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-bottom: 1px solid #eee;
}
.back-btn {
  margin-left: -4px;
  flex-shrink: 0;
  color: #333;
}
.group-head {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
  cursor: pointer;
}
.group-av {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;
  background: #f0f0f0;
}
.title-block {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.title-block--placeholder {
  flex: 1;
}
.peer-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub {
  font-size: 11px;
  color: #999;
}
.info-btn {
  flex-shrink: 0;
  margin-left: auto;
  color: #333;
  border: 1px solid #e8e8e8;
  background: #fafafa;
}
.hint {
  padding: 24px;
  text-align: center;
  color: #999;
  font-size: 14px;
}
.removed-tip {
  padding: 10px 12px;
  font-size: 13px;
  color: #c45656;
  background: #fef0f0;
  border-bottom: 1px solid #fde2e2;
}
.msgs {
  flex: 1;
  overflow: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.msg-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  max-width: 100%;
}
.msg-row.they {
  flex-direction: row;
  justify-content: flex-start;
}
.msg-row.me {
  flex-direction: row-reverse;
  justify-content: flex-start;
  align-self: flex-end;
}
.msg-av {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.msg-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  max-width: calc(100% - 44px);
}
.msg-row.me .msg-body {
  align-items: flex-end;
}
.msg-nick {
  font-size: 12px;
  color: #888;
  line-height: 1.2;
  padding: 0 2px;
}
.msg-row.me .msg-nick {
  text-align: right;
}
.bubble {
  max-width: 100%;
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.4;
}
.bubble.they {
  background: #f0f0f0;
  color: #333;
}
.bubble.me {
  background: #ffeef0;
  color: #333;
}
.ts {
  font-size: 10px;
  color: #999;
  margin-top: 4px;
}
.input {
  display: flex;
  gap: 8px;
  padding: 10px;
  border-top: 1px solid #eee;
}
.detail-body {
  padding: 0 4px 16px;
  min-height: 120px;
}
.detail-inner {
  min-height: 160px;
}
.detail-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 14px;
  margin-bottom: 4px;
  border-bottom: 1px solid #f0f0f0;
}
.detail-summary--owner {
  cursor: pointer;
}
.detail-summary--owner:active {
  opacity: 0.88;
}
.detail-summary-av {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  object-fit: cover;
  flex-shrink: 0;
  background: #f0f0f0;
}
.detail-summary-text {
  min-width: 0;
  flex: 1;
}
.detail-title {
  font-size: 18px;
  font-weight: 700;
  color: #222;
  margin-bottom: 6px;
}
.detail-sub {
  font-size: 13px;
  color: #888;
  margin-bottom: 0;
}
.detail-edit-hint {
  font-size: 12px;
  color: #999;
  margin-top: 6px;
}
.detail-muted {
  font-size: 14px;
  color: #999;
  line-height: 1.5;
}
.member-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.member-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}
.member-av {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  background: #f0f0f0;
}
.member-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}
.member-name {
  font-size: 15px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.owner-badge {
  flex-shrink: 0;
  font-size: 11px;
  color: #f56c6c;
  background: #fff0f0;
  padding: 2px 8px;
  border-radius: 4px;
}
.kick-link {
  flex-shrink: 0;
  border: none;
  background: none;
  color: #f56c6c;
  font-size: 14px;
  cursor: pointer;
  padding: 6px 4px;
}
.kick-link:hover {
  text-decoration: underline;
}
</style>

<style>
/* drawer 标题与内容区留白 */
.group-detail-drawer .el-drawer__header {
  margin-bottom: 8px;
}
</style>
