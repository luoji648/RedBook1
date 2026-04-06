<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { productGet, productSave, ossPresign } from '../api'
import { formatOssUploadError, ossPutFailMessage } from '../utils/ossUpload'

const route = useRoute()
const router = useRouter()

const editingId = ref(null)
const title = ref('')
const cover = ref('')
const priceYuan = ref('')
const stock = ref(0)
const status = ref(1)
const uploading = ref(false)

async function handleCoverUpload({ file, onError }) {
  uploading.value = true
  try {
    const ext = '.' + (file.name.split('.').pop() || 'jpg')
    const contentType = file.type || 'application/octet-stream'
    const { data } = await ossPresign(ext, contentType)
    const headers = { 'Content-Type': contentType }
    if (data.putAclPublicRead) {
      headers['x-oss-object-acl'] = 'public-read'
    }
    const putRes = await fetch(data.uploadUrl, {
      method: 'PUT',
      body: file,
      headers,
    })
    if (!putRes.ok) throw new Error(ossPutFailMessage(putRes))
    cover.value = data.publicUrl || ''
    ElMessage.success('封面已上传')
  } catch (e) {
    ElMessage.error(formatOssUploadError(e))
    onError(e)
  } finally {
    uploading.value = false
  }
}

function removeCover() {
  cover.value = ''
}

function resetForm() {
  editingId.value = null
  title.value = ''
  cover.value = ''
  priceYuan.value = ''
  stock.value = 0
  status.value = 1
}

async function loadForEdit() {
  const qid = route.query.id
  if (!qid) {
    resetForm()
    return
  }
  const id = Number(qid)
  if (Number.isNaN(id)) {
    resetForm()
    return
  }
  editingId.value = id
  try {
    const { data } = await productGet(id)
    if (!data) {
      resetForm()
      return
    }
    title.value = data.title || ''
    cover.value = data.cover || ''
    priceYuan.value = data.priceCent != null ? (data.priceCent / 100).toFixed(2) : ''
    stock.value = data.stock ?? 0
    status.value = data.status ?? 1
  } catch {
    editingId.value = null
    resetForm()
  }
}

async function submit() {
  const t = String(title.value || '').trim()
  if (!t) {
    ElMessage.warning('请填写标题')
    return
  }
  const yuan = Number(priceYuan.value)
  if (!Number.isFinite(yuan) || yuan < 0) {
    ElMessage.warning('请填写有效价格（元）')
    return
  }
  const priceCent = Math.round(yuan * 100)
  const st = Number(stock.value)
  if (!Number.isInteger(st) || st < 0) {
    ElMessage.warning('请填写有效库存（整数）')
    return
  }
  const body = {
    title: t,
    cover: cover.value || '',
    priceCent,
    stock: st,
    status: status.value,
  }
  if (editingId.value != null) {
    body.id = editingId.value
  }
  try {
    await productSave(body)
    ElMessage.success(editingId.value != null ? '已更新商品' : '已创建商品')
    router.push({ name: 'market' })
  } catch {
    /* http 已提示 */
  }
}

watch(
  () => route.query.id,
  () => {
    loadForEdit()
  },
  { immediate: false }
)

onMounted(() => {
  loadForEdit()
})
</script>

<template>
  <div class="wrap">
    <h2>{{ editingId != null ? '编辑商品' : '发布商品' }}</h2>
    <el-form label-position="top">
      <el-form-item label="标题">
        <el-input v-model="title" placeholder="商品标题" maxlength="200" show-word-limit />
      </el-form-item>
      <el-form-item label="封面（OSS 预签名上传）">
        <el-upload
          :show-file-list="false"
          :http-request="handleCoverUpload"
          :disabled="uploading"
          accept="image/*"
        >
          <el-button type="primary" plain :loading="uploading">上传封面</el-button>
        </el-upload>
        <div v-if="cover" class="cover-row">
          <img :src="cover" alt="" class="cover-img" />
          <el-button text type="danger" size="small" @click="removeCover">移除</el-button>
        </div>
      </el-form-item>
      <el-form-item label="价格（元）">
        <el-input v-model="priceYuan" placeholder="如 9.99" inputmode="decimal" />
      </el-form-item>
      <el-form-item label="库存">
        <el-input-number v-model="stock" :min="0" :step="1" controls-position="right" style="width: 100%" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="status">
          <el-radio :value="1">上架</el-radio>
          <el-radio :value="0">下架</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-button type="primary" size="large" class="full" @click="submit">保存</el-button>
    </el-form>
  </div>
</template>

<style scoped>
.wrap {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  max-width: 520px;
  margin: 0 auto;
}
h2 {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
}
.full {
  width: 100%;
  margin-top: 8px;
}
.cover-row {
  margin-top: 10px;
  display: flex;
  align-items: flex-end;
  gap: 10px;
}
.cover-img {
  max-width: 160px;
  max-height: 160px;
  border-radius: 8px;
  object-fit: cover;
}
</style>
