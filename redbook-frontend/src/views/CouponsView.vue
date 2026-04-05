<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { couponMy } from '../api'

const router = useRouter()

const list = ref([])
const current = ref(1)
const total = ref(0)
const loading = ref(false)

function yuan(cent) {
  return ((cent || 0) / 100).toFixed(2)
}

async function load(reset) {
  if (reset) {
    current.value = 1
  }
  loading.value = true
  try {
    const { data, total: t } = await couponMy({ current: current.value, size: 20 })
    const rows = data || []
    if (reset) {
      list.value = rows
    } else {
      list.value = [...list.value, ...rows]
    }
    total.value = t || 0
  } catch {
    if (reset) {
      list.value = []
      total.value = 0
    }
  } finally {
    loading.value = false
  }
}

function more() {
  if (list.value.length >= total.value) return
  current.value += 1
  load(false)
}

function goProduct(c) {
  if (!c?.productId) return
  router.push({ name: 'product-detail', params: { id: String(c.productId) } })
}

onMounted(() => load(true))
</script>

<template>
  <div class="page">
    <header class="bar">
      <span class="tit">优惠券</span>
      <el-button text type="primary" size="small" @click="load(true)">刷新</el-button>
    </header>

    <div v-if="loading" class="muted">加载中…</div>
    <el-empty v-else-if="!list.length" description="暂无可用优惠券" />

    <div v-else class="list">
      <div
        v-for="c in list"
        :key="c.id"
        class="ticket"
        :class="{ link: !!c.productId }"
        @click="goProduct(c)"
      >
        <div class="left">
          <div class="amt">¥{{ yuan(c.discountCent) }}</div>
          <div class="sub">满 {{ yuan(c.minOrderCent) }} 可用</div>
        </div>
        <div class="right">
          <div v-if="c.productCover" class="thumb-wrap">
            <img :src="c.productCover" alt="" class="thumb" />
          </div>
          <div class="r-text">
            <div class="name">{{ c.title || '优惠券' }}</div>
            <div v-if="c.productTitle" class="prod">适用商品：{{ c.productTitle }}</div>
            <div v-if="c.productId" class="go">点击查看商品</div>
            <div class="no">编号 {{ c.id }}</div>
            <div class="exp">有效期至 {{ c.expireTime }}</div>
          </div>
        </div>
      </div>
      <el-button v-if="list.length < total" text type="primary" class="more" @click="more">
        加载更多
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100%;
  background: #f5f5f5;
  padding: 0 16px 24px;
}
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -8px -8px 16px;
  padding: 8px 0 12px;
  border-bottom: 1px solid #eee;
  background: #f5f5f5;
}
.tit {
  font-size: 17px;
  font-weight: 600;
  color: #333;
}
.muted {
  text-align: center;
  color: #999;
  padding: 24px;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.ticket {
  display: flex;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  border: 1px solid #ffe0e8;
}
.ticket.link {
  cursor: pointer;
}
.ticket.link:active {
  opacity: 0.92;
}
.prod {
  font-size: 13px;
  color: #666;
  margin-top: 6px;
}
.go {
  font-size: 12px;
  color: #ff2442;
  margin-top: 4px;
}
.left {
  width: 100px;
  flex-shrink: 0;
  background: linear-gradient(160deg, #ff6b81, #ff2442);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12px 8px;
}
.amt {
  font-size: 22px;
  font-weight: 700;
}
.sub {
  font-size: 11px;
  opacity: 0.95;
  margin-top: 4px;
  text-align: center;
}
.right {
  flex: 1;
  padding: 12px 14px;
  min-width: 0;
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.thumb-wrap {
  flex-shrink: 0;
}
.thumb {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}
.r-text {
  flex: 1;
  min-width: 0;
}
.name {
  font-weight: 600;
  font-size: 15px;
  color: #333;
}
.no {
  font-size: 12px;
  color: #999;
  margin-top: 6px;
}
.exp {
  font-size: 12px;
  color: #888;
  margin-top: 4px;
}
.more {
  align-self: center;
  margin-top: 8px;
}
</style>
