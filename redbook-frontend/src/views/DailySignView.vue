<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { signIn, signCount } from '../api'

const streak = ref(0)
const loading = ref(false)
const signing = ref(false)

async function loadStreak() {
  loading.value = true
  try {
    const { data } = await signCount()
    streak.value = Number(data) || 0
  } catch {
    streak.value = 0
  } finally {
    loading.value = false
  }
}

async function doSign() {
  signing.value = true
  try {
    const { data } = await signIn()
    if (data?.firstSignToday) {
      ElMessage.success('签到成功')
      await loadStreak()
    } else {
      ElMessage.info('您今天已经签到过~请明天再来~')
    }
  } catch {
    /* http 层已提示 */
  } finally {
    signing.value = false
  }
}

onMounted(loadStreak)
</script>

<template>
  <div class="page">
    <header class="bar">
      <span class="tit">每日签到</span>
    </header>

    <div class="card">
      <div class="label">本月连续签到（截至今日）</div>
      <div v-if="loading" class="num muted">加载中…</div>
      <div v-else class="num">{{ streak }}<span class="unit">天</span></div>
      <p class="hint">进入本页时从服务端同步本月签到记录；签到成功后自动刷新连续天数。</p>
      <div class="actions">
        <el-button type="primary" round :loading="signing" @click="doSign">签到</el-button>
        <el-button round :disabled="loading" @click="loadStreak">刷新</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  padding: 0 16px 24px;
}
.bar {
  padding: 12px 0 16px;
}
.tit {
  font-size: 18px;
  font-weight: 700;
}
.card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 16px;
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.06);
}
.label {
  font-size: 13px;
  color: #666;
}
.num {
  font-size: 40px;
  font-weight: 800;
  margin: 12px 0 8px;
  line-height: 1.1;
  color: #e17055;
}
.num .unit {
  font-size: 16px;
  font-weight: 600;
  margin-left: 4px;
  color: #333;
}
.muted {
  color: #999;
  font-size: 20px;
  font-weight: 600;
}
.hint {
  font-size: 12px;
  color: #999;
  line-height: 1.5;
  margin: 0 0 16px;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
