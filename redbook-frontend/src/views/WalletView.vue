<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { walletBalance, walletRechargeAlipay } from '../api'

const balanceCent = ref(0)
const loading = ref(false)
const rechargeYuan = ref(10)
const paying = ref(false)

function yuan(cent) {
  return ((cent || 0) / 100).toFixed(2)
}

async function loadBalance() {
  loading.value = true
  try {
    const { data } = await walletBalance()
    balanceCent.value = data?.balanceCent ?? 0
  } catch {
    balanceCent.value = 0
  } finally {
    loading.value = false
  }
}

async function doRecharge() {
  const y = Number(rechargeYuan.value)
  if (!Number.isFinite(y) || y <= 0) {
    ElMessage.warning('请输入有效充值金额（元）')
    return
  }
  const amountCent = Math.round(y * 100)
  if (amountCent < 1) {
    ElMessage.warning('充值金额至少 0.01 元')
    return
  }
  paying.value = true
  try {
    await walletRechargeAlipay({ amountCent })
    ElMessage.success('充值成功（模拟入账）')
    await loadBalance()
  } catch {
    /* http 已提示 */
  } finally {
    paying.value = false
  }
}

onMounted(loadBalance)
</script>

<template>
  <div class="page">
    <header class="bar">
      <span class="tit">我的钱包</span>
    </header>

    <div class="card balance-card">
      <div class="label">账户余额（元）</div>
      <div v-if="loading" class="amount muted">加载中…</div>
      <div v-else class="amount">¥{{ yuan(balanceCent) }}</div>
      <div class="hint">余额用于集市购物车结算，与优惠券可同时使用</div>
      <el-button type="primary" plain round size="small" @click="loadBalance">刷新余额</el-button>
    </div>

    <div class="card recharge-card">
      <div class="sec-title">模拟充值</div>
      <p class="tip">当前为开发用假接口：点击后立即增加钱包余额并写入充值流水，不调用真实支付宝。</p>
      <div class="row">
        <span class="lbl">金额（元）</span>
        <el-input-number
          v-model="rechargeYuan"
          :min="0.01"
          :max="500000"
          :precision="2"
          :step="10"
          controls-position="right"
          class="inp"
        />
      </div>
      <el-button type="primary" class="btn-pay" :loading="paying" @click="doRecharge">确认充值</el-button>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100%;
  background: #f5f5f5;
  padding: 0 16px 32px;
}
.bar {
  display: flex;
  align-items: center;
  margin: -8px -8px 16px;
  padding: 8px 0 12px;
  border-bottom: 1px solid #eee;
  background: #f5f5f5;
}
.tit {
  font-size: 17px;
  font-weight: 600;
  color: #333;
}
.card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 16px;
  margin-bottom: 14px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.balance-card {
  background: linear-gradient(135deg, #fff5f7 0%, #fff 50%);
  border: 1px solid #ffe0e8;
}
.label {
  font-size: 13px;
  color: #888;
}
.amount {
  font-size: 28px;
  font-weight: 700;
  color: #ff2442;
  margin: 8px 0 10px;
}
.amount.muted {
  font-size: 16px;
  color: #999;
  font-weight: 500;
}
.hint {
  font-size: 12px;
  color: #999;
  margin-bottom: 12px;
  line-height: 1.5;
}
.sec-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #333;
}
.tip {
  font-size: 12px;
  color: #999;
  line-height: 1.5;
  margin: 0 0 14px;
}
.row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.lbl {
  font-size: 14px;
  color: #333;
  white-space: nowrap;
}
.inp {
  flex: 1;
  max-width: 200px;
}
.btn-pay {
  width: 100%;
  border-radius: 22px;
}
</style>
