<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  fetchMe,
  fetchUserInfo,
  updateProfile,
  updateUserInfo,
  changePassword,
  logout,
  ossPresign,
} from '../api'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const nickName = ref('')
const icon = ref('')
const uploadingAvatar = ref(false)
const city = ref('')
const introduce = ref('')
const gender = ref(true)
const birthday = ref('')
const collectPublic = ref(false)
const likePublic = ref(false)
const oldPassword = ref('')
const newPassword = ref('')

async function load() {
  try {
    const { data: m } = await fetchMe()
    nickName.value = m.user?.nickName || ''
    icon.value = m.user?.icon || ''
    const { data: info } = await fetchUserInfo()
    if (info) {
      city.value = info.city || ''
      introduce.value = info.introduce || ''
      gender.value = info.gender === true || info.gender === 1
      birthday.value = info.birthday || ''
      collectPublic.value = info.collectPublic === true || info.collectPublic === 1
      likePublic.value = info.likePublic === true || info.likePublic === 1
    }
  } catch {
    /* */
  }
}

async function saveProfile() {
  try {
    await updateProfile({ nickName: nickName.value, icon: icon.value })
    ElMessage.success('已更新资料')
  } catch {
    /* */
  }
}

async function handleAvatarUpload(opt) {
  const { file, onError } = opt
  const raw = file.raw ?? file
  uploadingAvatar.value = true
  try {
    const ext = '.' + (String(raw.name || 'image.jpg').split('.').pop() || 'jpg')
    const contentType = raw.type || 'application/octet-stream'
    const { data } = await ossPresign(ext, contentType)
    const headers = { 'Content-Type': contentType }
    if (data.putAclPublicRead) {
      headers['x-oss-object-acl'] = 'public-read'
    }
    const putRes = await fetch(data.uploadUrl, {
      method: 'PUT',
      body: raw,
      headers,
    })
    if (!putRes.ok) throw new Error('上传失败')
    icon.value = data.publicUrl
    ElMessage.success('上传成功，请点击「保存资料」')
    opt.onSuccess?.(data)
  } catch (e) {
    ElMessage.error(e.message || '上传失败')
    onError(e)
  } finally {
    uploadingAvatar.value = false
  }
}

function clearAvatar() {
  icon.value = ''
  ElMessage.success('已清除，保存后生效')
}

async function saveInfo() {
  try {
    await updateUserInfo({
      city: city.value,
      introduce: introduce.value,
      gender: gender.value,
      birthday: birthday.value || null,
      collectPublic: collectPublic.value,
      likePublic: likePublic.value,
    })
    ElMessage.success('已保存扩展资料')
  } catch {
    /* */
  }
}

async function savePwd() {
  try {
    await changePassword({
      oldPassword: oldPassword.value,
      newPassword: newPassword.value,
    })
    ElMessage.success('密码已修改，请重新登录')
    auth.clearToken()
    router.replace({ name: 'login' })
  } catch {
    /* */
  }
}

async function doLogout() {
  try {
    await logout()
  } catch {
    /* */
  }
  auth.clearToken()
  ElMessage.success('已退出')
  router.replace({ name: 'discover' })
}

onMounted(load)
</script>

<template>
  <div class="set">
    <h2>账号设置</h2>
    <el-divider>基本资料</el-divider>
    <el-form label-width="88px">
      <el-form-item label="昵称">
        <el-input v-model="nickName" />
      </el-form-item>
      <el-form-item label="头像">
        <div class="avatar-row">
          <el-upload
            class="av-uploader"
            :show-file-list="false"
            accept="image/*"
            :http-request="handleAvatarUpload"
            :disabled="uploadingAvatar"
          >
            <img v-if="icon" class="av-preview" :src="icon" alt="" />
            <div v-else class="av-placeholder">
              <el-icon :size="28"><Plus /></el-icon>
              <span class="av-hint">{{ uploadingAvatar ? '上传中…' : '点击上传' }}</span>
            </div>
          </el-upload>
          <el-button
            v-if="icon"
            text
            type="primary"
            class="av-clear"
            :disabled="uploadingAvatar"
            @click="clearAvatar"
          >
            清除
          </el-button>
        </div>
      </el-form-item>
      <el-button type="primary" @click="saveProfile">保存资料</el-button>
    </el-form>

    <el-divider>扩展资料</el-divider>
    <el-form label-width="88px">
      <el-form-item label="城市">
        <el-input v-model="city" />
      </el-form-item>
      <el-form-item label="简介">
        <el-input v-model="introduce" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item label="性别">
        <el-switch v-model="gender" active-text="男" inactive-text="女" />
      </el-form-item>
      <el-form-item label="生日">
        <el-input v-model="birthday" placeholder="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="公开收藏">
        <el-switch v-model="collectPublic" active-text="公开" inactive-text="仅自己" />
        <span class="tip">开启后，其他用户可在你的主页查看收藏</span>
      </el-form-item>
      <el-form-item label="公开赞过">
        <el-switch v-model="likePublic" active-text="公开" inactive-text="仅自己" />
        <span class="tip">开启后，其他用户可在你的主页查看赞过</span>
      </el-form-item>
      <el-button type="primary" @click="saveInfo">保存扩展资料</el-button>
    </el-form>

    <el-divider>安全</el-divider>
    <el-form label-width="88px">
      <el-form-item label="原密码">
        <el-input v-model="oldPassword" type="password" show-password placeholder="未设置过可留空" />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="newPassword" type="password" show-password />
      </el-form-item>
      <el-button type="warning" @click="savePwd">修改密码</el-button>
    </el-form>

    <el-divider />
    <el-button type="danger" plain class="full" @click="doLogout">退出登录</el-button>
  </div>
</template>

<style scoped>
.set {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}
h2 {
  margin: 0 0 8px;
}
.full {
  width: 100%;
}
.tip {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #999;
}
.avatar-row {
  display: flex;
  align-items: flex-end;
  gap: 12px;
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
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #999;
  font-size: 12px;
  padding: 8px;
  text-align: center;
}
.av-hint {
  line-height: 1.2;
}
.av-clear {
  align-self: center;
}
</style>
