<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Setting } from '@element-plus/icons-vue'
import {
  fetchMe,
  noteMy,
  collectMy,
  likeMy,
  deleteNote,
} from '../api'
import GroupTabPanel from '../components/GroupTabPanel.vue'

const router = useRouter()

const me = ref(null)
const tab = ref('notes')
const list = ref([])
const total = ref(0)
const current = ref(1)
async function loadMe() {
  try {
    const { data } = await fetchMe()
    me.value = data
  } catch {
    me.value = null
  }
}

async function loadTab() {
  current.value = 1
  list.value = []
  total.value = 0
  try {
    if (tab.value === 'notes') {
      const { data, total: t } = await noteMy({ current: 1, size: 20 })
      list.value = data || []
      total.value = t || 0
    } else if (tab.value === 'collect') {
      const { data, total: t } = await collectMy({ current: 1, size: 20 })
      list.value = data || []
      total.value = t || 0
    } else if (tab.value === 'like') {
      const { data, total: t } = await likeMy({ current: 1, size: 20 })
      list.value = data || []
      total.value = t || 0
    } else if (tab.value === 'groups') {
      list.value = []
      total.value = 0
    }
  } catch {
    list.value = []
  }
}

async function more() {
  if (tab.value !== 'notes' && tab.value !== 'collect' && tab.value !== 'like') return
  current.value += 1
  try {
    if (tab.value === 'notes') {
      const { data } = await noteMy({ current: current.value, size: 20 })
      list.value = list.value.concat(data || [])
    } else if (tab.value === 'collect') {
      const { data } = await collectMy({ current: current.value, size: 20 })
      list.value = list.value.concat(data || [])
    } else if (tab.value === 'like') {
      const { data } = await likeMy({ current: current.value, size: 20 })
      list.value = list.value.concat(data || [])
    }
  } catch {
    /* */
  }
}

function goSignPage() {
  router.push({ name: 'daily-sign' })
}

function goNote(n) {
  router.push({ name: 'note-detail', params: { id: n.id } })
}

function goSettings() {
  router.push({ name: 'settings' })
}

function goMyFollowing() {
  const id = me.value?.user?.id
  if (!id) return
  router.push({ name: 'user-following', params: { userId: String(id) } })
}

function goMyFollowers() {
  const id = me.value?.user?.id
  if (!id) return
  router.push({ name: 'user-followers', params: { userId: String(id) } })
}

function editNote(n) {
  router.push({ name: 'publish', query: { id: String(n.id) } })
}

async function removeNote(n) {
  try {
    await ElMessageBox.confirm('确定删除该笔记？删除后不可恢复。', '删除笔记', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteNote(n.id)
    ElMessage.success('已删除')
    await loadTab()
  } catch (e) {
    if (e !== 'cancel') {
      /* */
    }
  }
}

onMounted(async () => {
  await loadMe()
  await loadTab()
})
</script>

<template>
  <div v-if="me" class="profile">
    <div class="head">
      <img
        :src="me.user?.icon || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
        class="av"
        alt=""
      />
      <div class="txt">
        <div class="name">{{ me.user?.nickName || '用户' }}</div>
        <div class="sub">
          <span class="stat-link" role="button" tabindex="0" @click="goMyFollowing">关注 {{ me.userInfo?.followee ?? 0 }}</span>
          <span class="sep"> · </span>
          <span class="stat-link" role="button" tabindex="0" @click="goMyFollowers">粉丝 {{ me.userInfo?.fans ?? 0 }}</span>
        </div>
      </div>
      <el-button :icon="Setting" circle @click="goSettings" />
    </div>
    <div class="row">
      <el-button type="primary" plain size="small" @click="goSignPage">每日签到</el-button>
      <el-button size="small" @click="router.push({ name: 'orders' })">我的订单</el-button>
    </div>

    <el-tabs v-model="tab" @tab-change="loadTab">
      <el-tab-pane label="笔记" name="notes" />
      <el-tab-pane label="收藏" name="collect" />
      <el-tab-pane label="点赞" name="like" />
      <el-tab-pane label="群聊" name="groups" />
    </el-tabs>

    <div v-if="tab === 'notes' || tab === 'collect' || tab === 'like'" class="grid">
      <div v-for="n in list" :key="n.id" class="cell">
        <div class="cover" @click="goNote(n)">{{ (n.title || n.content || '笔记').slice(0, 20) }}</div>
        <div class="t" @click="goNote(n)">{{ n.title || '无标题' }}</div>
        <div v-if="tab === 'notes'" class="ops" @click.stop>
          <el-button link type="primary" size="small" @click="editNote(n)">编辑</el-button>
          <el-button link type="danger" size="small" @click="removeNote(n)">删除</el-button>
        </div>
      </div>
    </div>

    <div v-if="tab === 'groups' && me?.user?.id" class="gpanel">
      <GroupTabPanel :owner-user-id="me.user.id" />
    </div>

    <el-button
      v-if="tab === 'notes' || tab === 'collect' || tab === 'like'"
      v-show="list.length < total"
      class="more"
      text
      type="primary"
      @click="more"
    >
      加载更多
    </el-button>
  </div>
</template>

<style scoped>
.profile {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}
.head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.av {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
}
.txt {
  flex: 1;
}
.name {
  font-weight: 700;
  font-size: 18px;
}
.sub {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
.stat-link {
  cursor: pointer;
  color: inherit;
}
.stat-link:hover {
  color: #409eff;
}
.sep {
  user-select: none;
}
.row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.cell {
  background: #fafafa;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
}
.cover {
  height: 100px;
  background: linear-gradient(135deg, #ffeaa7, #fab1a0);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #fff;
  padding: 8px;
  text-align: center;
}
.t {
  padding: 8px;
  font-size: 13px;
}
.ops {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  padding: 0 8px 8px;
}
.more {
  width: 100%;
  margin-top: 12px;
}
.gpanel {
  padding-top: 4px;
}
</style>
