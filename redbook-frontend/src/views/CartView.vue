<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { cartMy, productGet, cartUpdate, cartRemove, orderCreate, couponMy } from '../api'
import { isProductOnShelf, PRODUCT_OFF_SHELF_MSG } from '../utils/productShelf'
import PayPasswordDialog from '../components/PayPasswordDialog.vue'

const router = useRouter()

const lines = ref([])
const loading = ref(false)
const coupons = ref([])
const selectedCouponId = ref(null)
const pwdVisible = ref(false)
const pendingOrderId = ref(null)

const totalCent = computed(() =>
  lines.value.reduce((s, row) => {
    const p = row.product?.priceCent ?? 0
    const q = row.cart.quantity ?? 0
    return s + p * q
  }, 0)
)

/** 与后端 OrderServiceImpl.computeCouponDiscount 一致：绑定商品的券仅看该商品行小计与件数 */
function cartMatchForCoupon(c) {
  const min = c.minOrderCent ?? 0
  const pid = c.productId
  if (pid == null) {
    return { matchSub: totalCent.value, matchQty: 0, minOk: totalCent.value >= min }
  }
  let matchSub = 0
  let matchQty = 0
  for (const row of lines.value) {
    if (row.product?.id === pid) {
      const p = row.product?.priceCent ?? 0
      const q = row.cart.quantity ?? 0
      matchSub += p * q
      matchQty += q
    }
  }
  return { matchSub, matchQty, minOk: matchSub > 0 && matchSub >= min }
}

const usableCoupons = computed(() =>
  (coupons.value || []).filter((c) => cartMatchForCoupon(c).minOk)
)

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

async function loadCoupons() {
  try {
    const { data } = await couponMy({ current: 1, size: 50 })
    coupons.value = data || []
  } catch {
    coupons.value = []
  }
}

function yuan(cent) {
  return ((cent || 0) / 100).toFixed(2)
}

async function changeQty(row, q) {
  try {
    await cartUpdate(row.cart.id, q)
    row.cart.quantity = q
    syncCouponSelection()
  } catch {
    /* */
  }
}

function syncCouponSelection() {
  if (selectedCouponId.value == null) return
  const ok = usableCoupons.value.some((c) => c.id === selectedCouponId.value)
  if (!ok) selectedCouponId.value = null
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('从购物车移除？', '提示')
    await cartRemove(row.cart.id)
    lines.value = lines.value.filter((x) => x.cart.id !== row.cart.id)
    syncCouponSelection()
    ElMessage.success('已移除')
  } catch {
    /* */
  }
}

const estimatedDiscount = computed(() => {
  if (selectedCouponId.value == null) return 0
  const c = coupons.value.find((x) => x.id === selectedCouponId.value)
  if (!c) return 0
  const unitDisc = c.discountCent ?? 0
  const pid = c.productId
  if (pid == null) {
    const min = c.minOrderCent ?? 0
    if (totalCent.value < min) return 0
    return Math.min(unitDisc, totalCent.value)
  }
  const { matchSub, matchQty, minOk } = cartMatchForCoupon(c)
  if (!minOk) return 0
  return Math.min(unitDisc * matchQty, matchSub)
})

const estimatedPay = computed(() => Math.max(0, totalCent.value - estimatedDiscount.value))

const hasOffShelfInCart = computed(() =>
  lines.value.some((row) => row.product != null && !isProductOnShelf(row.product))
)

async function checkout() {
  if (!lines.value.length) return
  if (hasOffShelfInCart.value) {
    ElMessage.warning(PRODUCT_OFF_SHELF_MSG)
    return
  }
  try {
    await ElMessageBox.confirm('将生成待支付订单，确认后在下一步输入支付密码完成扣款。', '结算下单')
    const body = {}
    if (selectedCouponId.value != null) {
      body.userCouponId = selectedCouponId.value
    }
    const { data: orderId } = await orderCreate(body)
    pendingOrderId.value = orderId
    pwdVisible.value = true
    selectedCouponId.value = null
    await loadCoupons()
    await load()
  } catch {
    /* */
  }
}

async function onCartPwdPaid(orderId) {
  pendingOrderId.value = null
  ElMessage.success('支付成功，订单号 ' + orderId)
  await loadCoupons()
  await load()
}

function onCartPwdCancelled(orderId) {
  pendingOrderId.value = null
  ElMessage.warning('订单已生成，请在 15 分钟内到「我的订单」完成支付')
  if (orderId != null && orderId !== '') {
    router.push({ name: 'order-detail', params: { orderId: String(orderId) } })
  }
}

onMounted(async () => {
  await Promise.all([load(), loadCoupons()])
})
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
          <div v-if="row.product && !isProductOnShelf(row.product)" class="off-line">{{ PRODUCT_OFF_SHELF_MSG }}</div>
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

      <div class="sum">
        <div>商品合计：<b>¥{{ yuan(totalCent) }}</b></div>
        <div v-if="usableCoupons.length" class="coupon-row">
          <span class="lbl">优惠券</span>
          <el-select
            v-model="selectedCouponId"
            clearable
            placeholder="不使用优惠券"
            class="sel"
          >
            <el-option
              v-for="c in usableCoupons"
              :key="c.id"
              :label="`${c.title || '优惠券'} · 减¥${yuan(c.discountCent)}（满¥${yuan(c.minOrderCent)}）`"
              :value="c.id"
            />
          </el-select>
        </div>
        <div v-else-if="coupons.length" class="muted">暂无满足门槛的可用优惠券</div>
        <div v-if="selectedCouponId != null" class="est">
          预计优惠 ¥{{ yuan(estimatedDiscount) }}，钱包实付
          <b class="pay">¥{{ yuan(estimatedPay) }}</b>
        </div>
        <div v-else class="est">
          钱包实付 <b class="pay">¥{{ yuan(totalCent) }}</b>
        </div>
      </div>

      <el-button type="primary" class="full" :disabled="hasOffShelfInCart" @click="checkout">结算下单</el-button>
    </div>

    <PayPasswordDialog
      v-model="pwdVisible"
      :order-id="pendingOrderId"
      @paid="onCartPwdPaid"
      @cancelled="onCartPwdCancelled"
    />
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
.off-line {
  font-size: 12px;
  color: #909399;
  font-weight: 600;
  margin-top: 4px;
}
.p {
  color: #ff2442;
  margin: 4px 0;
}
.sum {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  font-size: 14px;
  line-height: 1.7;
}
.coupon-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}
.coupon-row .lbl {
  white-space: nowrap;
  color: #666;
}
.sel {
  flex: 1;
  min-width: 0;
}
.muted {
  color: #999;
  font-size: 13px;
  margin-top: 8px;
}
.est {
  margin-top: 8px;
  color: #666;
}
.pay {
  color: #ff2442;
  font-size: 16px;
}
.full {
  width: 100%;
  margin-top: 16px;
}
</style>
