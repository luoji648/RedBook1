<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  List,
  ShoppingCart,
  Ticket,
  Service,
  View,
  Goods,
  Wallet,
  Management,
} from '@element-plus/icons-vue'
import NoteMasonry from '../components/NoteMasonry.vue'
import { noteRecommend } from '../api'

const router = useRouter()

const list = ref([])
const backendPage = ref(1)
const loading = ref(false)
const finished = ref(false)
const seenIds = new Set()

const MAX_BACKEND_PAGES = 30

function goOrders() {
  router.push({ name: 'orders' })
}

function goCart() {
  router.push({ name: 'cart' })
}

function goCoupons() {
  router.push({ name: 'market-coupons' })
}

function goService() {
  router.push({ name: 'chat' })
}

function goFootprint() {
  router.push({ name: 'product-footprint' })
}

function goWallet() {
  router.push({ name: 'market-wallet' })
}

function goProductSave() {
  router.push({ name: 'product-save' })
}

function goProductManage() {
  router.push({ name: 'product-manage' })
}

function hasLinkedProducts(vo) {
  const nps = vo?.noteProducts
  return Array.isArray(nps) && nps.length > 0
}

async function load() {
  if (loading.value || finished.value) return
  if (backendPage.value > MAX_BACKEND_PAGES) {
    finished.value = true
    return
  }
  loading.value = true
  try {
    const { data } = await noteRecommend({ current: backendPage.value, size: 15 })
    backendPage.value += 1
    if (!data || data.length === 0) {
      finished.value = true
      return
    }
    for (const vo of data) {
      const id = vo?.note?.id
      if (!id || seenIds.has(id)) continue
      if (!hasLinkedProducts(vo)) continue
      seenIds.add(id)
      list.value.push(vo)
    }
    if (data.length < 15) finished.value = true
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
  <div class="market">
    <h2 class="title">集市</h2>

    <div class="shortcuts">
      <div class="sc" @click="goProductSave">
        <div class="icon-wrap">
          <el-icon :size="22" color="#ff2442"><Goods /></el-icon>
        </div>
        <span>发布商品</span>
      </div>
      <div class="sc" @click="goProductManage">
        <div class="icon-wrap">
          <el-icon :size="22" color="#ff2442"><Management /></el-icon>
        </div>
        <span>我的商品</span>
      </div>
      <div class="sc" @click="goOrders">
        <div class="icon-wrap">
          <el-icon :size="22" color="#ff2442"><List /></el-icon>
        </div>
        <span>我的订单</span>
      </div>
      <div class="sc" @click="goCart">
        <div class="icon-wrap">
          <el-icon :size="22" color="#ff2442"><ShoppingCart /></el-icon>
        </div>
        <span>购物车</span>
      </div>
      <div class="sc" @click="goCoupons">
        <div class="icon-wrap">
          <el-icon :size="22" color="#ff2442"><Ticket /></el-icon>
        </div>
        <span>优惠券</span>
      </div>
      <div class="sc" @click="goWallet">
        <div class="icon-wrap">
          <el-icon :size="22" color="#ff2442"><Wallet /></el-icon>
        </div>
        <span>我的钱包</span>
      </div>
      <div class="sc" @click="goService">
        <div class="icon-wrap">
          <el-icon :size="22" color="#ff2442"><Service /></el-icon>
        </div>
        <span>客服消息</span>
      </div>
      <div class="sc" @click="goFootprint">
        <div class="icon-wrap">
          <el-icon :size="22" color="#ff2442"><View /></el-icon>
        </div>
        <span>商品足迹</span>
      </div>
    </div>

    <h3 class="sub">关联商品的笔记</h3>
    <NoteMasonry :list="list" />
    <div v-if="loading" class="more">加载中…</div>
    <div v-else-if="finished && list.length === 0" class="more">暂无带商品的笔记</div>
    <div v-else-if="finished" class="more">没有更多了</div>
  </div>
</template>

<style scoped>
.title {
  font-size: 18px;
  margin: 0 0 12px;
  font-weight: 600;
}
.shortcuts {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 6px;
  padding: 14px 8px 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  margin-bottom: 16px;
}
.sc {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 11px;
  color: #333;
  text-align: center;
  line-height: 1.2;
}
.icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: #fff5f7;
  display: flex;
  align-items: center;
  justify-content: center;
}
.sub {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 10px;
  color: #333;
}
.more {
  text-align: center;
  color: #999;
  padding: 16px;
  font-size: 13px;
}
</style>
