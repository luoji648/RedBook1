<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Star, User, ChatLineRound } from '@element-plus/icons-vue'
import { chatThreads, fetchMe } from '../api'

const router = useRouter()

const threads = ref([])
const myId = ref(null)

async function loadThreads() {
  try {
    const { data: me } = await fetchMe()
    myId.value = me?.user?.id
    const { data } = await chatThreads({ current: 1, size: 50 })
    threads.value = data || []
  } catch {
    threads.value = []
  }
}

function goNotice(slug) {
  router.push({ name: 'notice-category', params: { slug } })
}

function peer(t) {
  if (!myId.value) return null
  return t.userLow === myId.value ? t.userHigh : t.userLow
}

function openThread(t) {
  const p = peer(t)
  router.push({
    name: 'chat-thread',
    params: { threadId: t.id },
    query: p != null ? { peer: String(p) } : {},
  })
}

onMounted(() => {
  loadThreads()
})
</script>

<template>
  <div class="wrap">
    <h2 class="page-title">消息</h2>

    <div class="notice-btns">
      <button type="button" class="nb" @click="goNotice('like-collect')">
        <el-icon :size="28" class="nb-icon"><Star /></el-icon>
        <span class="nb-text">赞和收藏</span>
      </button>
      <button type="button" class="nb" @click="goNotice('follow')">
        <el-icon :size="28" class="nb-icon"><User /></el-icon>
        <span class="nb-text">新增关注</span>
      </button>
      <button type="button" class="nb" @click="goNotice('comment')">
        <el-icon :size="28" class="nb-icon"><ChatLineRound /></el-icon>
        <span class="nb-text">评论和@</span>
      </button>
    </div>

    <div class="dm-head">私信</div>
    <div class="dm-list">
      <div v-if="!threads.length" class="empty dm-empty">暂无私信，在笔记详情可向作者发私信</div>
      <div
        v-for="t in threads"
        :key="t.id"
        class="row"
        @click="openThread(t)"
      >
        <div class="av ph">{{ peer(t) }}</div>
        <div class="info">
          <div class="name">用户 {{ peer(t) }}</div>
          <div class="time">{{ t.lastMsgTime || '' }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}
.page-title {
  margin: 0 0 14px;
  font-size: 18px;
  font-weight: 600;
}
.notice-btns {
  display: flex;
  justify-content: space-around;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}
.nb {
  flex: 1;
  max-width: 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  padding: 12px 8px 10px;
  border: none;
  border-radius: 12px;
  background: #fafafa;
  cursor: pointer;
  color: #333;
}
.nb:active {
  opacity: 0.85;
}
.nb-icon {
  color: #ff2442;
}
.nb-text {
  font-size: 12px;
  line-height: 1.2;
  text-align: center;
  color: #333;
}
.empty {
  color: #999;
  padding: 20px;
  text-align: center;
  font-size: 14px;
}
.dm-empty {
  padding: 16px;
}
.dm-head {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}
.row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
}
.row .av.ph {
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
.row .info {
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
