<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { groupMeta, groupUpdateProfile, fetchMe } from '../api'
import { formatOssUploadError, uploadAvatarViaApi } from '../utils/ossUpload'
import { beforeUploadAvatarImage } from '../utils/avatarImageUpload'

const DEFAULT_AVATAR =
  'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const route = useRoute()
const router = useRouter()
const groupId = computed(() => route.params.groupId)

const myId = ref(null)
const name = ref('')
/** 提交给后端的自定义头像 URL，空字符串表示不设自定义（用群主头像） */
const customAvatarUrl = ref('')
const uploading = ref(false)
const saving = ref(false)
const loaded = ref(false)
const meta = ref(null)

const previewUrl = computed(() => {
  if (customAvatarUrl.value && String(customAvatarUrl.value).trim()) {
    return customAvatarUrl.value.trim()
  }
  return meta.value?.ownerAvatarUrl || DEFAULT_AVATAR
})

const isOwner = computed(
  () => myId.value != null && meta.value?.ownerId === myId.value
)

async function loadMe() {
  try {
    const { data } = await fetchMe()
    myId.value = data?.user?.id
  } catch {
    myId.value = null
  }
}

async function load() {
  loaded.value = false
  try {
    const { data } = await groupMeta(groupId.value)
    meta.value = data
    if (data?.ownerId !== myId.value) {
      ElMessage.warning('仅群主可编辑')
      router.replace({ name: 'group-chat', params: { groupId: groupId.value } })
      return
    }
    name.value = data?.name || ''
    customAvatarUrl.value = data?.customAvatarUrl ? String(data.customAvatarUrl) : ''
  } catch {
    meta.value = null
    router.replace({ name: 'chat' })
    return
  } finally {
    loaded.value = true
  }
}

function onBeforeGroupAvatarUpload(rawFile) {
  return beforeUploadAvatarImage(rawFile, ElMessage)
}

async function handleUpload(opt) {
  const { file, onError } = opt
  const raw = file.raw ?? file
  uploading.value = true
  try {
    const data = await uploadAvatarViaApi(raw)
    customAvatarUrl.value = data.publicUrl
    ElMessage.success('头像已更新')
    opt.onSuccess?.(data)
  } catch (e) {
    ElMessage.error(formatOssUploadError(e))
    onError(e)
  } finally {
    uploading.value = false
  }
}

function restoreDefaultAvatar() {
  customAvatarUrl.value = ''
  ElMessage.success('已改为默认头像（与群主头像一致）')
}

async function save() {
  const n = name.value.trim()
  if (!n) {
    ElMessage.warning('请填写群名称')
    return
  }
  saving.value = true
  try {
    await groupUpdateProfile(groupId.value, {
      name: n,
      avatar: customAvatarUrl.value.trim(),
    })
    ElMessage.success('已保存')
    router.push({ name: 'group-chat', params: { groupId: groupId.value } })
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadMe()
  await load()
})
</script>

<template>
  <div v-if="loaded && isOwner" class="page">
    <h1 class="title">编辑群资料</h1>
    <p class="hint">修改群名称与群头像，保存后对所有成员生效。</p>

    <div class="av-block">
      <el-upload
        class="av-uploader"
        :show-file-list="false"
        accept="image/*"
        :before-upload="onBeforeGroupAvatarUpload"
        :http-request="handleUpload"
        :disabled="uploading"
      >
        <img v-if="previewUrl" class="av-preview" :src="previewUrl" alt="" />
        <div v-else class="av-placeholder">
          <el-icon :size="28"><Plus /></el-icon>
        </div>
      </el-upload>
      <el-button text type="primary" class="restore-btn" @click="restoreDefaultAvatar">
        使用默认头像
      </el-button>
    </div>

    <div class="field">
      <div class="label">群名称</div>
      <el-input v-model="name" maxlength="128" show-word-limit placeholder="群名称" />
    </div>

    <div class="actions">
      <el-button @click="router.back()">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </div>
  </div>
  <div v-else-if="loaded" class="page empty">无法编辑</div>
</template>

<style scoped>
.page {
  max-width: 480px;
  margin: 0 auto;
  padding: 16px;
}
.title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
}
.hint {
  margin: 0 0 20px;
  font-size: 13px;
  color: #888;
  line-height: 1.5;
}
.av-block {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 24px;
}
.av-uploader :deep(.el-upload) {
  border: 1px dashed #dcdfe6;
  border-radius: 12px;
  cursor: pointer;
  overflow: hidden;
  width: 96px;
  height: 96px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
}
.av-preview {
  width: 96px;
  height: 96px;
  object-fit: cover;
  display: block;
}
.av-placeholder {
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}
.restore-btn {
  padding-left: 0;
}
.field {
  margin-bottom: 24px;
}
.label {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
}
.actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
.empty {
  text-align: center;
  color: #999;
  padding: 40px;
}
</style>
