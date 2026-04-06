<script setup>
import { ref, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import {
  orderDirectPreview,
  orderCreateDirect,
  couponClaimFollow,
  productGet,
} from '../api'
import { isProductOnShelf, PRODUCT_OFF_SHELF_MSG } from '../utils/productShelf'
import PayPasswordDialog from './PayPasswordDialog.vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  /** 当前要支付的商品 id */
  productId: { type: [Number, String], default: null },
  /** 可选：已有商品信息时可减少一次请求，仍会与预览合并库存等 */
  productHint: { type: Object, default: null },
  /**
   * 帖子场景：商品未写 seller_id 时传笔记作者用户 id，后端用于「关注卖家领券」判断
   */
  fallbackSellerUserId: { type: [Number, String], default: null },
  /** 下架提示文案；不传则用全局默认（如购物车/商品页） */
  offShelfMsg: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue', 'paid'])

const router = useRouter()
const auth = useAuthStore()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const quantity = ref(1)
const preview = ref(null)
const productSnap = ref(null)
const loading = ref(false)
const paying = ref(false)
const pwdVisible = ref(false)
const pendingOrderId = ref(null)
const claiming = ref(false)
const useCoupon = ref(true)

function yuan(cent) {
  return ((cent || 0) / 100).toFixed(2)
}

const maxQty = computed(() => {
  const s = productSnap.value?.stock ?? props.productHint?.stock
  if (s == null || s < 1) return 999
  return s
})

watch(
  () => [props.modelValue, props.productId, props.fallbackSellerUserId],
  async ([open, pid]) => {
    if (!open || pid == null || pid === '') {
      preview.value = null
      productSnap.value = null
      return
    }
    quantity.value = 1
    useCoupon.value = true
    await refreshAll()
  }
)

function onQuantityChange() {
  if (quantity.value < 1) quantity.value = 1
  if (quantity.value > maxQty.value) {
    quantity.value = maxQty.value
    ElMessage.warning('已超过库存')
  }
  loadPreviewOnly()
}

async function refreshAll() {
  const pid = props.productId
  if (pid == null || pid === '') return
  loading.value = true
  preview.value = null
  try {
    const { data: prod } = await productGet(pid)
    productSnap.value = prod ?? props.productHint ?? null
  } catch {
    productSnap.value = props.productHint ?? null
  }
  if (auth.token) {
    try {
      const fb = props.fallbackSellerUserId
      const { data: prev } = await orderDirectPreview(pid, quantity.value, fb != null && fb !== '' ? fb : undefined)
      preview.value = prev
    } catch {
      preview.value = null
    }
  }
  loading.value = false
}

async function loadPreviewOnly() {
  if (!auth.token || props.productId == null) return
  loading.value = true
  try {
    const fb = props.fallbackSellerUserId
    const { data } = await orderDirectPreview(
      props.productId,
      quantity.value,
      fb != null && fb !== '' ? fb : undefined
    )
    preview.value = data
  } catch {
    preview.value = null
  } finally {
    loading.value = false
  }
}

async function claim() {
  if (!auth.token) {
    router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  if (offShelf.value) {
    ElMessage.warning(offShelfMessage.value)
    return
  }
  claiming.value = true
  try {
    const body = { productId: Number(props.productId) }
    const fb = props.fallbackSellerUserId
    if (fb != null && fb !== '') body.fallbackSellerUserId = Number(fb)
    await couponClaimFollow(body)
    ElMessage.success('已领取，可在「集市-优惠券」查看')
    await loadPreviewOnly()
  } catch {
    /* http 已提示 */
  } finally {
    claiming.value = false
  }
}

function goFollowSeller() {
  const sid = preview.value?.sellerId
  if (!sid) return
  router.push({ name: 'user-profile', params: { userId: String(sid) } })
}

function goWallet() {
  visible.value = false
  router.push({ name: 'market-wallet' })
}

async function pay() {
  if (!auth.token) {
    router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
    return
  }
  if (offShelf.value) {
    ElMessage.warning(offShelfMessage.value)
    return
  }
  const pid = Number(props.productId)
  const q = quantity.value
  const body = { productId: pid, quantity: q }
  if (useCoupon.value && preview.value?.userCouponId) {
    body.userCouponId = preview.value.userCouponId
  }
  paying.value = true
  try {
    const { data: orderId } = await orderCreateDirect(body)
    pendingOrderId.value = orderId
    visible.value = false
    pwdVisible.value = true
  } catch {
    /* */
  } finally {
    paying.value = false
  }
}

function onPwdPaid(orderId) {
  pendingOrderId.value = null
  ElMessage.success('支付成功')
  emit('paid', orderId)
  visible.value = false
}

function onPwdCancelled(orderId) {
  pendingOrderId.value = null
  ElMessage.warning('订单已生成，请在 15 分钟内到「我的订单」完成支付')
  if (orderId != null && orderId !== '') {
    router.push({ name: 'order-detail', params: { orderId: String(orderId) } })
  }
}

/** 与下单逻辑一致：未勾选券时用商品小计 */
const effectivePayCent = computed(() => {
  if (!preview.value) return 0
  if (useCoupon.value && preview.value.userCouponId) return preview.value.payCent ?? 0
  return preview.value.totalCent ?? 0
})

const balanceOk = computed(() => {
  if (!preview.value) return true
  const b = preview.value.walletBalanceCent ?? 0
  return b >= effectivePayCent.value
})

const displayTitle = computed(
  () => productSnap.value?.title || preview.value?.title || props.productHint?.title || '商品'
)
const displayCover = computed(
  () => productSnap.value?.cover || preview.value?.cover || props.productHint?.cover || ''
)
const displayPrice = computed(
  () =>
    productSnap.value?.priceCent ?? preview.value?.priceCent ?? props.productHint?.priceCent ?? 0
)

const mergedProduct = computed(() => productSnap.value ?? props.productHint ?? null)
const offShelf = computed(() => mergedProduct.value != null && !isProductOnShelf(mergedProduct.value))
const offShelfMessage = computed(() =>
  props.offShelfMsg && props.offShelfMsg.trim() !== '' ? props.offShelfMsg : PRODUCT_OFF_SHELF_MSG
)
</script>

<template>
  <el-dialog
    v-model="visible"
    title="立即支付"
    width="92%"
    class="direct-pay-dlg"
    destroy-on-close
  >
    <div v-if="loading && !productSnap && !productHint" class="muted">加载中…</div>
    <template v-else>
      <el-alert v-if="offShelf" type="warning" :closable="false" class="off-alert" :title="offShelfMessage" />
      <div class="row">
        <img v-if="displayCover" :src="displayCover" alt="" class="cov" />
        <div class="meta">
          <div class="t">{{ displayTitle }}</div>
          <div class="p">¥{{ yuan(displayPrice) }}</div>
        </div>
      </div>

      <div class="qty">
        <span>数量</span>
        <el-input-number v-model="quantity" :min="1" :max="maxQty" size="small" @change="onQuantityChange" />
        <span v-if="productSnap?.stock != null" class="stk">库存 {{ productSnap.stock }}</span>
      </div>

      <template v-if="auth.token">
        <el-alert v-if="!preview && !loading && !offShelf" type="error" :closable="false" class="hint">
          <template #title>支付信息加载失败</template>
          <p class="err-desc">
            请确认后端已更新并启动；前端请用 <code>npm run dev</code>，或使用已带 API 代理的
            <code>npm run preview</code>。部署时请将 <code>/order</code>、<code>/coupon</code> 等前缀反代到 Spring Boot。
          </p>
        </el-alert>
        <el-alert v-if="preview?.followSellerCouponHint" type="info" :closable="false" class="hint">
          <div>{{ preview.followSellerCouponHint }}</div>
          <el-button v-if="preview?.sellerId" link type="primary" size="small" @click="goFollowSeller">
            去关注卖家
          </el-button>
        </el-alert>

        <div v-if="preview?.claimableFollowCoupon" class="claim-row">
          <el-button type="primary" plain size="small" :loading="claiming" @click="claim">
            领取关注卖家券（约省 10%）
          </el-button>
        </div>

        <el-checkbox
          v-if="preview?.userCouponId"
          v-model="useCoupon"
          class="use-cp"
        >
          使用优惠券（减 ¥{{ yuan(preview.discountCent) }}）
        </el-checkbox>

        <div v-if="preview" class="sum">
          <div>商品小计 <b>¥{{ yuan(preview.totalCent) }}</b></div>
          <div v-if="useCoupon && preview.userCouponId" class="disc">
            优惠 <b>-¥{{ yuan(preview.discountCent) }}</b>
          </div>
          <div class="pay">
            实付 <b class="rmb">¥{{ yuan(effectivePayCent) }}</b>
          </div>
          <div class="bal">
            钱包余额 ¥{{ yuan(preview.walletBalanceCent) }}
            <el-button link type="primary" size="small" @click="goWallet">去充值</el-button>
          </div>
        </div>

        <el-alert
          v-if="preview && !balanceOk"
          type="warning"
          :closable="false"
          show-icon
          title="余额不足，请先充值"
        />

        <el-button
          type="primary"
          class="pay-btn"
          :loading="paying"
          :disabled="!preview || !balanceOk || offShelf"
          @click="pay"
        >
          确认支付
        </el-button>
      </template>
      <template v-else>
        <p class="muted">请先登录后支付</p>
        <el-button type="primary" @click="router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })">
          去登录
        </el-button>
      </template>
    </template>
  </el-dialog>

  <PayPasswordDialog
    v-model="pwdVisible"
    :order-id="pendingOrderId"
    @paid="onPwdPaid"
    @cancelled="onPwdCancelled"
  />
</template>

<style scoped>
.row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.cov {
  width: 88px;
  height: 88px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
}
.meta {
  flex: 1;
  min-width: 0;
}
.meta .t {
  font-weight: 600;
  font-size: 15px;
  line-height: 1.4;
}
.meta .p {
  color: #ff2442;
  font-size: 18px;
  font-weight: 700;
  margin-top: 8px;
}
.qty {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.stk {
  font-size: 12px;
  color: #999;
}
.hint {
  margin-bottom: 10px;
}
.off-alert {
  margin-bottom: 12px;
}
.claim-row {
  margin-bottom: 12px;
}
.use-cp {
  margin-bottom: 12px;
}
.sum {
  background: #f8f8f8;
  border-radius: 8px;
  padding: 12px;
  font-size: 14px;
  line-height: 1.8;
  margin-bottom: 12px;
}
.sum .disc {
  color: #67c23a;
}
.sum .pay {
  margin-top: 4px;
}
.rmb {
  color: #ff2442;
  font-size: 18px;
}
.bal {
  font-size: 13px;
  color: #666;
  margin-top: 6px;
}
.pay-btn {
  width: 100%;
  margin-top: 4px;
}
.muted {
  color: #999;
  font-size: 14px;
}
.err-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: #666;
}
.err-desc code {
  font-size: 12px;
  background: #f0f0f0;
  padding: 0 4px;
  border-radius: 4px;
}
</style>
