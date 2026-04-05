<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ChatDotRound, Share, StarFilled } from '@element-plus/icons-vue'
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
  chatSend,
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
            <img :src="p.cover || ''" alt="" />
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
            </div>
          </div>
          <el-button class="pm-btn" @click="menuGoDetail">查看详情</el-button>
          <el-button type="primary" plain class="pm-btn" @click="menuAddCart">加入购物车</el-button>
          <el-button type="primary" class="pm-btn" @click="menuDirectPay">立即支付</el-button>
        </div>
      </el-dialog>

      <DirectPayDialog
        v-model="directPayOpen"
        :product-id="directPayProduct?.id"
        :product-hint="directPayProduct"
        :fallback-seller-user-id="author?.id"
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
