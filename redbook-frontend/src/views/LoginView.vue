<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { sendCode, login, loginByPassword } from '../api'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const mode = ref('code')
const phone = ref('')
const code = ref('')
const password = ref('')
const sending = ref(false)
const sec = ref(0)
let timer = null

async function onSend() {
  if (!/^1\d{10}$/.test(phone.value)) {
    ElMessage.warning('请输入正确手机号')
    return
  }
  sending.value = true
  try {
    await sendCode(phone.value)
    ElMessage.success('验证码已发送（请查看后端日志）')
    sec.value = 60
    timer = setInterval(() => {
      sec.value -= 1
      if (sec.value <= 0) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  } catch {
    /* http 已提示 */
  } finally {
    sending.value = false
  }
}

async function onSubmitCode() {
  if (!/^1\d{10}$/.test(phone.value)) {
    ElMessage.warning('请输入正确手机号')
    return
  }
  if (!code.value) {
    ElMessage.warning('请输入验证码')
    return
  }
  try {
    const { data } = await login({ phone: phone.value, code: code.value })
    auth.setToken(data)
    ElMessage.success('登录成功')
    const red = route.query.redirect
    router.replace(typeof red === 'string' && red ? red : '/')
  } catch {
    /* */
  }
}

async function onSubmitPassword() {
  if (!/^1\d{10}$/.test(phone.value)) {
    ElMessage.warning('请输入正确手机号')
    return
  }
  if (!password.value) {
    ElMessage.warning('请输入密码')
    return
  }
  try {
    const { data } = await loginByPassword({
      phone: phone.value,
      password: password.value,
    })
    auth.setToken(data)
    ElMessage.success('登录成功')
    const red = route.query.redirect
    router.replace(typeof red === 'string' && red ? red : '/')
  } catch {
    /* */
  }
}
</script>

<template>
  <div class="page">
    <div class="box">
      <h1>登录</h1>
      <el-tabs v-model="mode" class="tabs">
        <el-tab-pane label="验证码登录" name="code">
          <p class="tip">验证码请在后端日志中查看（模拟短信）</p>
          <el-input v-model="phone" placeholder="手机号" maxlength="11" />
          <div class="row">
            <el-input v-model="code" placeholder="验证码" maxlength="6" />
            <el-button :disabled="sec > 0 || sending" @click="onSend">
              {{ sec > 0 ? `${sec}s` : '获取验证码' }}
            </el-button>
          </div>
          <el-button type="primary" class="full" @click="onSubmitCode">登录</el-button>
        </el-tab-pane>
        <el-tab-pane label="密码登录" name="password">
          <p class="tip">使用已注册手机号与密码登录</p>
          <el-input v-model="phone" placeholder="手机号" maxlength="11" />
          <el-input
            v-model="password"
            type="password"
            placeholder="密码"
            show-password
            class="pwd"
            @keyup.enter="onSubmitPassword"
          />
          <el-button type="primary" class="full" @click="onSubmitPassword">登录</el-button>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #fff5f5, #fff);
}
.box {
  width: 100%;
  max-width: 360px;
  padding: 24px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(255, 36, 66, 0.12);
}
h1 {
  margin: 0 0 8px;
  font-size: 22px;
}
.tabs {
  margin-top: 4px;
}
.tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}
.tip {
  font-size: 12px;
  color: #999;
  margin-bottom: 16px;
}
.el-input {
  margin-bottom: 12px;
}
.pwd {
  margin-bottom: 16px;
}
.row {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.row .el-input {
  flex: 1;
  margin-bottom: 0;
}
.full {
  width: 100%;
}
</style>
