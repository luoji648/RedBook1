<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import NoteMasonry from '../components/NoteMasonry.vue'
import { noteFeed } from '../api'

const list = ref([])
const maxTime = ref(null)
const offset = ref(0)
const loading = ref(false)
const empty = ref(false)
const nomore = ref(false)

async function load() {
  if (loading.value || nomore.value) return
  loading.value = true
  try {
    const params = { size: 10 }
    if (maxTime.value != null) {
      params.maxTime = maxTime.value
      params.offset = offset.value
    }
    const { data } = await noteFeed(params)
    const sr = data || {}
    const chunk = sr.list || []
    if (chunk.length === 0 && list.value.length === 0) empty.value = true
    if (chunk.length === 0) nomore.value = true
    list.value = list.value.concat(chunk)
    maxTime.value = sr.minTime
    offset.value = sr.offset ?? 0
  } catch {
    /* */
  } finally {
    loading.value = false
  }
}

function onScroll() {
  const doc = document.documentElement
  if (doc.scrollHeight - doc.scrollTop - doc.clientHeight < 160) {
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
  <div class="feed">
    <h2 class="h">关注动态</h2>
    <p v-if="empty" class="empty">还没有动态，去发现页逛逛或关注作者吧</p>
    <NoteMasonry v-else :list="list" />
    <div v-if="loading" class="more">加载中…</div>
    <div v-else-if="nomore && list.length" class="more">没有更多了</div>
  </div>
</template>

<style scoped>
.h {
  font-size: 18px;
  margin: 0 0 12px;
  font-weight: 600;
}
.empty {
  color: #999;
  font-size: 14px;
  line-height: 1.6;
}
.more {
  text-align: center;
  color: #999;
  padding: 16px;
}
</style>
