<script setup>
import { ref, onMounted, computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, User, ChatLineRound, Plus } from '@element-plus/icons-vue'
import { chatThreads, fetchMe, groupMy, groupCreate, ossPresign } from '../api'
import { useMessageUnreadStore } from '../stores/messageUnread'

const router = useRouter()
const msgUnread = useMessageUnreadStore()
const { noticeLikeCollect, noticeFollow, noticeComment } = storeToRefs(msgUnread)

function badgeFromCount(n) {
  const x = Number(n) || 0
  if (x <= 0) return ''
  return x > 99 ? '99+' : String(x)
}

// computed 内对 ref 显式 .value，保证 Pinia 状态更新时角标一定重算
const badgeLikeCollect = computed(() => badgeFromCount(noticeLikeCollect.value))
const badgeFollow = computed(() => badgeFromCount(noticeFollow.value))
const badgeComment = computed(() => badgeFromCount(noticeComment.value))

const threads = ref([])
const groups = ref([])
const myId = ref(null)
const createDlg = ref(false)
const creating = ref(false)
const createUploading = ref(false)
const createForm = ref({ name: '', joinMode: 0, avatar: '' })

function openCreateGroupDialog() {
  createForm.value = { name: '', joinMode: 0, avatar: '' }
  createDlg.value = true
}

async function handleCreateAvatarUpload(opt) {
  const { file, onError } = opt
  const raw = file.raw ?? file
  createUploading.value = true
  try {
    const ext = '.' + (String(raw.name || 'image.jpg').split('.').pop() || 'jpg')
    const contentType = raw.type || 'application/octet-stream'
    const { data } = await ossPresign(ext, contentType)
    const headers = { 'Content-Type': contentType }
    if (data.putAclPublicRead) {
      headers['x-oss-object-acl'] = 'public-read'
    }
    const putRes = await fetch(data.uploadUrl, {
      method: 'PUT',
      body: raw,
      headers,
    })
    if (!putRes.ok) throw new Error('上传失败')
    createForm.value.avatar = data.publicUrl
    ElMessage.success('头像已上传')
    opt.onSuccess?.(data)
  } catch (e) {
    ElMessage.error(e.message || '上传失败')
    onError(e)
  } finally {
    createUploading.value = false
  }
}

async function loadThreads() {
  try {
    const { data: me } = await fetchMe()
    myId.value = me?.user?.id
    const { data } = await chatThreads({ current: 1, size: 50 })
    threads.value = data || []
  } catch {
    threads.value = []
  }
  try {
    const { data: glist } = await groupMy()
    groups.value = glist || []
  } catch {
    groups.value = []
  }
  await msgUnread.refresh()
}

function unreadBadgeText(n) {
  const x = Number(n) || 0
  if (x <= 0) return ''
  return x > 99 ? '99+' : String(x)
}

function goNotice(slug) {
  router.push({ name: 'notice-category', params: { slug } })
}

function peer(t) {
  if (!myId.value) return null
  return t.userLow === myId.value ? t.userHigh : t.userLow
}

function openThread(t) {
  const p = peer(t)
  router.push({
    name: 'chat-thread',
    params: { threadId: t.id },
    query: p != null ? { peer: String(p) } : {},
  })
}

function openGroup(g) {
  router.push({ name: 'group-chat', params: { groupId: String(g.id) } })
}

async function submitCreateGroup() {
  const name = createForm.value.name?.trim()
  if (!name) {
    ElMessage.warning('请填写群名称')
    return
  }
  creating.value = true
  try {
    const body = { name, joinMode: createForm.value.joinMode }
    const av = createForm.value.avatar?.trim()
    if (av) body.avatar = av
    const { data: gid } = await groupCreate(body)
    ElMessage.success('已创建')
    createDlg.value = false
    createForm.value = { name: '', joinMode: 0, avatar: '' }
    await loadThreads()
    if (gid != null) {
      router.push({ name: 'group-chat', params: { groupId: String(gid) } })
    }
  } finally {
    creating.value = false
  }
}

onMounted(() => {
  loadThreads()
})
</script>

<template>
  <div class="wrap">
    <header class="page-head">
      <h2 class="page-title">消息</h2>
      <el-button type="primary" size="small" plain @click="openCreateGroupDialog">创建群聊</el-button>
    </header>

    <div class="notice-btns">
      <button type="button" class="nb" @click="goNotice('like-collect')">
        <div class="nb-ic-wrap">
          <el-icon :size="28" class="nb-icon"><Star /></el-icon>
          <span v-if="badgeLikeCollect" class="nb-badge">{{ badgeLikeCollect }}</span>
        </div>
        <span class="nb-text">赞和收藏</span>
      </button>
      <button type="button" class="nb" @click="goNotice('follow')">
        <div class="nb-ic-wrap">
          <el-icon :size="28" class="nb-icon"><User /></el-icon>
          <span v-if="badgeFollow" class="nb-badge">{{ badgeFollow }}</span>
        </div>
        <span class="nb-text">新增关注</span>
      </button>
      <button type="button" class="nb" @click="goNotice('comment')">
        <div class="nb-ic-wrap">
          <el-icon :size="28" class="nb-icon"><ChatLineRound /></el-icon>
          <span v-if="badgeComment" class="nb-badge">{{ badgeComment }}</span>
        </div>
        <span class="nb-text">评论和@</span>
      </button>
    </div>

    <div class="dm-head">群聊</div>
    <div class="dm-list">
      <div v-if="!groups.length" class="empty dm-empty">暂无群聊，可点击右上角「创建群聊」或从他人主页加入</div>
      <div
        v-for="g in groups"
        :key="'g' + g.id"
        class="row"
        @click="openGroup(g)"
      >
        <img v-if="g.avatar" class="av av-img" :src="g.avatar" alt="" />
        <div v-else class="av ph g">群</div>
        <div class="info">
          <div class="name-line">
            <div class="name-wrap">
              <span class="name">{{ g.name || '群聊' }}</span>
              <span v-if="g.removedFromGroup" class="removed-tag">已移出</span>
            </div>
            <span v-if="unreadBadgeText(g.unreadCount)" class="row-badge">{{ unreadBadgeText(g.unreadCount) }}</span>
          </div>
          <div class="time">{{ g.lastMsgTime || '' }}</div>
        </div>
      </div>
    </div>

    <div class="dm-head">私信</div>
    <div class="dm-list">
      <div v-if="!threads.length" class="empty dm-empty">暂无私信，在笔记详情可向作者发私信</div>
      <div
        v-for="t in threads"
        :key="t.id"
        class="row"
        @click="openThread(t)"
      >
        <div class="av ph">{{ peer(t) }}</div>
        <div class="info">
          <div class="name-line">
            <span class="name">用户 {{ peer(t) }}</span>
            <span v-if="unreadBadgeText(t.unreadCount)" class="row-badge">{{ unreadBadgeText(t.unreadCount) }}</span>
          </div>
          <div class="time">{{ t.lastMsgTime || '' }}</div>
        </div>
      </div>
    </div>

    <el-dialog v-model="createDlg" title="创建群聊" width="90%" style="max-width: 400px">
      <el-form label-position="top">
        <el-form-item label="群头像（可选）">
          <div class="create-av-row">
            <el-upload
              class="create-av-uploader"
              :show-file-list="false"
              accept="image/*"
              :http-request="handleCreateAvatarUpload"
              :disabled="createUploading"
            >
              <img
                v-if="createForm.avatar"
                class="create-av-preview"
                :src="createForm.avatar"
                alt=""
              />
              <div v-else class="create-av-placeholder">
                <el-icon :size="26"><Plus /></el-icon>
                <span>上传</span>
              </div>
            </el-upload>
            <el-button
              v-if="createForm.avatar"
              text
              type="primary"
              @click="createForm.avatar = ''"
            >
              清除
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="群名称">
          <el-input v-model="createForm.name" maxlength="128" placeholder="给群聊起个名字" />
        </el-form-item>
        <el-form-item label="加群方式">
          <el-radio-group v-model="createForm.joinMode">
            <el-radio :label="0">无需验证</el-radio>
            <el-radio :label="1">需群主验证</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDlg = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreateGroup">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.wrap {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  overflow: visible;
}
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}
.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
}
.notice-btns {
  display: flex;
  justify-content: space-around;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
  overflow: visible;
}
.nb {
  flex: 1;
  max-width: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  padding: 12px 8px 10px;
  border: none;
  border-radius: 12px;
  background: #fafafa;
  cursor: pointer;
  color: #333;
  overflow: visible;
  position: relative;
}
.nb:active {
  opacity: 0.85;
}
.nb-ic-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: visible;
}
.nb-badge {
  position: absolute;
  top: -6px;
  right: -10px;
  z-index: 2;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #ff2442;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  line-height: 16px;
  text-align: center;
  box-sizing: border-box;
  border: 1px solid #fafafa;
}
.nb-icon {
  color: #ff2442;
}
.nb-text {
  font-size: 12px;
  line-height: 1.2;
  text-align: center;
  color: #333;
}
.empty {
  color: #999;
  padding: 20px;
  text-align: center;
  font-size: 14px;
}
.dm-empty {
  padding: 16px;
}
.dm-head {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}
.row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
}
.row .av.av-img {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  background: #f0f0f0;
}
.row .av.ph {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #eee;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #666;
}
.row .av.ph.g {
  background: #ffeef0;
  color: #ff2442;
  font-weight: 700;
}
.row .info {
  flex: 1;
}
.name-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}
.name-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  flex: 1;
}
.name {
  font-weight: 600;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.removed-tag {
  flex-shrink: 0;
  font-size: 10px;
  color: #999;
  border: 1px solid #e0e0e0;
  padding: 0 5px;
  border-radius: 4px;
  line-height: 16px;
}
.row-badge {
  flex-shrink: 0;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #ff2442;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  line-height: 18px;
  text-align: center;
  box-sizing: border-box;
}
.time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
.create-av-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.create-av-uploader :deep(.el-upload) {
  border: 1px dashed #dcdfe6;
  border-radius: 10px;
  cursor: pointer;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  overflow: hidden;
}
.create-av-preview {
  width: 80px;
  height: 80px;
  object-fit: cover;
  display: block;
}
.create-av-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #999;
  font-size: 12px;
  width: 100%;
  height: 100%;
}
</style>
