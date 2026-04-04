<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  fetchMe,
  fetchUserInfo,
  updateProfile,
  updateUserInfo,
  changePassword,
  logout,
} from '../api'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const nickName = ref('')
const icon = ref('')
const city = ref('')
const introduce = ref('')
const gender = ref(true)
const birthday = ref('')
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

async function saveInfo() {
  try {
    await updateUserInfo({
      city: city.value,
      introduce: introduce.value,
      gender: gender.value,
      birthday: birthday.value || null,
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
      <el-form-item label="头像 URL">
        <el-input v-model="icon" placeholder="可填 OSS 公开地址" />
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
</style>
