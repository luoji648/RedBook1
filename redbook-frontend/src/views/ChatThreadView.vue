<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { chatMessages, chatSend, fetchMe, userPublic } from '../api'

const DEFAULT_AVATAR =
  'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const route = useRoute()
const router = useRouter()
const threadId = computed(() => route.params.threadId)
const peerId = computed(() => {
  const q = route.query.peer
  const n = Number(q)
  return Number.isFinite(n) ? n : null
})

const myId = ref(null)
const msgs = ref([])
const text = ref('')
const peerProfile = ref(null)

const peerUser = computed(() => peerProfile.value?.user)

async function loadMe() {
  try {
    const { data } = await fetchMe()
    myId.value = data?.user?.id
  } catch {
    myId.value = null
  }
}

async function loadPeer() {
  if (peerId.value == null) {
    peerProfile.value = null
    return
  }
  try {
    const { data } = await userPublic(peerId.value)
    peerProfile.value = data
  } catch {
    peerProfile.value = null
  }
}

async function loadMsgs() {
  try {
    const { data } = await chatMessages(threadId.value, { current: 1, size: 100 })
    const arr = data || []
    msgs.value = arr.slice().reverse()
  } catch {
    msgs.value = []
  }
}

function goPeerProfile() {
  if (peerId.value == null) return
  router.push({ name: 'user-profile', params: { userId: String(peerId.value) } })
}

async function send() {
  const t = text.value.trim()
  if (!t) return
  if (peerId.value == null) {
    return
  }
  try {
    await chatSend(peerId.value, t)
    text.value = ''
    await loadMsgs()
  } catch {
    /* */
  }
}

watch([threadId, peerId], async () => {
  await loadPeer()
  await loadMsgs()
})

onMounted(async () => {
  await loadMe()
  await loadPeer()
  await loadMsgs()
})
</script>

<template>
  <div class="thread">
    <header class="bar">
      <el-button text circle title="返回" class="back-btn" @click="router.push({ name: 'chat' })">
        <el-icon :size="22"><ArrowLeft /></el-icon>
      </el-button>
      <button
        v-if="peerId != null"
        type="button"
        class="peer-head"
        title="查看主页"
        @click="goPeerProfile"
      >
        <img class="peer-av" :src="peerUser?.icon || DEFAULT_AVATAR" alt="" />
        <span class="peer-name">{{ peerUser?.nickName || `用户 ${peerId}` }}</span>
      </button>
      <span v-else class="title-fallback">会话 {{ threadId }}</span>
    </header>
    <div class="msgs">
      <div
        v-for="m in msgs"
        :key="m.id"
        :class="['bubble', m.senderId === myId ? 'me' : 'they']"
      >
        {{ m.content }}
        <div class="ts">{{ m.createTime }}</div>
      </div>
    </div>
    <div class="input">
      <el-input
        v-model="text"
        placeholder="输入消息"
        @keyup.enter="send"
      />
      <el-button type="primary" @click="send">发送</el-button>
    </div>
  </div>
</template>

<style scoped>
.thread {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}
.bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-bottom: 1px solid #eee;
}
.back-btn {
  margin-left: -4px;
  flex-shrink: 0;
  color: #333;
}
.peer-head {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
  padding: 4px 6px;
  margin: -4px -6px;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  text-align: left;
  font: inherit;
  color: inherit;
}
.peer-head:hover {
  background: #f7f7f7;
}
.peer-av {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.peer-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.title-fallback {
  font-size: 15px;
  color: #333;
}
.msgs {
  flex: 1;
  overflow: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.bubble {
  max-width: 80%;
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.4;
}
.they {
  align-self: flex-start;
  background: #f0f0f0;
}
.me {
  align-self: flex-end;
  background: #ffeef0;
  color: #333;
}
.ts {
  font-size: 10px;
  color: #999;
  margin-top: 4px;
}
.input {
  display: flex;
  gap: 8px;
  padding: 10px;
  border-top: 1px solid #eee;
}
</style>
