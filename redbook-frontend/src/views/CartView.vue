<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cartMy, productGet, cartUpdate, cartRemove, orderCreate } from '../api'

const lines = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const { data } = await cartMy()
    const raw = data || []
    const enriched = []
    for (const c of raw) {
      try {
        const { data: p } = await productGet(c.productId)
        enriched.push({ cart: c, product: p })
      } catch {
        enriched.push({ cart: c, product: null })
      }
    }
    lines.value = enriched
  } catch {
    lines.value = []
  } finally {
    loading.value = false
  }
}

function yuan(cent) {
  return ((cent || 0) / 100).toFixed(2)
}

async function changeQty(row, q) {
  try {
    await cartUpdate(row.cart.id, q)
    row.cart.quantity = q
  } catch {
    /* */
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('从购物车移除？', '提示')
    await cartRemove(row.cart.id)
    lines.value = lines.value.filter((x) => x.cart.id !== row.cart.id)
    ElMessage.success('已移除')
  } catch {
    /* */
  }
}

async function checkout() {
  if (!lines.value.length) return
  try {
    await ElMessageBox.confirm('使用购物车商品下单？', '下单')
    const { data: orderId } = await orderCreate()
    ElMessage.success('下单成功，订单号 ' + orderId)
    lines.value = []
  } catch {
    /* */
  }
}

onMounted(load)
</script>

<template>
  <div class="cart">
    <h2>购物车</h2>
    <div v-if="loading">加载中…</div>
    <div v-else-if="!lines.length" class="empty">购物车是空的</div>
    <div v-else>
      <div v-for="row in lines" :key="row.cart.id" class="line">
        <img v-if="row.product?.cover" :src="row.product.cover" alt="" class="cov" />
        <div class="info">
          <div class="t">{{ row.product?.title || '商品' + row.cart.productId }}</div>
          <div class="p">¥{{ yuan(row.product?.priceCent) }} × {{ row.cart.quantity }}</div>
          <el-input-number
            :model-value="row.cart.quantity"
            :min="1"
            size="small"
            @change="(v) => changeQty(row, v)"
          />
        </div>
        <el-button type="danger" text @click="remove(row)">删除</el-button>
      </div>
      <el-button type="primary" class="full" @click="checkout">结算下单</el-button>
    </div>
  </div>
</template>

<style scoped>
.cart {
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
  padding: 40px;
}
.line {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  align-items: flex-start;
}
.cov {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 8px;
}
.info {
  flex: 1;
}
.t {
  font-weight: 600;
}
.p {
  color: #ff2442;
  margin: 4px 0;
}
.full {
  width: 100%;
  margin-top: 16px;
}
</style>
