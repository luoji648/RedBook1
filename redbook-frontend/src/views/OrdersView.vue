<script setup>
import { ref, onMounted } from 'vue'
import { orderMy } from '../api'

const list = ref([])
const current = ref(1)
const total = ref(0)

function statusText(s) {
  if (s === 0) return '待支付'
  if (s === 1) return '已支付'
  if (s === 2) return '已取消'
  return String(s)
}

function yuan(cent) {
  return ((cent || 0) / 100).toFixed(2)
}

async function load() {
  try {
    const { data, total: t } = await orderMy({ current: current.value, size: 10 })
    list.value = data || []
    total.value = t || 0
  } catch {
    list.value = []
  }
}

function next() {
  if (list.value.length >= total.value) return
  current.value += 1
  load()
}

onMounted(load)
</script>

<template>
  <div class="orders">
    <h2>我的订单</h2>
    <div v-if="!list.length" class="empty">暂无订单</div>
    <div v-for="o in list" :key="o.id" class="row">
      <div class="id">订单 #{{ o.id }}</div>
      <div class="meta">
        <span>{{ statusText(o.status) }}</span>
        <span class="price">¥{{ yuan(o.totalCent) }}</span>
      </div>
      <div class="time">{{ o.createTime }}</div>
    </div>
    <el-button v-if="list.length < total" text type="primary" @click="next">下一页</el-button>
  </div>
</template>

<style scoped>
.orders {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}
h2 {
  margin: 0 0 16px;
}
.empty {
  color: #999;
  text-align: center;
  padding: 32px;
}
.row {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.id {
  font-weight: 600;
}
.meta {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
}
.price {
  color: #ff2442;
}
.time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
