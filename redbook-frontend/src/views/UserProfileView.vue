<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ArrowLeft } from '@element-plus/icons-vue'
import { userPublic, noteByUser, followUser, unfollowUser, collectByUser, likeByUser } from '../api'
import NoteMasonry from '../components/NoteMasonry.vue'
import GroupTabPanel from '../components/GroupTabPanel.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const userId = computed(() => route.params.userId)

const profile = ref(null)
const list = ref([])
const total = ref(0)
const page = ref(1)
const collectList = ref([])
const collectTotal = ref(0)
const collectPage = ref(1)
const likeList = ref([])
const likeTotal = ref(0)
const likePage = ref(1)
const tab = ref(route.query.tab === 'groups' ? 'groups' : 'notes')

const user = computed(() => profile.value?.user)
const userInfo = computed(() => profile.value?.userInfo)
const followedByMe = computed(() => !!profile.value?.followedByMe)
const isSelf = computed(() => !!profile.value?.isSelf)
const canViewCollect = computed(
  () =>
    isSelf.value ||
    userInfo.value?.collectPublic === true ||
    userInfo.value?.collectPublic === 1,
)
const canViewLike = computed(
  () =>
    isSelf.value ||
    userInfo.value?.likePublic === true ||
    userInfo.value?.likePublic === 1,
)

async function loadProfile() {
  try {
    const { data } = await userPublic(userId.value)
    profile.value = data
  } catch {
    profile.value = null
  }
}

watch(
  () => route.query.tab,
  (q) => {
    if (q === 'groups') tab.value = 'groups'
  },
)

async function loadNotes(reset) {
  if (tab.value !== 'notes') return
  if (reset) {
    page.value = 1
    list.value = []
    total.value = 0
  }
  try {
    const { data, total: t } = await noteByUser(userId.value, {
      current: page.value,
      size: 20,
    })
    list.value = reset ? data || [] : list.value.concat(data || [])
    total.value = t ?? 0
  } catch {
    if (reset) list.value = []
  }
}

async function loadCollect(reset) {
  if (tab.value !== 'collect' || !canViewCollect.value) return
  if (reset) {
    collectPage.value = 1
    collectList.value = []
    collectTotal.value = 0
  }
  try {
    const { data, total: t } = await collectByUser(userId.value, {
      current: collectPage.value,
      size: 20,
    })
    collectList.value = reset ? data || [] : collectList.value.concat(data || [])
    collectTotal.value = t ?? 0
  } catch {
    if (reset) collectList.value = []
  }
}

async function loadLike(reset) {
  if (tab.value !== 'like' || !canViewLike.value) return
  if (reset) {
    likePage.value = 1
    likeList.value = []
    likeTotal.value = 0
  }
  try {
    const { data, total: t } = await likeByUser(userId.value, {
      current: likePage.value,
      size: 20,
    })
    likeList.value = reset ? data || [] : likeList.value.concat(data || [])
    likeTotal.value = t ?? 0
  } catch {
    if (reset) likeList.value = []
  }
}

function more() {
  if (list.value.length >= total.value) return
  page.value += 1
  loadNotes(false)
}

function moreCollect() {
  if (collectList.value.length >= collectTotal.value) return
  collectPage.value += 1
  loadCollect(false)
}

function moreLike() {
  if (likeList.value.length >= likeTotal.value) return
  likePage.value += 1
  loadLike(false)
}

async function toggleFollow() {
  if (!auth.token) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!user.value?.id || isSelf.value) return
  try {
    if (followedByMe.value) {
      await unfollowUser(user.value.id)
      profile.value.followedByMe = false
    } else {
      await followUser(user.value.id)
      profile.value.followedByMe = true
    }
  } catch {
    /* */
  }
}

function goMe() {
  router.push({ name: 'me' })
}

function goFollowingList() {
  router.push({ name: 'user-following', params: { userId: String(userId.value) } })
}

function goFollowersList() {
  router.push({ name: 'user-followers', params: { userId: String(userId.value) } })
}

watch(userId, async () => {
  await loadProfile()
  await loadNotes(true)
  collectList.value = []
  likeList.value = []
  collectPage.value = 1
  likePage.value = 1
  if (tab.value === 'collect') await loadCollect(true)
  if (tab.value === 'like') await loadLike(true)
})

watch(tab, (v) => {
  if (v === 'notes') loadNotes(true)
  if (v === 'collect') loadCollect(true)
  if (v === 'like') loadLike(true)
})

onMounted(async () => {
  await loadProfile()
  await loadNotes(true)
})
</script>

<template>
  <div v-if="user" class="page">
    <header class="bar">
      <el-button text circle title="返回" class="back-btn" @click="router.back()">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
    </header>

    <div class="head">
      <img
        :src="user.icon || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
        class="av"
        alt=""
      />
      <div class="txt">
        <div class="name">{{ user.nickName || '用户' }}</div>
        <div class="sub">
          {{ userInfo?.introduce || '暂无简介' }}
        </div>
        <div class="stats">
          <span class="stat-link" role="button" tabindex="0" @click="goFollowersList">{{ userInfo?.fans ?? 0 }} 粉丝</span>
          <span class="stat-link" role="button" tabindex="0" @click="goFollowingList">{{ userInfo?.followee ?? 0 }} 关注</span>
        </div>
      </div>
    </div>

    <div v-if="!isSelf" class="actions">
      <el-button
        v-if="auth.token"
        :type="followedByMe ? 'default' : 'primary'"
        size="small"
        @click="toggleFollow"
      >
        {{ followedByMe ? '已关注' : '关注' }}
      </el-button>
      <el-button v-else size="small" type="primary" @click="router.push({ name: 'login', query: { redirect: route.fullPath } })">
        登录后关注
      </el-button>
    </div>
    <div v-else class="actions">
      <el-button size="small" @click="goMe">前往「我」管理笔记</el-button>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="笔记" name="notes" />
      <el-tab-pane label="收藏" name="collect" />
      <el-tab-pane label="赞过" name="like" />
      <el-tab-pane label="群聊" name="groups" />
    </el-tabs>

    <template v-if="tab === 'notes'">
      <div v-if="!list.length" class="empty">暂无公开笔记</div>
      <NoteMasonry v-else :list="list" />
      <el-button
        v-show="list.length < total"
        class="more"
        text
        type="primary"
        @click="more"
      >
        加载更多
      </el-button>
    </template>
    <template v-else-if="tab === 'collect'">
      <div v-if="!canViewCollect" class="hint">该用户未公开收藏列表。</div>
      <template v-else>
        <div v-if="!collectList.length" class="empty">暂无收藏</div>
        <NoteMasonry v-else :list="collectList" />
        <el-button
          v-show="collectList.length < collectTotal"
          class="more"
          text
          type="primary"
          @click="moreCollect"
        >
          加载更多
        </el-button>
      </template>
    </template>
    <template v-else-if="tab === 'like'">
      <div v-if="!canViewLike" class="hint">该用户未公开赞过列表。</div>
      <template v-else>
        <div v-if="!likeList.length" class="empty">暂无赞过</div>
        <NoteMasonry v-else :list="likeList" />
        <el-button
          v-show="likeList.length < likeTotal"
          class="more"
          text
          type="primary"
          @click="moreLike"
        >
          加载更多
        </el-button>
      </template>
    </template>
    <template v-else-if="tab === 'groups'">
      <GroupTabPanel :owner-user-id="userId" />
    </template>
  </div>
  <div v-else class="loading">加载中…</div>
</template>

<style scoped>
.page {
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  min-height: 60vh;
}
.bar {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}
.back-btn {
  margin-left: -8px;
  color: #333;
}
.head {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  margin-bottom: 12px;
}
.av {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
}
.txt {
  flex: 1;
  min-width: 0;
}
.name {
  font-weight: 700;
  font-size: 18px;
}
.sub {
  font-size: 13px;
  color: #666;
  margin-top: 6px;
  line-height: 1.4;
}
.stats {
  margin-top: 8px;
  font-size: 13px;
  color: #999;
  display: flex;
  gap: 16px;
}
.stat-link {
  cursor: pointer;
}
.stat-link:hover {
  color: #409eff;
}
.actions {
  margin-bottom: 12px;
}
.empty {
  text-align: center;
  color: #999;
  padding: 32px;
}
.more {
  width: 100%;
  margin-top: 8px;
}
.hint {
  font-size: 14px;
  color: #666;
  padding: 24px 8px;
  line-height: 1.6;
}
.loading {
  text-align: center;
  padding: 40px;
  color: #999;
}
</style>
