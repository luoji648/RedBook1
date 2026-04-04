<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  vo: { type: Object, required: true },
})

const router = useRouter()

const note = computed(() => props.vo.note || {})
const cover = computed(() => {
  const m = props.vo.media
  if (m && m.length) return m[0].url
  return ''
})
const title = computed(() => note.value.title || (note.value.content || '').slice(0, 40) || '笔记')
const likeCount = computed(() => props.vo.likeCount ?? note.value.likeCount ?? 0)

function go() {
  router.push({ name: 'note-detail', params: { id: note.value.id } })
}
</script>

<template>
  <div class="card" @click="go">
    <div class="thumb">
      <img v-if="cover" :src="cover" alt="" />
      <div v-else class="ph">{{ title.slice(0, 1) }}</div>
      <span v-if="note.type === 1" class="badge">视频</span>
    </div>
    <div class="meta">
      <div class="t">{{ title }}</div>
      <div class="foot">
        <span class="likes">♥ {{ likeCount }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card {
  break-inside: avoid;
  margin-bottom: 12px;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  cursor: pointer;
}
.thumb {
  position: relative;
  background: #f2f2f2;
  aspect-ratio: 3/4;
}
.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.ph {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: #ccc;
}
.badge {
  position: absolute;
  right: 8px;
  top: 8px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
}
.meta {
  padding: 8px 10px 10px;
}
.t {
  font-size: 14px;
  line-height: 1.35;
  color: #333;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.foot {
  margin-top: 6px;
  font-size: 12px;
  color: #999;
}
</style>
