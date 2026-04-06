<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { productMy, productUpdateStock } from '../api'

const router = useRouter()

const list = ref([])
const current = ref(1)
const total = ref(0)
const loading = ref(false)
/** 每行待提交的库存，与列表同步 */
const stockDraft = ref({})
const savingId = ref(null)

function yuan(cent) {
  return ((cent || 0) / 100).toFixed(2)
}

function statusText(s) {
  if (s === 1) return '上架'
  if (s === 0) return '下架'
  return String(s)
}

function initDraftForRows(rows) {
  const m = {}
  for (const r of rows) {
    m[r.id] = r.stock ?? 0
  }
  stockDraft.value = m
}

function mergeDraftForNewRows(rows) {
  const m = { ...stockDraft.value }
  for (const r of rows) {
    if (m[r.id] === undefined) {
      m[r.id] = r.stock ?? 0
    }
  }
  stockDraft.value = m
}

async function load(append) {
  if (!append) {
    current.value = 1
  }
  loading.value = true
  try {
    const { data, total: t } = await productMy({ current: current.value, size: 10 })
    const rows = data || []
    total.value = t || 0
    if (append) {
      list.value = [...list.value, ...rows]
      mergeDraftForNewRows(rows)
    } else {
      list.value = rows
      initDraftForRows(rows)
    }
  } catch {
    if (!append) {
      list.value = []
      total.value = 0
      stockDraft.value = {}
    }
  } finally {
    loading.value = false
  }
}

function nextPage() {
  if (list.value.length >= total.value) return
  current.value += 1
  load(true)
}

function goEdit(row) {
  router.push({ name: 'product-save', query: { id: String(row.id) } })
}

function goDetail(row) {
  router.push({ name: 'product-detail', params: { id: String(row.id) } })
}

async function submitStock(row) {
  if (row.status !== 1) {
    ElMessage.warning('仅上架商品可在此快捷改库存，请先编辑商品并上架')
    return
  }
  const id = row.id
  const st = Number(stockDraft.value[id])
  if (!Number.isInteger(st) || st < 0) {
    ElMessage.warning('请输入有效库存（非负整数）')
    return
  }
  if (st === (row.stock ?? 0)) {
    ElMessage.info('库存未变化')
    return
  }
  savingId.value = id
  try {
    await productUpdateStock(id, { stock: st })
    row.stock = st
    ElMessage.success('库存已更新')
  } catch {
    /* http 已提示 */
  } finally {
    savingId.value = null
  }
}

onMounted(() => {
  load(false)
})
</script>

<template>
  <div class="wrap">
    <div class="head">
      <h2>我的商品</h2>
      <el-button type="primary" plain @click="router.push({ name: 'product-save' })">发布新商品</el-button>
    </div>
    <p class="hint">已上架商品可直接修改库存并保存；下架商品请点「编辑」在发布页修改（含库存）。</p>

    <div v-loading="loading" class="list">
      <div v-for="row in list" :key="row.id" class="card">
        <div class="thumb" @click="goDetail(row)">
          <img v-if="row.cover" :src="row.cover" alt="" />
          <div v-else class="no-cover">无图</div>
        </div>
        <div class="main">
          <div class="title-row">
            <span class="title" @click="goDetail(row)">{{ row.title }}</span>
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ statusText(row.status) }}</el-tag>
          </div>
          <div class="meta">¥{{ yuan(row.priceCent) }} · 当前库存 {{ row.stock ?? 0 }}</div>
          <div class="actions">
            <el-input-number
              v-model="stockDraft[row.id]"
              :min="0"
              :step="1"
              :disabled="row.status !== 1"
              controls-position="right"
              size="small"
              class="stock-input"
            />
            <el-button
              type="primary"
              size="small"
              :loading="savingId === row.id"
              :disabled="row.status !== 1"
              @click="submitStock(row)"
            >
              保存库存
            </el-button>
            <el-button size="small" @click="goEdit(row)">编辑商品</el-button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="!loading && list.length === 0" class="empty">暂无商品，去发布一个吧</div>
    <div v-else-if="list.length > 0 && list.length < total" class="more">
      <el-button text type="primary" @click="nextPage">加载更多</el-button>
    </div>
    <div v-else-if="list.length > 0 && list.length >= total" class="more muted">没有更多了</div>
  </div>
</template>

<style scoped>
.wrap {
  max-width: 640px;
  margin: 0 auto;
  padding: 16px;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}
h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}
.hint {
  margin: 0 0 16px;
  font-size: 13px;
  color: #888;
  line-height: 1.5;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.card {
  display: flex;
  gap: 12px;
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.thumb {
  width: 88px;
  height: 88px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  background: #f5f5f5;
}
.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.no-cover {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #999;
}
.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.title {
  font-weight: 600;
  font-size: 15px;
  cursor: pointer;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.meta {
  font-size: 13px;
  color: #666;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}
.stock-input {
  width: 120px;
}
.empty,
.more {
  text-align: center;
  padding: 24px 12px;
  font-size: 14px;
}
.muted {
  color: #999;
}
</style>
