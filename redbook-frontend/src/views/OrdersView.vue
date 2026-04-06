<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderMy, orderRefund, orderClose, orderDeleteRecord } from '../api'

const router = useRouter()

const list = ref([])
const current = ref(1)
const total = ref(0)

function statusText(s) {
  if (s === 0) return '待支付'
  if (s === 1) return '已支付'
  if (s === 2) return '已取消'
  if (s === 3) return '已退款'
  return String(s)
}

function yuan(cent) {
  return ((cent || 0) / 100).toFixed(2)
}

async function load(append) {
  try {
    const { data, total: t } = await orderMy({ current: current.value, size: 10 })
    const rows = data || []
    if (append) {
      list.value = [...list.value, ...rows]
    } else {
      list.value = rows
    }
    total.value = t || 0
  } catch {
    if (!append) list.value = []
  }
}

function next() {
  if (list.value.length >= total.value) return
  current.value += 1
  load(true)
}

function canRefund(o) {
  if (o.status !== 1 || o.payCent == null) return false
  if (o.sellerSettled === 1) return false
  if (!o.payTime) return false
  const t = typeof o.payTime === 'string' ? o.payTime.replace(' ', 'T') : o.payTime
  const deadline = new Date(t).getTime() + 60 * 60 * 1000
  return Date.now() < deadline
}

function canClose(o) {
  return o.status === 0
}

function canDeleteRecord(o) {
  return o.status === 1 || o.status === 2 || o.status === 3
}

function openDetail(o) {
  router.push({ name: 'order-detail', params: { orderId: String(o.id) } })
}

function goProduct(productId) {
  if (productId == null) return
  router.push({ name: 'product-detail', params: { id: String(productId) } })
}

async function refund(o) {
  try {
    await ElMessageBox.confirm(
      '支付成功后 1 小时内可申请退款：实付金额退回钱包，已用优惠券将恢复为未使用。超时后货款将结算给卖家。确定退款？',
      '申请退款',
      { type: 'warning' }
    )
    await orderRefund(o.id)
    ElMessage.success('已退款')
    current.value = 1
    await load(false)
  } catch (e) {
    if (e !== 'cancel') {
      /* http 已提示 */
    }
  }
}

async function closeOrder(o) {
  try {
    await ElMessageBox.confirm(
      '关闭后订单将取消；购物车中的商品不会被移除。确定关闭？',
      '关闭订单',
      { type: 'warning' }
    )
    await orderClose(o.id)
    ElMessage.success('订单已关闭')
    current.value = 1
    await load(false)
  } catch (e) {
    if (e !== 'cancel') {
      /* http 已提示 */
    }
  }
}

async function deleteRecord(o) {
  try {
    await ElMessageBox.confirm(
      '删除后无法恢复，仅移除订单记录（已支付订单不会自动退款）。确定删除？',
      '删除订单记录',
      { type: 'warning' }
    )
    await orderDeleteRecord(o.id)
    ElMessage.success('已删除')
    current.value = 1
    await load(false)
  } catch (e) {
    if (e !== 'cancel') {
      /* http 已提示 */
    }
  }
}

onMounted(() => load(false))
</script>

<template>
  <div class="orders">
    <h2>我的订单</h2>
    <div v-if="!list.length" class="empty">暂无订单</div>
    <div v-for="o in list" :key="o.id" class="row">
      <div class="row-body" @click="openDetail(o)">
        <div class="head">
          <span class="title-line">{{ o.listTitle || '订单' }}</span>
          <span class="st">{{ statusText(o.status) }}</span>
        </div>
        <div v-if="o.products?.length" class="covers" @click.stop>
          <div
            v-for="(it, idx) in o.products"
            :key="`${o.id}-${it.productId}-${idx}`"
            class="cov-wrap"
            role="button"
            tabindex="0"
            @click="goProduct(it.productId)"
            @keydown.enter.prevent="goProduct(it.productId)"
          >
            <img v-if="it.cover" :src="it.cover" :alt="it.title || ''" class="cov" />
            <div v-else class="cov cov-ph" />
          </div>
        </div>
        <div class="prices">
          <span>商品总额 ¥{{ yuan(o.totalCent) }}</span>
          <template v-if="o.payCent != null">
            <span class="pay">实付 ¥{{ yuan(o.payCent) }}</span>
            <span v-if="o.discountCent" class="disc">优惠 ¥{{ yuan(o.discountCent) }}</span>
          </template>
        </div>
        <div class="time">{{ o.createTime }}</div>
      </div>
      <div class="row-actions" @click.stop>
        <el-button
          v-if="canClose(o)"
          type="danger"
          plain
          size="small"
          @click="closeOrder(o)"
        >
          关闭订单
        </el-button>
        <el-button
          v-if="canDeleteRecord(o)"
          type="info"
          plain
          size="small"
          @click="deleteRecord(o)"
        >
          删除记录
        </el-button>
        <el-button
          v-if="canRefund(o)"
          type="warning"
          plain
          size="small"
          @click="refund(o)"
        >
          申请退款
        </el-button>
      </div>
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
  padding: 14px 0;
  border-bottom: 1px solid #f0f0f0;
}
.row-body {
  cursor: pointer;
}
.row:hover {
  background: #fafafa;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}
.title-line {
  font-weight: 600;
  line-height: 1.35;
  flex: 1;
  min-width: 0;
}
.covers {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}
.cov-wrap {
  flex-shrink: 0;
}
.cov {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  object-fit: cover;
  display: block;
  cursor: pointer;
  vertical-align: middle;
}
.cov-ph {
  background: #eee;
}
.st {
  font-size: 13px;
  color: #ff2442;
}
.prices {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin-top: 8px;
  font-size: 13px;
  color: #666;
}
.disc {
  color: #e6a23c;
}
.pay {
  color: #333;
  font-weight: 600;
}
.time {
  font-size: 12px;
  color: #999;
  margin-top: 6px;
}
.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}
</style>
