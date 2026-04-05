<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProductFootprints } from '../utils/productFootprint'

const router = useRouter()
const list = ref([])

onMounted(() => {
  list.value = getProductFootprints()
})

function yuan(cent) {
  return ((cent || 0) / 100).toFixed(2)
}

function go(p) {
  router.push({ name: 'product-detail', params: { id: String(p.id) } })
}
</script>

<template>
  <div class="page">
    <header class="bar">
      <span class="tit">商品足迹</span>
    </header>
    <p v-if="list.length === 0" class="empty">暂无浏览记录，去集市逛逛吧</p>
    <div v-else class="grid">
      <div v-for="p in list" :key="p.id" class="card" @click="go(p)">
        <div class="cover-wrap">
          <img v-if="p.cover" :src="p.cover" class="cover" alt="" />
          <div v-else class="cover ph" />
        </div>
        <div class="meta">
          <div class="t">{{ p.title || '商品' }}</div>
          <div v-if="p.priceCent != null" class="price">¥{{ yuan(p.priceCent) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100%;
  background: #f5f5f5;
  padding: 0 12px 24px;
}
.bar {
  display: flex;
  align-items: center;
  margin: -8px -4px 14px;
  padding: 8px 0 12px;
  border-bottom: 1px solid #eee;
}
.tit {
  font-size: 17px;
  font-weight: 600;
  color: #333;
}
.empty {
  text-align: center;
  color: #999;
  font-size: 14px;
  padding: 48px 16px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
.card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  cursor: pointer;
}
.cover-wrap {
  aspect-ratio: 1;
  background: #f0f0f0;
}
.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.cover.ph {
  min-height: 100px;
}
.meta {
  padding: 10px 10px 12px;
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
.price {
  margin-top: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #ff2442;
}
</style>
