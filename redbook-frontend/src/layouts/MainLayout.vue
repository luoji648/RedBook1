<script setup>
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  House,
  Shop,
  Plus,
  ChatDotRound,
  User,
  ShoppingCart,
  ArrowLeft,
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { useMessageUnreadStore } from '../stores/messageUnread'

const router = useRouter()
const r = useRoute()
const auth = useAuthStore()
const msgUnread = useMessageUnreadStore()

const chatTabBadge = computed(() => {
  const n = msgUnread.totalUnread
  if (n <= 0) return ''
  return n > 99 ? '99+' : String(n)
})

watch(
  () => auth.token,
  (t) => {
    if (t) msgUnread.refresh()
    else msgUnread.reset()
  },
  { immediate: true },
)

const needsTopBack = computed(() =>
  [
    'settings',
    'publish',
    'cart',
    'orders',
    'notice-category',
    'market-coupons',
    'market-wallet',
    'product-save',
    'product-footprint',
    'daily-sign',
    'group-edit',
  ].includes(String(r.name)),
)

function goBack() {
  router.back()
}

function go(name) {
  router.push({ name })
}

function active(name) {
  if (name === 'discover') return r.name === 'discover'
  if (name === 'market') return r.name === 'market'
  if (name === 'chat')
    return (
      r.name === 'chat' ||
      r.name === 'chat-thread' ||
      r.name === 'group-chat' ||
      r.name === 'group-edit' ||
      r.name === 'notice-category'
    )
  return r.name === name
}
</script>

<template>
  <div class="layout">
    <header class="top">
      <div class="top-left">
        <el-button
          v-if="needsTopBack"
          text
          circle
          class="back-btn"
          title="返回"
          @click="goBack"
        >
          <el-icon :size="22"><ArrowLeft /></el-icon>
        </el-button>
        <span class="logo" @click="go('discover')">RedBook</span>
      </div>
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
      <div :class="['tab', { on: active('market') }]" @click="go('market')">
        <el-icon :size="22"><Shop /></el-icon>
        <span>集市</span>
      </div>
      <div class="tab mid" @click="go('publish')">
        <div class="fab">
          <el-icon :size="26"><Plus /></el-icon>
        </div>
        <span>发布</span>
      </div>
      <div :class="['tab', { on: active('chat') }]" @click="go('chat')">
        <div class="tab-ic-wrap">
          <el-icon :size="22"><ChatDotRound /></el-icon>
          <span v-if="chatTabBadge" class="msg-badge">{{ chatTabBadge }}</span>
        </div>
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
.top-left {
  display: flex;
  align-items: center;
  gap: 2px;
}
.back-btn {
  margin-left: -6px;
  color: #333;
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
.tab-ic-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.msg-badge {
  position: absolute;
  top: -7px;
  right: -12px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #ff2442;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  line-height: 16px;
  text-align: center;
  box-sizing: border-box;
  border: 1px solid #fff;
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
