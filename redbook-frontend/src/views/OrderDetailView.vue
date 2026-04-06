<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { orderDetail, orderClose, orderDeleteRecord, orderRefund } from '../api'
import PayPasswordDialog from '../components/PayPasswordDialog.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const pwdVisible = ref(false)
const detail = ref(null)

const orderId = computed(() => route.params.orderId)

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

function fmtTime(v) {
  if (v == null || v === '') return ''
  const s = typeof v === 'string' ? v : String(v)
  return s.replace('T', ' ').slice(0, 19)
}

async function load() {
  const id = orderId.value
  if (id == null || id === '') {
    loading.value = false
    return
  }
  loading.value = true
  try {
    const { data } = await orderDetail(id)
    detail.value = data ?? null
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
}

function openPayDialog() {
  if (!detail.value?.payable || orderId.value == null) return
  pwdVisible.value = true
}

async function onPwdPaid() {
  await load()
  ElMessage.success('支付成功')
}

function goWallet() {
  router.push({ name: 'market-wallet' })
}

async function closeOrder() {
  if (detail.value?.status !== 0) return
  try {
    await ElMessageBox.confirm(
      '关闭后订单将取消；购物车中的商品不会被移除。确定关闭？',
      '关闭订单',
      { type: 'warning' }
    )
    await orderClose(orderId.value)
    ElMessage.success('订单已关闭')
    await load()
  } catch (e) {
    if (e !== 'cancel') {
      /* http 已提示 */
    }
  }
}

async function refundOrder() {
  if (!detail.value?.refundable) return
  try {
    await ElMessageBox.confirm(
      '实付将退回钱包，优惠券将恢复为未使用。确定退款？',
      '申请退款',
      { type: 'warning' }
    )
    await orderRefund(orderId.value)
    ElMessage.success('已退款')
    await load()
  } catch (e) {
    if (e !== 'cancel') {
      /* http 已提示 */
    }
  }
}

async function deleteRecord() {
  const st = detail.value?.status
  if (st === 0 || st == null) return
  try {
    await ElMessageBox.confirm(
      '删除后无法恢复，仅移除订单记录（已支付订单不会自动退款）。确定删除？',
      '删除订单记录',
      { type: 'warning' }
    )
    await orderDeleteRecord(orderId.value)
    ElMessage.success('已删除')
    router.replace({ name: 'orders' })
  } catch (e) {
    if (e !== 'cancel') {
      /* http 已提示 */
    }
  }
}

watch(orderId, () => load())

onMounted(() => load())
</script>

<template>
  <div class="order-detail">
    <div class="top-bar">
      <el-button text circle title="返回" class="back-btn" @click="router.back()">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
    </div>
    <div v-if="loading" class="muted">加载中…</div>
    <div v-else-if="!detail" class="muted">订单不存在或无权查看</div>
    <template v-else>
      <div class="head">
        <span class="id">订单 #{{ detail.id }}</span>
        <span class="st">{{ statusText(detail.status) }}</span>
      </div>
      <div v-if="detail.status === 0 && detail.payDeadline" class="deadline">
        支付截止时间：{{ fmtTime(detail.payDeadline) }}
        <span v-if="!detail.payable" class="exp">（已超时，请重新下单）</span>
      </div>
      <div class="prices">
        <span>商品总额 ¥{{ yuan(detail.totalCent) }}</span>
        <span v-if="detail.payCent != null" class="pay">应付 ¥{{ yuan(detail.payCent) }}</span>
        <span v-if="detail.discountCent" class="disc">优惠 ¥{{ yuan(detail.discountCent) }}</span>
      </div>
      <div class="time">创建时间 {{ fmtTime(detail.createTime) }}</div>
      <div v-if="detail.status === 1 && detail.payTime" class="time">
        支付时间 {{ fmtTime(detail.payTime) }}
        <template v-if="detail.refundDeadline">
          · 退款截止 {{ fmtTime(detail.refundDeadline) }}
        </template>
      </div>
      <div v-if="detail.status === 1 && detail.sellerSettled === 1" class="muted-line">
        货款已结算给卖家，不可退款
      </div>

      <h3 class="sub">商品明细</h3>
      <div v-for="(it, idx) in detail.items || []" :key="idx" class="line">
        <img v-if="it.cover" :src="it.cover" alt="" class="cov" />
        <div class="meta">
          <div class="t">{{ it.title }}</div>
          <div class="p">¥{{ yuan(it.priceCent) }} × {{ it.quantity }}</div>
        </div>
      </div>

      <div class="actions">
        <template v-if="detail.payable">
          <el-button type="primary" @click="openPayDialog">继续支付</el-button>
          <el-button text type="primary" @click="goWallet">去钱包充值</el-button>
        </template>
        <el-button v-if="detail.refundable" type="warning" plain @click="refundOrder">
          申请退款
        </el-button>
        <el-button v-if="detail.status === 0" type="danger" plain @click="closeOrder">
          关闭订单
        </el-button>
        <el-button
          v-if="detail.status === 1 || detail.status === 2 || detail.status === 3"
          type="info"
          plain
          @click="deleteRecord"
        >
          删除记录
        </el-button>
      </div>
    </template>

    <PayPasswordDialog
      v-model="pwdVisible"
      :order-id="orderId"
      @paid="onPwdPaid"
    />
  </div>
</template>

<style scoped>
.order-detail {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  max-width: 560px;
  margin: 0 auto;
}
.top-bar {
  display: flex;
  align-items: center;
  margin: -4px 0 12px -4px;
}
.back-btn {
  color: #333;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.id {
  font-weight: 600;
  font-size: 16px;
}
.st {
  font-size: 14px;
  color: #ff2442;
}
.deadline {
  margin-top: 10px;
  font-size: 13px;
  color: #666;
}
.deadline .exp {
  color: #e6a23c;
}
.prices {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin-top: 12px;
  font-size: 14px;
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
  margin-top: 8px;
}
.sub {
  margin: 20px 0 12px;
  font-size: 15px;
  font-weight: 600;
}
.line {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}
.cov {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 8px;
}
.meta .t {
  font-weight: 500;
  font-size: 14px;
}
.meta .p {
  color: #ff2442;
  margin-top: 6px;
  font-size: 13px;
}
.actions {
  margin-top: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.muted {
  color: #999;
  text-align: center;
  padding: 32px;
}
.muted-line {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}
</style>
