<script setup>
import { ref, watch, computed } from 'vue'
import { orderPay } from '../api'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  orderId: { type: [Number, String], default: null },
})

const emit = defineEmits(['update:modelValue', 'paid', 'cancelled'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const password = ref('123456')
const paying = ref(false)

watch(
  () => props.modelValue,
  (v) => {
    if (v) password.value = '123456'
  }
)

async function confirmPay() {
  const id = props.orderId
  if (id == null || id === '') return
  paying.value = true
  try {
    await orderPay(id, { payPassword: password.value })
    emit('paid', id)
    visible.value = false
  } catch {
    /* ElMessage 由 http 层处理 */
  } finally {
    paying.value = false
  }
}

function back() {
  const id = props.orderId
  visible.value = false
  emit('cancelled', id)
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="确认支付"
    width="90%"
    max-width="400px"
    align-center
    destroy-on-close
    :close-on-click-modal="false"
  >
    <p class="tip">请输入支付密码完成扣款（模拟环境默认已填 <code>123456</code>）</p>
    <el-input
      v-model="password"
      type="password"
      show-password
      placeholder="支付密码"
      maxlength="32"
      @keyup.enter="confirmPay"
    />
    <template #footer>
      <el-button @click="back">返回</el-button>
      <el-button type="primary" :loading="paying" @click="confirmPay">确认支付</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.tip {
  font-size: 14px;
  color: #666;
  margin: 0 0 12px;
  line-height: 1.5;
}
.tip code {
  font-size: 13px;
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
}
</style>
