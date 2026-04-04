<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { chatThreads, fetchMe } from '../api'

const router = useRouter()
const threads = ref([])
const myId = ref(null)

async function load() {
  try {
    const { data: me } = await fetchMe()
    myId.value = me?.user?.id
    const { data } = await chatThreads({ current: 1, size: 50 })
    threads.value = data || []
  } catch {
    threads.value = []
  }
}

function peer(t) {
  if (!myId.value) return null
  return t.userLow === myId.value ? t.userHigh : t.userLow
}

function open(t) {
  const p = peer(t)
  router.push({
    name: 'chat-thread',
    params: { threadId: t.id },
    query: p != null ? { peer: String(p) } : {},
  })
}

onMounted(load)
</script>

<template>
  <div class="chat">
    <h2>私信</h2>
    <div v-if="!threads.length" class="empty">暂无私信，在笔记详情可向作者发私信</div>
    <div
      v-for="t in threads"
      :key="t.id"
      class="row"
      @click="open(t)"
    >
      <div class="av">{{ peer(t) }}</div>
      <div class="info">
        <div class="name">用户 {{ peer(t) }}</div>
        <div class="time">{{ t.lastMsgTime || '' }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}
h2 {
  margin: 0 0 12px;
}
.empty {
  color: #999;
  padding: 24px;
  text-align: center;
}
.row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
}
.av {
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
.info {
  flex: 1;
}
.name {
  font-weight: 600;
}
.time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
