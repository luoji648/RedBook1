<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { followFollowingOfUser, followFollowersOfUser } from '../api'

const route = useRoute()
const router = useRouter()

const userId = computed(() => String(route.params.userId || ''))
const isFollowingList = computed(() => route.name === 'user-following')

const list = ref([])
const total = ref(0)
const current = ref(1)
const loading = ref(false)
const loadError = ref('')

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const pageTitle = computed(() => (isFollowingList.value ? '关注' : '粉丝'))

async function load(reset) {
  if (!userId.value) return
  if (reset) {
    current.value = 1
    list.value = []
    total.value = 0
    loadError.value = ''
  }
  loading.value = true
  try {
    const api = isFollowingList.value ? followFollowingOfUser : followFollowersOfUser
    const { data, total: t } = await api(userId.value, {
      current: current.value,
      size: 50,
    })
    list.value = reset ? data || [] : list.value.concat(data || [])
    total.value = t ?? 0
  } catch (e) {
    loadError.value = e?.message || '加载失败'
    if (reset) {
      list.value = []
      total.value = 0
    }
  } finally {
    loading.value = false
  }
}

function more() {
  if (list.value.length >= total.value || loading.value) return
  current.value += 1
  load(false)
}

function goUserProfile(u) {
  if (!u?.id) return
  router.push({ name: 'user-profile', params: { userId: String(u.id) } })
}

watch(
  () => [route.name, route.params.userId],
  () => {
    load(true)
  },
)

onMounted(() => {
  load(true)
})
</script>

<template>
  <div class="page">
    <header class="bar">
      <el-button text circle title="返回" class="back-btn" @click="router.back()">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <h1 class="title">{{ pageTitle }}</h1>
    </header>

    <div v-if="loadError" class="hint">{{ loadError }}</div>
    <div v-else-if="loading && !list.length" class="empty">加载中…</div>
    <div v-else-if="!list.length" class="empty">暂无{{ pageTitle }}</div>
    <div v-else class="ulist">
      <div v-for="u in list" :key="u.id" class="urow">
        <div class="uinfo">
          <img
            :src="u.icon || defaultAvatar"
            class="uav"
            alt=""
            @click="goUserProfile(u)"
          />
          <span class="uname" @click="goUserProfile(u)">{{ u.nickName || '用户' }}</span>
        </div>
      </div>
    </div>

    <el-button
      v-if="!loadError && list.length < total"
      class="more"
      text
      type="primary"
      :loading="loading"
      @click="more"
    >
      加载更多
    </el-button>
  </div>
</template>

<style scoped>
.page {
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  min-height: 50vh;
}
.bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.back-btn {
  margin-left: -8px;
  color: #333;
  flex-shrink: 0;
}
.title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
}
.hint {
  text-align: center;
  color: #999;
  padding: 32px 12px;
  font-size: 14px;
}
.empty {
  text-align: center;
  color: #999;
  padding: 40px 12px;
  font-size: 14px;
}
.ulist {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.urow {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}
.uinfo {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.uav {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  cursor: pointer;
}
.uname {
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}
.more {
  width: 100%;
  margin-top: 12px;
}
</style>
