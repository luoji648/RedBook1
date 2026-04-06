<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { saveNote, ossPresign, productList, noteDetail, noteMy } from '../api'
import { formatOssUploadError, ossPutFailMessage } from '../utils/ossUpload'

const route = useRoute()
const router = useRouter()

const editingId = ref(null)
const editingIsDraft = ref(false)

const draftDialogVisible = ref(false)
const draftLoading = ref(false)
const draftList = ref([])

const pageTitle = computed(() => {
  if (editingId.value == null) return '发布笔记'
  return editingIsDraft.value ? '编辑草稿' : '编辑笔记'
})

const title = ref('')
const content = ref('')
const visibility = ref(0)
const type = ref(0)
const publish = ref(true)
const mediaUrls = ref([])
const productIds = ref([])
const products = ref([])
const uploading = ref(false)

async function loadProducts() {
  try {
    const { data } = await productList({ current: 1, size: 100 })
    products.value = data || []
  } catch {
    products.value = []
  }
}

async function handleUpload({ file, onError }) {
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
    mediaUrls.value.push(data.publicUrl)
    ElMessage.success('已上传')
  } catch (e) {
    ElMessage.error(formatOssUploadError(e))
    onError(e)
  } finally {
    uploading.value = false
  }
}

function resetForm() {
  title.value = ''
  content.value = ''
  visibility.value = 0
  type.value = 0
  publish.value = true
  mediaUrls.value = []
  productIds.value = []
}

async function loadForEdit() {
  const qid = route.query.id
  if (!qid) {
    editingId.value = null
    editingIsDraft.value = false
    resetForm()
    return
  }
  const id = Number(qid)
  if (Number.isNaN(id)) {
    editingId.value = null
    editingIsDraft.value = false
    return
  }
  editingId.value = id
  try {
    const { data } = await noteDetail(id)
    const n = data.note
    editingIsDraft.value = n.status === 0
    title.value = n.title || ''
    content.value = n.content || ''
    type.value = n.type ?? 0
    visibility.value = n.visibility ?? 0
    publish.value = n.status === 1
    mediaUrls.value = (data.media || []).map((m) => m.url).filter(Boolean)
    productIds.value = (data.noteProducts || []).map((np) => np.productId)
  } catch {
    editingId.value = null
    editingIsDraft.value = false
    resetForm()
  }
}

const DRAFT_FETCH_PAGE_SIZE = 30
const DRAFT_FETCH_MAX_PAGES = 25

async function openDraftBox() {
  draftDialogVisible.value = true
  draftLoading.value = true
  draftList.value = []
  try {
    const out = []
    let current = 1
    let total = 0
    do {
      const { data, total: t } = await noteMy({ current, size: DRAFT_FETCH_PAGE_SIZE })
      total = Number(t) || 0
      const batch = data || []
      for (const n of batch) {
        if (n.status === 0) out.push(n)
      }
      current += 1
      if (!batch.length) break
    } while (current <= Math.ceil(total / DRAFT_FETCH_PAGE_SIZE) && current <= DRAFT_FETCH_MAX_PAGES)
    draftList.value = out
  } catch {
    draftList.value = []
  } finally {
    draftLoading.value = false
  }
}

function pickDraft(row) {
  if (!row?.id) return
  draftDialogVisible.value = false
  router.replace({ name: 'publish', query: { id: String(row.id) } })
}

function clearDraftEdit() {
  router.replace({ name: 'publish', query: {} })
}

async function submit() {
  try {
    const body = {
      title: title.value,
      content: content.value,
      type: type.value,
      visibility: visibility.value,
      mediaUrls: mediaUrls.value,
      productIds: productIds.value,
      publish: publish.value,
    }
    if (editingId.value != null) {
      body.id = editingId.value
    }
    await saveNote(body)
    ElMessage.success(publish.value ? '发布成功' : '已保存草稿')
    router.push({ name: 'me' })
  } catch {
    /* */
  }
}

function removeMedia(i) {
  mediaUrls.value.splice(i, 1)
}

watch(
  () => route.query.id,
  () => {
    loadForEdit()
  },
  { immediate: false }
)

onMounted(() => {
  loadProducts()
  loadForEdit()
})
</script>

<template>
  <div class="pub">
    <div class="pub-head">
      <h2>{{ pageTitle }}</h2>
      <div class="pub-actions">
        <el-button v-if="editingId != null" plain @click="clearDraftEdit">新建笔记</el-button>
        <el-button type="primary" plain @click="openDraftBox">草稿箱</el-button>
      </div>
    </div>
    <el-dialog v-model="draftDialogVisible" title="草稿箱" width="min(520px, 92vw)" destroy-on-close>
      <div v-loading="draftLoading" class="draft-body">
        <el-empty v-if="!draftLoading && draftList.length === 0" description="暂无草稿" />
        <el-table v-else-if="draftList.length" :data="draftList" size="small" @row-click="pickDraft">
          <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.title?.trim() ? row.title : '（无标题）' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="right">
            <template #default="{ row }">
              <el-button type="primary" link @click.stop="pickDraft(row)">继续编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
    <el-form label-position="top">
      <el-form-item label="标题">
        <el-input v-model="title" placeholder="填写标题" />
      </el-form-item>
      <el-form-item label="正文">
        <el-input v-model="content" type="textarea" :rows="8" placeholder="分享你的想法…" />
      </el-form-item>
      <el-form-item label="类型">
        <el-radio-group v-model="type">
          <el-radio :value="0">图文</el-radio>
          <el-radio :value="1">视频</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="可见范围">
        <el-select v-model="visibility" style="width: 100%">
          <el-option :value="0" label="公开" />
          <el-option :value="1" label="仅关注" />
          <el-option :value="2" label="私密" />
        </el-select>
      </el-form-item>
      <el-form-item label="图片 / 视频（OSS 预签名上传）">
        <el-upload
          :show-file-list="false"
          :http-request="handleUpload"
          :disabled="uploading"
          multiple
        >
          <el-button type="primary" plain :loading="uploading">选择文件上传</el-button>
        </el-upload>
        <div class="previews">
          <div v-for="(u, i) in mediaUrls" :key="i" class="pv">
            <img :src="u" alt="" />
            <el-button text type="danger" size="small" @click="removeMedia(i)">删除</el-button>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="关联商品（可选）">
        <el-select v-model="productIds" multiple filterable placeholder="选择商品" style="width: 100%">
          <el-option
            v-for="p in products"
            :key="p.id"
            :label="p.title"
            :value="p.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="">
        <el-checkbox v-model="publish">直接发布（否则为草稿）</el-checkbox>
      </el-form-item>
      <el-button type="primary" size="large" class="full" @click="submit">提交</el-button>
    </el-form>
  </div>
</template>

<style scoped>
.pub {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}
.pub-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.pub-head h2 {
  margin: 0;
  font-size: 18px;
}
.pub-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.draft-body {
  min-height: 120px;
}
.previews {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
.pv {
  width: 88px;
}
.pv img {
  width: 88px;
  height: 88px;
  object-fit: cover;
  border-radius: 8px;
  display: block;
}
.full {
  width: 100%;
}
</style>
