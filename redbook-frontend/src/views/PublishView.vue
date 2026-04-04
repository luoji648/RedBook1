<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { saveNote, ossPresign, productList } from '../api'

const router = useRouter()

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
    const { data } = await ossPresign(ext)
    const putRes = await fetch(data.uploadUrl, {
      method: 'PUT',
      body: file,
      headers: { 'Content-Type': file.type || 'application/octet-stream' },
    })
    if (!putRes.ok) throw new Error('上传失败')
    mediaUrls.value.push(data.publicUrl)
    ElMessage.success('已上传')
  } catch (e) {
    ElMessage.error(e.message || '上传失败')
    onError(e)
  } finally {
    uploading.value = false
  }
}

async function submit() {
  try {
    await saveNote({
      title: title.value,
      content: content.value,
      type: type.value,
      visibility: visibility.value,
      mediaUrls: mediaUrls.value,
      productIds: productIds.value,
      publish: publish.value,
    })
    ElMessage.success(publish.value ? '发布成功' : '已保存草稿')
    router.push({ name: 'me' })
  } catch {
    /* */
  }
}

function removeMedia(i) {
  mediaUrls.value.splice(i, 1)
}

onMounted(loadProducts)
</script>

<template>
  <div class="pub">
    <h2>发布笔记</h2>
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
h2 {
  margin: 0 0 16px;
  font-size: 18px;
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
