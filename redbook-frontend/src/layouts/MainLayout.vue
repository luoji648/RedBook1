<script setup>
import { useRoute, useRouter } from 'vue-router'
import { House, StarFilled, Plus, ChatDotRound, User, ShoppingCart } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const r = useRoute()
const auth = useAuthStore()

function go(name) {
  router.push({ name })
}

function active(name) {
  if (name === 'discover') return r.name === 'discover'
  return r.name === name
}
</script>

<template>
  <div class="layout">
    <header class="top">
      <span class="logo" @click="go('discover')">RedBook</span>
      <div class="actions">
        <el-button
          v-if="auth.isLogin"
          text
          circle
          @click="go('cart')"
          title="购物车"
        >
          <el-icon :size="22"><ShoppingCart /></el-icon>
        </el-button>
      </div>
    </header>

    <main class="main">
      <router-view />
    </main>

    <nav class="tabbar">
      <div :class="['tab', { on: active('discover') }]" @click="go('discover')">
        <el-icon :size="22"><House /></el-icon>
        <span>发现</span>
      </div>
      <div :class="['tab', { on: active('follow') }]" @click="go('follow')">
        <el-icon :size="22"><StarFilled /></el-icon>
        <span>关注</span>
      </div>
      <div class="tab mid" @click="go('publish')">
        <div class="fab">
          <el-icon :size="26"><Plus /></el-icon>
        </div>
        <span>发布</span>
      </div>
      <div :class="['tab', { on: active('chat') }]" @click="go('chat')">
        <el-icon :size="22"><ChatDotRound /></el-icon>
        <span>消息</span>
      </div>
      <div :class="['tab', { on: active('me') }]" @click="go('me')">
        <el-icon :size="22"><User /></el-icon>
        <span>我</span>
      </div>
    </nav>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
  padding-bottom: 56px;
}
.top {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  background: #fff;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 20;
}
.logo {
  font-weight: 700;
  font-size: 18px;
  color: #ff2442;
  cursor: pointer;
}
.main {
  flex: 1;
  max-width: 640px;
  width: 100%;
  margin: 0 auto;
  padding: 10px 10px 20px;
  box-sizing: border-box;
}
.tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 52px;
  display: flex;
  background: #fff;
  border-top: 1px solid #eee;
  z-index: 30;
  padding-bottom: env(safe-area-inset-bottom);
}
.tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: #999;
  gap: 2px;
  cursor: pointer;
}
.tab.on {
  color: #ff2442;
}
.tab.mid .fab {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b6b, #ff2442);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: -16px;
  box-shadow: 0 4px 12px rgba(255, 36, 66, 0.35);
}
</style>
