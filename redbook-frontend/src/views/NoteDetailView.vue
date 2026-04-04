<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Share, StarFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import {
  noteDetail,
  noteRelated,
  commentTree,
  commentAdd,
  likeNote,
  unlikeNote,
  collectNote,
  uncollectNote,
  followUser,
  unfollowUser,
  followFollowing,
  shareNote,
  chatSend,
  productGet,
  apiBase,
} from '../api'
import { streamNoteAi } from '../utils/sseAi'
import NoteCard from '../components/NoteCard.vue'
import CommentBlock from '../components/CommentBlock.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const noteId = computed(() => route.params.id)

const detail = ref(null)
const related = ref([])
const comments = ref([])
const followingIds = ref(new Set())
const collected = ref(false)
const products = ref([])

const replyParent = ref(null)
const replyToUserId = ref(null)
const replyHint = ref('')
const commentText = ref('')

const aiOpen = ref(false)
const aiMode = ref('SUMMARY_POST')
const aiQuestion = ref('')
const aiText = ref('')
const aiLoading = ref(false)
let aiAbort = null

const note = computed(() => detail.value?.note)
const author = computed(() => detail.value?.author)
const media = computed(() => detail.value?.media || [])
const liked = computed(() => !!detail.value?.liked)
const likeCount = computed(() => detail.value?.likeCount ?? note.value?.likeCount ?? 0)

async function refresh() {
  const { data } = await noteDetail(noteId.value)
  detail.value = data
}

async function loadRelated() {
  try {
    const { data } = await noteRelated(noteId.value)
    related.value = data || []
  } catch {
    related.value = []
  }
}

async function loadComments() {
  try {
    const { data } = await commentTree(noteId.value)
    comments.value = data || []
  } catch {
    comments.value = []
  }
}

async function loadFollowing() {
  if (!auth.token) return
  try {
    const { data } = await followFollowing({ current: 1, size: 200 })
    followingIds.value = new Set((data || []).map((u) => u.id))
  } catch {
    /* */
  }
}

async function loadProducts() {
  const nps = detail.value?.noteProducts
  if (!nps || !nps.length) {
    products.value = []
    return
  }
  const list = []
  for (const np of nps) {
    try {
      const { data } = await productGet(np.productId)
      if (data) list.push(data)
    } catch {
      /* */
    }
  }
  products.value = list
}

watch(noteId, async () => {
  await refresh()
  await loadRelated()
  await loadComments()
  await loadFollowing()
  await loadProducts()
})

onMounted(async () => {
  try {
    await refresh()
    await loadRelated()
    await loadComments()
    await loadFollowing()
    await loadProducts()
  } catch {
    /* http 已提示 */
  }
})

async function toggleLike() {
  if (!auth.token) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  try {
    if (liked.value) {
      await unlikeNote(note.value.id)
      detail.value.liked = false
      detail.value.likeCount = Math.max(0, likeCount.value - 1)
    } else {
      await likeNote(note.value.id)
      detail.value.liked = true
      detail.value.likeCount = likeCount.value + 1
    }
  } catch {
    /* */
  }
}

async function toggleCollect() {
  if (!auth.token) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  try {
    if (collected.value) {
      await uncollectNote(note.value.id)
      collected.value = false
    } else {
      await collectNote(note.value.id)
      collected.value = true
    }
  } catch {
    /* */
  }
}

const authorFollowed = computed(
  () => author.value && followingIds.value.has(author.value.id)
)

async function toggleFollow() {
  if (!auth.token) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  const uid = author.value.id
  try {
    if (authorFollowed.value) {
      await unfollowUser(uid)
      followingIds.value.delete(uid)
    } else {
      await followUser(uid)
      followingIds.value.add(uid)
    }
  } catch {
    /* */
  }
}

async function onShare() {
  if (!auth.token) {
    ElMessage.info('登录后可生成分享短链')
    return
  }
  try {
    const { data } = await shareNote(note.value.id)
    const path = data.path || `/s/${data.shortCode}`
    const full = `${apiBase}${path}`
    await navigator.clipboard.writeText(full)
    ElMessage.success('短链已复制')
  } catch {
    /* */
  }
}

async function dmAuthor() {
  if (!auth.token) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  try {
    const { value } = await ElMessageBox.prompt('输入私信内容', '私信作者', {
      confirmButtonText: '发送',
      cancelButtonText: '取消',
    })
    if (!value) return
    await chatSend(author.value.id, value)
    ElMessage.success('已发送，请在「消息」中查看')
    router.push({ name: 'chat' })
  } catch {
    /* cancel */
  }
}

function onReply({ id, userId, nick }) {
  replyParent.value = id
  replyToUserId.value = userId
  replyHint.value = `回复 ${nick}`
}

function clearReply() {
  replyParent.value = null
  replyToUserId.value = null
  replyHint.value = ''
}

async function submitComment() {
  if (!auth.token) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!commentText.value.trim()) {
    ElMessage.warning('请输入评论')
    return
  }
  try {
    const payload = { content: commentText.value.trim() }
    if (replyParent.value != null) payload.parentId = replyParent.value
    if (replyToUserId.value != null) payload.replyToUserId = replyToUserId.value
    await commentAdd(note.value.id, payload)
    commentText.value = ''
    clearReply()
    await loadComments()
  } catch {
    /* */
  }
}

function goProduct(p) {
  router.push({ name: 'product-detail', params: { id: p.id } })
}

function priceYuan(cent) {
  return ((cent || 0) / 100).toFixed(2)
}

async function runAi() {
  if (!auth.token) {
    ElMessage.info('请先登录后使用 AI 助手')
    return
  }
  if (aiMode.value === 'QA' && !aiQuestion.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }
  aiLoading.value = true
  aiText.value = ''
  aiAbort?.abort()
  aiAbort = new AbortController()
  try {
    await streamNoteAi({
      noteId: note.value.id,
      mode: aiMode.value,
      question: aiMode.value === 'QA' ? aiQuestion.value : '',
      token: auth.token,
      signal: aiAbort.signal,
      onAnswer: (t) => {
        aiText.value += t
      },
    })
  } catch (e) {
    if (e.name !== 'AbortError') ElMessage.error(e.message || 'AI 请求失败')
  } finally {
    aiLoading.value = false
  }
}

function closeAi() {
  aiAbort?.abort()
  aiOpen.value = false
}
</script>

<template>
  <div v-if="note" class="page">
    <header class="bar">
      <el-button text @click="router.back()">返回</el-button>
    </header>

    <div class="carousel">
      <video
        v-if="note.type === 1 && media[0]"
        :src="media[0].url"
        controls
        class="mv"
      />
      <el-carousel v-else-if="media.length" height="360px" indicator-position="outside">
        <el-carousel-item v-for="m in media" :key="m.id || m.url">
          <img :src="m.url" class="cv" alt="" />
        </el-carousel-item>
      </el-carousel>
      <div v-else class="no-media">暂无配图</div>
    </div>

    <div class="body">
      <h1>{{ note.title || '笔记' }}</h1>
      <pre class="content">{{ note.content }}</pre>

      <div class="author">
        <img :src="author?.icon || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" class="av" alt="" />
        <div class="info">
          <div class="name">{{ author?.nickName || '用户' }}</div>
        </div>
        <el-button
          v-if="auth.token && author && author.id"
          size="small"
          :type="authorFollowed ? 'default' : 'primary'"
          @click="toggleFollow"
        >
          {{ authorFollowed ? '已关注' : '关注' }}
        </el-button>
        <el-button size="small" :icon="ChatDotRound" @click="dmAuthor">私信</el-button>
      </div>

      <div class="acts">
        <el-button :type="liked ? 'danger' : 'default'" @click="toggleLike">
          ♥ {{ likeCount }}
        </el-button>
        <el-button :type="collected ? 'warning' : 'default'" @click="toggleCollect">
          <el-icon><StarFilled /></el-icon>
          {{ collected ? '已收藏' : '收藏' }}
        </el-button>
        <el-button :icon="Share" @click="onShare">分享短链</el-button>
      </div>

      <section v-if="products.length" class="sec">
        <h3>相似 / 关联商品</h3>
        <div class="prows">
          <div v-for="p in products" :key="p.id" class="pro" @click="goProduct(p)">
            <img :src="p.cover || ''" alt="" />
            <div class="pt">{{ p.title }}</div>
            <div class="pp">¥{{ priceYuan(p.priceCent) }}</div>
          </div>
        </div>
      </section>

      <section class="sec">
        <h3>评论</h3>
        <div v-if="replyHint" class="rh">
          {{ replyHint }}
          <el-button link type="primary" @click="clearReply">取消</el-button>
        </div>
        <el-input
          v-model="commentText"
          type="textarea"
          :rows="3"
          placeholder="说点什么…"
        />
        <el-button type="primary" class="cmt-btn" @click="submitComment">发表评论</el-button>
        <CommentBlock :nodes="comments" @reply="onReply" />
      </section>

      <section v-if="related.length" class="sec">
        <h3>相关笔记</h3>
        <div class="rel">
          <NoteCard v-for="(vo, i) in related" :key="(vo.note && vo.note.id) || i" :vo="vo" />
        </div>
      </section>
    </div>

    <div class="fab-ai" @click="aiOpen = true">AI</div>

    <el-drawer v-model="aiOpen" title="AI 助手" size="90%" direction="btt" @close="closeAi">
      <el-radio-group v-model="aiMode" size="small">
        <el-radio-button label="SUMMARY_POST">总结帖子</el-radio-button>
        <el-radio-button label="SUMMARY_COMMENTS">总结评论</el-radio-button>
        <el-radio-button label="QA">自由提问</el-radio-button>
      </el-radio-group>
      <el-input
        v-if="aiMode === 'QA'"
        v-model="aiQuestion"
        type="textarea"
        :rows="2"
        placeholder="针对本帖提问"
        style="margin-top: 10px"
      />
      <el-button type="primary" :loading="aiLoading" style="margin-top: 10px" @click="runAi">
        开始生成
      </el-button>
      <div class="ai-out">{{ aiText || (aiLoading ? '思考中…' : '') }}</div>
    </el-drawer>
  </div>
  <div v-else class="loading">加载中…</div>
</template>

<style scoped>
.page {
  max-width: 640px;
  margin: 0 auto;
  background: #fff;
  min-height: 100vh;
  padding-bottom: 40px;
}
.bar {
  padding: 8px 12px;
  border-bottom: 1px solid #eee;
}
.carousel {
  background: #000;
}
.cv {
  width: 100%;
  height: 360px;
  object-fit: contain;
  background: #111;
}
.mv {
  width: 100%;
  max-height: 420px;
  display: block;
}
.no-media {
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  background: #f5f5f5;
}
.body {
  padding: 16px;
}
h1 {
  font-size: 20px;
  margin: 0 0 12px;
}
.content {
  white-space: pre-wrap;
  font-family: inherit;
  font-size: 15px;
  line-height: 1.6;
  color: #333;
  margin: 0 0 16px;
}
.author {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.av {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
}
.info {
  flex: 1;
}
.name {
  font-weight: 600;
}
.acts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}
.sec {
  margin-top: 24px;
}
.sec h3 {
  font-size: 16px;
  margin: 0 0 12px;
}
.prows {
  display: flex;
  gap: 12px;
  overflow-x: auto;
}
.pro {
  width: 120px;
  flex-shrink: 0;
  cursor: pointer;
}
.pro img {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
}
.pt {
  font-size: 13px;
  margin-top: 4px;
}
.pp {
  color: #ff2442;
  font-weight: 600;
}
.cmt-btn {
  margin: 8px 0 16px;
}
.rel {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.rh {
  font-size: 13px;
  color: #666;
  margin-bottom: 6px;
}
.fab-ai {
  position: fixed;
  right: 16px;
  bottom: 72px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6c5ce7, #a29bfe);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  box-shadow: 0 4px 16px rgba(108, 92, 231, 0.4);
  cursor: pointer;
  z-index: 40;
}
.ai-out {
  margin-top: 16px;
  padding: 12px;
  background: #f8f8f8;
  border-radius: 8px;
  min-height: 120px;
  white-space: pre-wrap;
  font-size: 14px;
  line-height: 1.6;
}
.loading {
  text-align: center;
  padding: 40px;
  color: #999;
}
</style>
