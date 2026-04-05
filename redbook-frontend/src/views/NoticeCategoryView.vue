<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { noticeInteractions } from '../api'

const route = useRoute()
const router = useRouter()

const apiCategory = computed(() => {
  const slug = String(route.params.slug || '')
  if (slug === 'like-collect') return 'like_collect'
  if (slug === 'follow') return 'follow'
  if (slug === 'comment') return 'comment'
  return 'like_collect'
})

const emptyHint = computed(() => {
  const c = apiCategory.value
  if (c === 'like_collect') return '暂无赞或收藏通知'
  if (c === 'follow') return '暂无新关注'
  return '暂无评论或转发通知'
})

const interactions = ref([])
const intTotal = ref(0)
const intPage = ref(1)

async function loadInteractions(reset) {
  if (reset) {
    intPage.value = 1
    interactions.value = []
    intTotal.value = 0
  }
  try {
    const { data, total } = await noticeInteractions({
      current: intPage.value,
      size: 20,
      category: apiCategory.value,
    })
    const rows = data || []
    interactions.value = reset ? rows : interactions.value.concat(rows)
    intTotal.value = total ?? 0
  } catch {
    if (reset) interactions.value = []
  }
}

function typeLabel(type) {
  if (type === 'LIKE') return '赞了你的笔记'
  if (type === 'COLLECT') return '收藏了你的笔记'
  if (type === 'COMMENT') return '评论了你的笔记'
  if (type === 'SHARE') return '转发了你的笔记'
  if (type === 'FOLLOW') return '关注了你'
  return type || '通知'
}

function openInteraction(row) {
  if (row.type === 'FOLLOW' && row.actor?.id != null) {
    router.push({ name: 'user-profile', params: { userId: String(row.actor.id) } })
    return
  }
  if (row.noteId) {
    router.push({ name: 'note-detail', params: { id: String(row.noteId) } })
  }
}

function moreInteractions() {
  if (interactions.value.length >= intTotal.value) return
  intPage.value += 1
  loadInteractions(false)
}

watch(apiCategory, () => {
  loadInteractions(true)
})

onMounted(() => {
  loadInteractions(true)
})
</script>

<template>
  <div class="wrap">
    <div v-if="!interactions.length" class="empty">{{ emptyHint }}</div>
    <div
      v-for="(row, i) in interactions"
      :key="i"
      class="irow"
      @click="openInteraction(row)"
    >
      <img
        class="av"
        :src="row.actor?.icon || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
        alt=""
      />
      <div class="info">
        <div class="line1">
          <span class="nick">{{ row.actor?.nickName || '用户' }}</span>
          <span class="txt">{{ typeLabel(row.type) }}</span>
        </div>
        <div v-if="row.noteTitle" class="note-title">「{{ row.noteTitle }}」</div>
        <div class="time">{{ row.eventTime || '' }}</div>
      </div>
    </div>
    <el-button
      v-show="interactions.length > 0 && interactions.length < intTotal"
      class="more"
      text
      type="primary"
      @click="moreInteractions"
    >
      加载更多
    </el-button>
  </div>
</template>

<style scoped>
.wrap {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  min-height: 200px;
}
.empty {
  color: #999;
  padding: 40px 20px;
  text-align: center;
  font-size: 14px;
}
.irow {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
}
.irow .av {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.irow .info {
  flex: 1;
  min-width: 0;
}
.line1 {
  font-size: 14px;
  line-height: 1.4;
}
.nick {
  font-weight: 600;
  margin-right: 6px;
}
.txt {
  color: #333;
}
.note-title {
  font-size: 13px;
  color: #666;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
.more {
  width: 100%;
  margin-top: 8px;
}
</style>
