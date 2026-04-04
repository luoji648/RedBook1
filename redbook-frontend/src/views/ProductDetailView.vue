<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { productGet, cartAdd } from '../api'

const route = useRoute()
const id = computed(() => route.params.id)
const p = ref(null)

async function load() {
  try {
    const { data } = await productGet(id.value)
    p.value = data
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

onMounted(load)
watch(id, () => {
  load()
})
</script>

<template>
  <div v-if="p" class="pd">
    <img v-if="p.cover" :src="p.cover" class="cover" alt="" />
    <h1>{{ p.title }}</h1>
    <div class="price">¥{{ yuan(p.priceCent) }}</div>
    <div class="stock">库存 {{ p.stock ?? 0 }}</div>
    <el-button type="primary" size="large" class="full" @click="addCart">加入购物车</el-button>
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
.full {
  width: 100%;
}
.loading {
  text-align: center;
  padding: 48px;
  color: #999;
}
</style>
