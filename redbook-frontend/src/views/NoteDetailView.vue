<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Camera,
  ChatDotRound,
  Close,
  Microphone,
  MoreFilled,
  Promotion,
  Share,
  StarFilled,
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import {
  noteDetail,
  noteRelated,
  commentTree,
  commentAdd,
  commentDelete,
  likeNote,
  unlikeNote,
  collectNote,
  uncollectNote,
  followUser,
  unfollowUser,
  followFollowing,
  shareNote,
  chatEnsureThread,
  productGet,
  cartAdd,
  apiBase,
  fetchMe,
  deleteNote,
} from '../api'
import { streamNoteAi } from '../utils/sseAi'
import NoteCard from '../components/NoteCard.vue'
import CommentBlock from '../components/CommentBlock.vue'
import DirectPayDialog from '../components/DirectPayDialog.vue'
import {
  isProductOnShelf,
  isProductSoldOut,
  PRODUCT_SOLD_OUT_MSG,
} from '../utils/productShelf'

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

const productMenuOpen = ref(false)
const menuProduct = ref(null)
const directPayOpen = ref(false)
const directPayProduct = ref(null)

const replyParent = ref(null)
const replyToUserId = ref(null)
const replyHint = ref('')
const commentText = ref('')

const aiOpen = ref(false)
const aiComposerText = ref('')
const aiMessages = ref([])
const aiChatScrollRef = ref(null)
const aiLoading = ref(false)
let aiAbort = null
let aiMsgSeq = 0

const AI_SUGGESTIONS = [
  {
    label: '这篇笔记主要讲了什么？',
    mode: 'QA',
    question: '这篇笔记主要讲了什么？',
  },
  {
    label: '总结一下评论区里大家的观点',
    mode: 'SUMMARY_COMMENTS',
    question: '',
  },
]

const note = computed(() => detail.value?.note)
const author = computed(() => detail.value?.author)
const media = computed(() => detail.value?.media || [])
const noteCoverUrl = computed(() => media.value[0]?.url || '')
const liked = computed(() => !!detail.value?.liked)
const likeCount = computed(() => detail.value?.likeCount ?? note.value?.likeCount ?? 0)

const myUserId = ref(null)
/** 已拿到当前登录用户 id（或已确认未登录），避免在 fetchMe 返回前误判 !isOwner 而闪出关注/私信 */
const meResolved = ref(false)
let loadMeGen = 0

const defaultAvatar =
  'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

async function loadMe() {
  if (!auth.token) {
    loadMeGen += 1
    myUserId.value = null
    meResolved.value = true
    return
  }
  const gen = ++loadMeGen
  meResolved.value = false
  try {
    const { data } = await fetchMe()
    if (gen !== loadMeGen) return
    myUserId.value = data?.user?.id ?? null
  } catch {
    if (gen !== loadMeGen) return
    myUserId.value = null
  } finally {
    if (gen === loadMeGen) {
      meResolved.value = true
    }
  }
}

const isOwner = computed(() => {
  if (!auth.token || myUserId.value == null || !note.value?.userId) return false
  return Number(note.value.userId) === Number(myUserId.value)
})

async function refresh() {
  const { data } = await noteDetail(noteId.value)
  detail.value = data
  if (data && typeof data.collected === 'boolean') {
    collected.value = data.collected
  }
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
  aiMessages.value = []
  aiMsgSeq = 0
  aiComposerText.value = ''
  await refresh()
  await loadRelated()
  await loadComments()
  await loadFollowing()
  await loadProducts()
  await loadMe()
})

watch(
  () => auth.token,
  () => {
    loadMe()
  }
)

onMounted(async () => {
  try {
    await refresh()
    await loadRelated()
    await loadComments()
    await loadFollowing()
    await loadProducts()
    await loadMe()
  } catch {
    /* http 已提示 */
  }
})

async function scrollAiToBottom() {
  await nextTick()
  const el = aiChatScrollRef.value
  if (el) el.scrollTop = el.scrollHeight
}

watch(aiOpen, (open) => {
  if (!open) return
  if (aiMessages.value.length === 0) {
    aiMsgSeq += 1
    aiMessages.value.push({
      id: aiMsgSeq,
      role: 'assistant',
      content: '朋友你好呀，今天有什么想和我聊聊的吗？也可以试试下方的快捷问题～',
    })
  }
  scrollAiToBottom()
})

function editMyNote() {
  if (!note.value?.id) return
  router.push({ name: 'publish', query: { id: String(note.value.id) } })
}

async function deleteMyNote() {
  if (!note.value?.id) return
  try {
    await ElMessageBox.confirm('确定删除该笔记？删除后不可恢复。', '删除笔记', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteNote(note.value.id)
    ElMessage.success('已删除')
    router.replace({ name: 'me' })
  } catch (e) {
    if (e !== 'cancel') {
      /* */
    }
  }
}

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
  const peerId = author.value?.id
  if (peerId == null) return
  try {
    const { data: threadId } = await chatEnsureThread(peerId)
    if (threadId == null) {
      ElMessage.warning('无法打开会话')
      return
    }
    router.push({
      name: 'chat-thread',
      params: { threadId: String(threadId) },
      query: { peer: String(peerId) },
    })
  } catch {
    /* */
  }
}

function onReply({ parentId, replyToUserId: toUid, nick }) {
  replyParent.value = parentId
  replyToUserId.value = toUid
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

async function onDeleteComment(commentId) {
  if (!auth.token) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  try {
    await ElMessageBox.confirm('确定删除该评论？', '删除评论', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await commentDelete(commentId)
    ElMessage.success('已删除')
    await loadComments()
    await refresh()
  } catch (e) {
    if (e !== 'cancel') {
      /* */
    }
  }
}

function goProduct(p) {
  router.push({ name: 'product-detail', params: { id: p.id } })
}

function openProductActions(p) {
  menuProduct.value = p
  productMenuOpen.value = true
}

function menuGoDetail() {
  productMenuOpen.value = false
  if (menuProduct.value) goProduct(menuProduct.value)
}

async function menuAddCart() {
  if (!auth.token) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!menuProduct.value) return
  if (!isProductOnShelf(menuProduct.value)) {
    ElMessage.warning('已下架')
    return
  }
  if (isProductSoldOut(menuProduct.value)) {
    ElMessage.warning(PRODUCT_SOLD_OUT_MSG)
    return
  }
  try {
    await cartAdd(menuProduct.value.id, 1)
    ElMessage.success('已加入购物车')
    productMenuOpen.value = false
  } catch {
    /* */
  }
}

function menuDirectPay() {
  if (!auth.token) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  const p = menuProduct.value
  if (!isProductOnShelf(p)) {
    ElMessage.warning('已下架')
    return
  }
  if (isProductSoldOut(p)) {
    ElMessage.warning(PRODUCT_SOLD_OUT_MSG)
    return
  }
  productMenuOpen.value = false
  directPayProduct.value = p
  directPayOpen.value = true
}

function goAuthor() {
  if (!author.value?.id) return
  router.push({ name: 'user-profile', params: { userId: String(author.value.id) } })
}

function priceYuan(cent) {
  return ((cent || 0) / 100).toFixed(2)
}

function clearAiChat() {
  if (aiLoading.value) aiAbort?.abort()
  aiLoading.value = false
  aiMessages.value = []
  aiComposerText.value = ''
  aiMsgSeq = 0
  aiMsgSeq += 1
  aiMessages.value.push({
    id: aiMsgSeq,
    role: 'assistant',
    content: '对话已清空。还有什么想聊的吗？',
  })
  scrollAiToBottom()
}

function aiPlaceholderHint() {
  ElMessage.info('功能开发中，敬请期待')
}

async function runAiSession(mode, apiQuestion, userBubbleText) {
  if (!auth.token) {
    ElMessage.info('请先登录后使用 AI 助手')
    return
  }
  if (!note.value?.id) return
  if (aiLoading.value) return
  const q = (apiQuestion || '').trim()
  if (mode === 'QA' && !q) {
    ElMessage.warning('请输入问题')
    return
  }
  aiMsgSeq += 1
  aiMessages.value.push({ id: aiMsgSeq, role: 'user', content: userBubbleText })
  aiMsgSeq += 1
  const assistantId = aiMsgSeq
  aiMessages.value.push({
    id: assistantId,
    role: 'assistant',
    content: '',
    streaming: true,
  })
  await scrollAiToBottom()

  aiLoading.value = true
  aiAbort?.abort()
  aiAbort = new AbortController()
  try {
    await streamNoteAi({
      noteId: note.value.id,
      mode,
      question: mode === 'QA' ? q : '',
      token: auth.token,
      signal: aiAbort.signal,
      onAnswer: (t) => {
        const row = aiMessages.value.find((m) => m.id === assistantId)
        if (row) row.content += t
        scrollAiToBottom()
      },
    })
    const row = aiMessages.value.find((m) => m.id === assistantId)
    if (row && !row.content.trim()) {
      row.content = '暂无回复，请稍后再试。'
    }
  } catch (e) {
    if (e.name !== 'AbortError') {
      ElMessage.error(e.message || 'AI 请求失败')
      const row = aiMessages.value.find((m) => m.id === assistantId)
      if (row) row.content = row.content || '请求失败，请检查网络后重试。'
    }
  } finally {
    const row = aiMessages.value.find((m) => m.id === assistantId)
    if (row) row.streaming = false
    aiLoading.value = false
    scrollAiToBottom()
  }
}

function onAiSuggestion(item) {
  const label = item.label
  if (item.mode === 'QA') {
    runAiSession('QA', item.question || label, label)
  } else {
    runAiSession(item.mode, '', label)
  }
}

function submitAiComposer() {
  const t = aiComposerText.value.trim()
  if (!t) return
  aiComposerText.value = ''
  runAiSession('QA', t, t)
}

function quickSummaryPost() {
  runAiSession('SUMMARY_POST', '', '总结一下这篇笔记')
}

function quickSummaryComments() {
  runAiSession('SUMMARY_COMMENTS', '', '总结一下评论区')
}

function closeAi() {
  aiAbort?.abort()
  aiOpen.value = false
}
</script>

<template>
  <div v-if="note" class="page">
    <header class="bar sticky-bar">
      <el-button text circle title="返回" class="back-btn" @click="router.back()">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <div
        class="author-top"
        role="button"
        tabindex="0"
        @click="goAuthor"
        @keydown.enter="goAuthor"
      >
        <img :src="author?.icon || defaultAvatar" class="av-top" alt="" />
        <span class="name-top">{{ author?.nickName || '用户' }}</span>
      </div>
      <div class="bar-right">
        <template v-if="auth.token && author && author.id && meResolved && !isOwner">
          <el-button
            size="small"
            :type="authorFollowed ? 'default' : 'primary'"
            @click="toggleFollow"
          >
            {{ authorFollowed ? '已关注' : '关注' }}
          </el-button>
          <el-button size="small" :icon="ChatDotRound" @click="dmAuthor">私信</el-button>
        </template>
      </div>
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

      <div v-if="isOwner" class="owner-acts">
        <el-button plain type="primary" @click="editMyNote">编辑</el-button>
        <el-button plain type="danger" @click="deleteMyNote">删除</el-button>
      </div>

      <section v-if="products.length" class="sec">
        <h3>相似 / 关联商品</h3>
        <div class="prows">
          <div v-for="p in products" :key="p.id" class="pro" role="button" tabindex="0" @click="openProductActions(p)">
            <div class="pro-thumb">
              <img :src="p.cover || ''" alt="" />
              <div v-if="!isProductOnShelf(p)" class="off-mask">已下架</div>
              <div v-else-if="isProductSoldOut(p)" class="off-mask">{{ PRODUCT_SOLD_OUT_MSG }}</div>
            </div>
            <div class="pt">{{ p.title }}</div>
            <div class="pp">¥{{ priceYuan(p.priceCent) }}</div>
          </div>
        </div>
      </section>

      <el-dialog v-model="productMenuOpen" title="商品操作" width="88%" class="prod-menu-dlg">
        <div v-if="menuProduct" class="pm-body">
          <div class="pm-row">
            <img v-if="menuProduct.cover" :src="menuProduct.cover" alt="" class="pm-img" />
            <div>
              <div class="pm-t">{{ menuProduct.title }}</div>
              <div class="pm-p">¥{{ priceYuan(menuProduct.priceCent) }}</div>
              <div v-if="!isProductOnShelf(menuProduct)" class="pm-off">已下架</div>
              <div v-else-if="isProductSoldOut(menuProduct)" class="pm-off">{{ PRODUCT_SOLD_OUT_MSG }}</div>
            </div>
          </div>
          <el-button class="pm-btn" @click="menuGoDetail">查看详情</el-button>
          <el-button
            type="primary"
            plain
            class="pm-btn"
            :disabled="!isProductOnShelf(menuProduct) || isProductSoldOut(menuProduct)"
            @click="menuAddCart"
          >
            加入购物车
          </el-button>
          <el-button
            type="primary"
            class="pm-btn"
            :disabled="!isProductOnShelf(menuProduct) || isProductSoldOut(menuProduct)"
            @click="menuDirectPay"
          >
            立即支付
          </el-button>
        </div>
      </el-dialog>

      <DirectPayDialog
        v-model="directPayOpen"
        :product-id="directPayProduct?.id"
        :product-hint="directPayProduct"
        :fallback-seller-user-id="author?.id"
        off-shelf-msg="已下架"
        @paid="() => router.push({ name: 'orders' })"
      />

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
        <CommentBlock
          :nodes="comments"
          :my-user-id="myUserId"
          @reply="onReply"
          @delete="onDeleteComment"
        />
      </section>

      <section v-if="related.length" class="sec">
        <h3>相关笔记</h3>
        <div class="rel">
          <NoteCard v-for="(vo, i) in related" :key="(vo.note && vo.note.id) || i" :vo="vo" />
        </div>
      </section>
    </div>

    <div class="fab-ai" @click="aiOpen = true">AI</div>

    <el-drawer
      v-model="aiOpen"
      direction="btt"
      size="90%"
      :show-close="false"
      class="note-ai-drawer"
      append-to-body
      @close="closeAi"
    >
      <template #header>
        <div class="ai-dh">
          <button type="button" class="ai-dh-icon" aria-label="关闭" @click="closeAi">
            <el-icon :size="20"><Close /></el-icon>
          </button>
          <div class="ai-dh-title">
            <span class="ai-dh-dot" aria-hidden="true" />
            <span class="ai-dh-name">AI 助手</span>
          </div>
          <el-dropdown trigger="click" placement="bottom-end">
            <button type="button" class="ai-dh-icon" aria-label="更多">
              <el-icon :size="20"><MoreFilled /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :disabled="aiLoading" @click="quickSummaryPost">
                  总结帖子
                </el-dropdown-item>
                <el-dropdown-item :disabled="aiLoading" @click="quickSummaryComments">
                  总结评论
                </el-dropdown-item>
                <el-dropdown-item divided @click="clearAiChat">清空对话</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </template>

      <div class="ai-shell">
        <div ref="aiChatScrollRef" class="ai-scroll">
          <div v-if="note" class="ai-ctx-card">
            <div
              class="ai-ctx-cover"
              :style="noteCoverUrl ? { backgroundImage: `url(${noteCoverUrl})` } : {}"
            />
            <div class="ai-ctx-body">
              <div class="ai-ctx-title">{{ note.title || '笔记' }}</div>
              <div class="ai-ctx-meta">
                <img :src="author?.icon || defaultAvatar" class="ai-ctx-av" alt="" />
                <span>{{ author?.nickName || '用户' }}</span>
              </div>
            </div>
          </div>

          <div class="ai-thread">
            <div
              v-for="m in aiMessages"
              :key="m.id"
              class="ai-row"
              :class="m.role === 'user' ? 'ai-row-user' : 'ai-row-bot'"
            >
              <div
                class="ai-bubble"
                :class="m.role === 'user' ? 'ai-bubble-user' : 'ai-bubble-bot'"
              >
                <span v-if="m.role === 'assistant' && m.streaming && !m.content" class="ai-thinking">
                  思考中…
                </span>
                <span v-else class="ai-bubble-text">{{ m.content }}</span>
              </div>
            </div>
          </div>

          <div class="ai-suggest">
            <button
              v-for="(s, i) in AI_SUGGESTIONS"
              :key="i"
              type="button"
              class="ai-suggest-line"
              :disabled="aiLoading"
              @click="onAiSuggestion(s)"
            >
              <span class="ai-suggest-txt">{{ s.label }}</span>
              <span class="ai-suggest-go" aria-hidden="true">↗</span>
            </button>
          </div>
        </div>

        <footer class="ai-footer">
          <div class="ai-composer">
            <button
              type="button"
              class="ai-composer-side"
              aria-label="语音输入"
              @click="aiPlaceholderHint"
            >
              <el-icon :size="22"><Microphone /></el-icon>
            </button>
            <el-input
              v-model="aiComposerText"
              class="ai-composer-input"
              :disabled="aiLoading"
              placeholder="给 AI 助手发消息…"
              clearable
              @keyup.enter="submitAiComposer"
            >
              <template #suffix>
                <button
                  type="button"
                  class="ai-send-suffix"
                  aria-label="发送"
                  :disabled="aiLoading || !aiComposerText.trim()"
                  @click="submitAiComposer"
                >
                  <el-icon :size="18"><Promotion /></el-icon>
                </button>
              </template>
            </el-input>
            <button
              type="button"
              class="ai-composer-side"
              aria-label="拍照或传图"
              @click="aiPlaceholderHint"
            >
              <el-icon :size="22"><Camera /></el-icon>
            </button>
          </div>
          <p class="ai-disclaimer">内容由 AI 生成</p>
        </footer>
      </div>
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
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-bottom: 1px solid #eee;
}
.sticky-bar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.06);
}
.back-btn {
  margin-left: -4px;
  color: #333;
  flex-shrink: 0;
}
.author-top {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.av-top {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.name-top {
  font-weight: 600;
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bar-right {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
  max-width: 46%;
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
.acts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.owner-acts {
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
.pro-thumb {
  position: relative;
  width: 120px;
  height: 120px;
}
.pro img {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
  display: block;
}
.off-mask {
  position: absolute;
  inset: 0;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 4px;
}
.pt {
  font-size: 13px;
  margin-top: 4px;
}
.pp {
  color: #ff2442;
  font-weight: 600;
}
.pm-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.pm-row {
  display: flex;
  gap: 12px;
  margin-bottom: 6px;
}
.pm-img {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 8px;
}
.pm-t {
  font-weight: 600;
  font-size: 15px;
}
.pm-p {
  color: #ff2442;
  font-weight: 700;
  margin-top: 6px;
}
.pm-off {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
  font-weight: 600;
}
.pm-btn {
  width: 100%;
  margin-left: 0 !important;
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
.note-ai-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 12px 14px 10px;
  border-bottom: 1px solid #ebebeb;
}
.note-ai-drawer :deep(.el-drawer__body) {
  padding: 0;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}
.note-ai-drawer :deep(.el-drawer.btt) {
  border-radius: 16px 16px 0 0;
  overflow: hidden;
}

.ai-dh {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
}
.ai-dh-icon {
  border: none;
  background: transparent;
  padding: 6px;
  border-radius: 50%;
  color: #333;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}
.ai-dh-icon:hover {
  background: rgba(0, 0, 0, 0.06);
}
.ai-dh-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 16px;
  color: #1a1a1a;
}
.ai-dh-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: linear-gradient(145deg, #3ecf8e, #2bbd7e);
  flex-shrink: 0;
}
.ai-dh-name {
  letter-spacing: 0.02em;
}

.ai-shell {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  height: 100%;
}
.ai-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 14px 8px;
  -webkit-overflow-scrolling: touch;
}

.ai-ctx-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  margin-bottom: 16px;
}
.ai-ctx-cover {
  height: 96px;
  background: #e8e8e8 center / cover no-repeat;
}
.ai-ctx-body {
  padding: 12px 14px 14px;
}
.ai-ctx-title {
  font-size: 15px;
  font-weight: 600;
  color: #222;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.ai-ctx-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  font-size: 13px;
  color: #666;
}
.ai-ctx-av {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  object-fit: cover;
}

.ai-thread {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.ai-row {
  display: flex;
  width: 100%;
}
.ai-row-user {
  justify-content: flex-end;
}
.ai-row-bot {
  justify-content: flex-start;
}
.ai-bubble {
  max-width: 88%;
  padding: 11px 14px;
  font-size: 15px;
  line-height: 1.55;
  word-break: break-word;
}
.ai-bubble-text {
  white-space: pre-wrap;
}
.ai-bubble-user {
  background: #ebebeb;
  color: #222;
  border-radius: 18px 18px 6px 18px;
}
.ai-bubble-bot {
  background: #fff;
  color: #222;
  border-radius: 18px 18px 18px 6px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.ai-thinking {
  color: #999;
  font-size: 14px;
}

.ai-suggest {
  margin-top: 18px;
  padding-bottom: 8px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.ai-suggest-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
  text-align: left;
  font-size: 14px;
  color: #888;
  line-height: 1.45;
}
.ai-suggest-line:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.ai-suggest-txt {
  flex: 1;
}
.ai-suggest-go {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  border: 1px solid #d0d0d0;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #999;
  background: #fafafa;
}

.ai-footer {
  flex-shrink: 0;
  padding: 10px 14px 14px;
  padding-bottom: calc(14px + env(safe-area-inset-bottom, 0px));
  background: #f5f5f5;
  border-top: 1px solid #ebebeb;
}
.ai-composer {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ai-composer-side {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: #fff;
  color: #444;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ai-composer-side:hover {
  color: #111;
}
.ai-composer-input {
  flex: 1;
  min-width: 0;
}
.ai-composer-input :deep(.el-input__wrapper) {
  border-radius: 22px;
  padding-left: 16px;
  padding-right: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.ai-send-suffix {
  border: none;
  background: transparent;
  padding: 4px 6px;
  margin-right: 2px;
  border-radius: 8px;
  color: #ff2442;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}
.ai-send-suffix:disabled {
  color: #ccc;
  cursor: not-allowed;
}
.ai-send-suffix:not(:disabled):hover {
  background: rgba(255, 36, 66, 0.08);
}
.ai-disclaimer {
  margin: 8px 0 0;
  text-align: center;
  font-size: 11px;
  color: #bbb;
  letter-spacing: 0.02em;
}
.loading {
  text-align: center;
  padding: 40px;
  color: #999;
}
</style>
