<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { productGet, cartAdd } from '../api'
import DirectPayDialog from '../components/DirectPayDialog.vue'
import { recordProductFootprint } from '../utils/productFootprint'

const route = useRoute()
const router = useRouter()
const id = computed(() => route.params.id)
const p = ref(null)
const payOpen = ref(false)

async function load() {
  try {
    const { data } = await productGet(id.value)
    p.value = data
    if (data) {
      recordProductFootprint({
        id: data.id,
        title: data.title,
        cover: data.cover,
        priceCent: data.priceCent,
      })
    }
  } catch {
    p.value = null
  }
}

function yuan(cent) {
  return ((cent || 0) / 100).toFixed(2)
}

async function addCart() {
  try {
    await cartAdd(p.value.id, 1)
    ElMessage.success('已加入购物车')
  } catch {
    /* */
  }
}

function openPay() {
  payOpen.value = true
}

onMounted(load)
watch(id, () => {
  load()
})
</script>

<template>
  <div v-if="p" class="pd">
    <header class="bar">
      <el-button text circle title="返回" class="back-btn" @click="router.back()">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
    </header>
    <img v-if="p.cover" :src="p.cover" class="cover" alt="" />
    <h1>{{ p.title }}</h1>
    <div class="price">¥{{ yuan(p.priceCent) }}</div>
    <div class="stock">库存 {{ p.stock ?? 0 }}</div>
    <div class="btns">
      <el-button type="primary" size="large" class="half" @click="addCart">加入购物车</el-button>
      <el-button type="danger" size="large" class="half" plain @click="openPay">立即支付</el-button>
    </div>
    <DirectPayDialog v-model="payOpen" :product-id="p.id" :product-hint="p" @paid="() => router.push({ name: 'orders' })" />
  </div>
  <div v-else class="loading">加载中…</div>
</template>

<style scoped>
.pd {
  max-width: 480px;
  margin: 0 auto;
  padding: 16px;
  background: #fff;
  min-height: 100vh;
}
.bar {
  display: flex;
  align-items: center;
  margin: -8px -8px 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #eee;
}
.back-btn {
  margin-left: -8px;
  color: #333;
}
.cover {
  width: 100%;
  max-height: 360px;
  object-fit: cover;
  border-radius: 12px;
}
h1 {
  font-size: 20px;
  margin: 16px 0 8px;
}
.price {
  color: #ff2442;
  font-size: 22px;
  font-weight: 700;
}
.stock {
  color: #999;
  margin: 8px 0 24px;
}
.btns {
  display: flex;
  gap: 10px;
}
.half {
  flex: 1;
}
.loading {
  text-align: center;
  padding: 48px;
  color: #999;
}
</style>
