<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NoteMasonry from '../components/NoteMasonry.vue'
import FollowFeedView from './FollowFeedView.vue'
import { noteRecommend } from '../api'

const route = useRoute()
const router = useRouter()

const subTab = ref('recommend')

const list = ref([])
const current = ref(1)
const loading = ref(false)
const finished = ref(false)

function syncTabFromRoute() {
  subTab.value = route.query.tab === 'follow' ? 'follow' : 'recommend'
}

watch(
  () => route.query.tab,
  () => syncTabFromRoute(),
  { immediate: true },
)

function setSubTab(t) {
  subTab.value = t
  router.replace({
    name: 'discover',
    query: t === 'follow' ? { tab: 'follow' } : {},
  })
}

async function loadRecommend() {
  if (loading.value || finished.value) return
  loading.value = true
  try {
    const { data } = await noteRecommend({ current: current.value, size: 10 })
    list.value = list.value.concat(data || [])
    if (!data || data.length < 10) finished.value = true
    else current.value += 1
  } catch {
    finished.value = true
  } finally {
    loading.value = false
  }
}

function onScroll() {
  if (subTab.value !== 'recommend') return
  const doc = document.documentElement
  if (doc.scrollHeight - doc.scrollTop - doc.clientHeight < 120) {
    loadRecommend()
  }
}

watch(subTab, (t) => {
  if (t === 'recommend' && list.value.length === 0 && !finished.value) {
    loadRecommend()
  }
})

onMounted(() => {
  if (subTab.value === 'recommend') loadRecommend()
  window.addEventListener('scroll', onScroll, { passive: true })
})
onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<template>
  <div class="discover">
    <div class="seg">
      <button
        type="button"
        class="seg-item"
        :class="{ on: subTab === 'follow' }"
        @click="setSubTab('follow')"
      >
        关注
      </button>
      <button
        type="button"
        class="seg-item"
        :class="{ on: subTab === 'recommend' }"
        @click="setSubTab('recommend')"
      >
        推荐
      </button>
    </div>

    <FollowFeedView v-if="subTab === 'follow'" embedded />

    <template v-else>
      <NoteMasonry :list="list" />
      <div v-if="loading" class="more">加载中…</div>
      <div v-else-if="finished && list.length === 0" class="more">暂无笔记</div>
      <div v-else-if="finished" class="more">没有更多了</div>
    </template>
  </div>
</template>

<style scoped>
.seg {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 28px;
  margin: 0 0 16px;
  padding: 4px 0 8px;
  border-bottom: 1px solid #eee;
  background: #f5f5f5;
  margin-left: -10px;
  margin-right: -10px;
  padding-left: 10px;
  padding-right: 10px;
}
.seg-item {
  border: none;
  background: none;
  font-size: 16px;
  font-weight: 500;
  color: #999;
  padding: 6px 4px;
  cursor: pointer;
  position: relative;
}
.seg-item.on {
  color: #111;
  font-weight: 700;
}
.seg-item.on::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  width: 22px;
  height: 3px;
  border-radius: 2px;
  background: #ff2442;
}
.more {
  text-align: center;
  color: #999;
  padding: 16px;
  font-size: 13px;
}
</style>
