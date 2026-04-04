<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import NoteMasonry from '../components/NoteMasonry.vue'
import { noteRecommend } from '../api'

const list = ref([])
const current = ref(1)
const total = ref(0)
const loading = ref(false)
const finished = ref(false)

async function load() {
  if (loading.value || finished.value) return
  loading.value = true
  try {
    const { data, total: t } = await noteRecommend({ current: current.value, size: 10 })
    list.value = list.value.concat(data || [])
    total.value = t || 0
    if (!data || data.length < 10) finished.value = true
    else current.value += 1
  } catch {
    finished.value = true
  } finally {
    loading.value = false
  }
}

function onScroll() {
  const doc = document.documentElement
  if (doc.scrollHeight - doc.scrollTop - doc.clientHeight < 120) {
    load()
  }
}

onMounted(() => {
  load()
  window.addEventListener('scroll', onScroll, { passive: true })
})
onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<template>
  <div class="discover">
    <h2 class="h">推荐</h2>
    <NoteMasonry :list="list" />
    <div v-if="loading" class="more">加载中…</div>
    <div v-else-if="finished && list.length === 0" class="more">暂无笔记</div>
    <div v-else-if="finished" class="more">没有更多了</div>
  </div>
</template>

<style scoped>
.h {
  font-size: 18px;
  margin: 0 0 12px;
  font-weight: 600;
}
.more {
  text-align: center;
  color: #999;
  padding: 16px;
  font-size: 13px;
}
</style>
