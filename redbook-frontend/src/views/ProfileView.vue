<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Setting } from '@element-plus/icons-vue'
import {
  fetchMe,
  noteMy,
  collectMy,
  likeMy,
  followFollowing,
  followFollowers,
  signIn,
  signCount,
} from '../api'

const router = useRouter()

const me = ref(null)
const tab = ref('notes')
const list = ref([])
const total = ref(0)
const current = ref(1)
const streak = ref(0)

async function loadMe() {
  try {
    const { data } = await fetchMe()
    me.value = data
  } catch {
    me.value = null
  }
}

async function loadStreak() {
  try {
    const { data } = await signCount()
    streak.value = data ?? 0
  } catch {
    streak.value = 0
  }
}

async function loadTab() {
  current.value = 1
  list.value = []
  total.value = 0
  try {
    if (tab.value === 'notes') {
      const { data, total: t } = await noteMy({ current: 1, size: 20 })
      list.value = data || []
      total.value = t || 0
    } else if (tab.value === 'collect') {
      const { data, total: t } = await collectMy({ current: 1, size: 20 })
      list.value = data || []
      total.value = t || 0
    } else if (tab.value === 'like') {
      const { data, total: t } = await likeMy({ current: 1, size: 20 })
      list.value = data || []
      total.value = t || 0
    } else if (tab.value === 'following') {
      const { data, total: t } = await followFollowing({ current: 1, size: 50 })
      list.value = data || []
      total.value = t || 0
    } else if (tab.value === 'followers') {
      const { data, total: t } = await followFollowers({ current: 1, size: 50 })
      list.value = data || []
      total.value = t || 0
    }
  } catch {
    list.value = []
  }
}

async function more() {
  if (tab.value !== 'notes' && tab.value !== 'collect' && tab.value !== 'like') return
  current.value += 1
  try {
    if (tab.value === 'notes') {
      const { data } = await noteMy({ current: current.value, size: 20 })
      list.value = list.value.concat(data || [])
    } else if (tab.value === 'collect') {
      const { data } = await collectMy({ current: current.value, size: 20 })
      list.value = list.value.concat(data || [])
    } else if (tab.value === 'like') {
      const { data } = await likeMy({ current: current.value, size: 20 })
      list.value = list.value.concat(data || [])
    }
  } catch {
    /* */
  }
}

async function doSign() {
  try {
    await signIn()
    ElMessage.success('签到成功')
    loadStreak()
  } catch {
    /* */
  }
}

function goNote(n) {
  router.push({ name: 'note-detail', params: { id: n.id } })
}

function goSettings() {
  router.push({ name: 'settings' })
}

onMounted(async () => {
  await loadMe()
  await loadStreak()
  await loadTab()
})
</script>

<template>
  <div v-if="me" class="profile">
    <div class="head">
      <img
        :src="me.user?.icon || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
        class="av"
        alt=""
      />
      <div class="txt">
        <div class="name">{{ me.user?.nickName || '用户' }}</div>
        <div class="sub">连续签到 {{ streak }} 天 · 粉丝 {{ me.userInfo?.fans ?? 0 }} · 关注 {{ me.userInfo?.followee ?? 0 }}</div>
      </div>
      <el-button :icon="Setting" circle @click="goSettings" />
    </div>
    <div class="row">
      <el-button type="primary" plain size="small" @click="doSign">每日签到</el-button>
      <el-button size="small" @click="router.push({ name: 'orders' })">我的订单</el-button>
    </div>

    <el-tabs v-model="tab" @tab-change="loadTab">
      <el-tab-pane label="笔记" name="notes" />
      <el-tab-pane label="收藏" name="collect" />
      <el-tab-pane label="点赞" name="like" />
      <el-tab-pane label="关注" name="following" />
      <el-tab-pane label="粉丝" name="followers" />
    </el-tabs>

    <div v-if="tab === 'notes' || tab === 'collect' || tab === 'like'" class="grid">
      <div
        v-for="n in list"
        :key="n.id"
        class="cell"
        @click="goNote(n)"
      >
        <div class="cover">{{ (n.title || n.content || '笔记').slice(0, 20) }}</div>
        <div class="t">{{ n.title || '无标题' }}</div>
      </div>
    </div>

    <div v-else class="ulist">
      <div v-for="u in list" :key="u.id" class="urow">
        <span>{{ u.nickName || '用户' }}</span>
        <span class="muted">ID {{ u.id }}</span>
      </div>
    </div>

    <el-button
      v-if="tab === 'notes' || tab === 'collect' || tab === 'like'"
      v-show="list.length < total"
      class="more"
      text
      type="primary"
      @click="more"
    >
      加载更多
    </el-button>
  </div>
</template>

<style scoped>
.profile {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}
.head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.av {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
}
.txt {
  flex: 1;
}
.name {
  font-weight: 700;
  font-size: 18px;
}
.sub {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
.row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.cell {
  background: #fafafa;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
}
.cover {
  height: 100px;
  background: linear-gradient(135deg, #ffeaa7, #fab1a0);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #fff;
  padding: 8px;
  text-align: center;
}
.t {
  padding: 8px;
  font-size: 13px;
}
.ulist {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.urow {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}
.muted {
  color: #999;
  font-size: 12px;
}
.more {
  width: 100%;
  margin-top: 12px;
}
</style>
