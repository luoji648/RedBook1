<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { chatMessages, chatSend, fetchMe } from '../api'

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

async function loadMe() {
  try {
    const { data } = await fetchMe()
    myId.value = data?.user?.id
  } catch {
    myId.value = null
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

onMounted(async () => {
  await loadMe()
  await loadMsgs()
})
</script>

<template>
  <div class="thread">
    <header class="bar">
      <el-button text @click="router.push({ name: 'chat' })">返回</el-button>
      <span>会话 {{ threadId }}</span>
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
