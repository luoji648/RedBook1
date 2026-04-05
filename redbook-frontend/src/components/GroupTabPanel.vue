<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { groupByOwner, groupJoin } from '../api'

const props = defineProps({
  ownerUserId: { type: [String, Number], required: true },
})

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const list = ref([])
async function load() {
  try {
    const { data } = await groupByOwner(props.ownerUserId)
    list.value = data || []
  } catch {
    list.value = []
  }
}

async function enterGroup(g) {
  if (g.inGroup) {
    router.push({ name: 'group-chat', params: { groupId: String(g.id) } })
    return
  }
  if (!auth.token) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  try {
    const { data } = await groupJoin(g.id)
    const st = data?.status
    if (st === 'joined' || st === 'already') {
      ElMessage.success(st === 'already' ? '已在群内' : '已加入群聊')
      await load()
      router.push({ name: 'group-chat', params: { groupId: String(g.id) } })
    } else if (st === 'pending') {
      ElMessage.success('已提交申请，群主将在私信中收到通知')
    }
  } catch {
    /* http 已提示 */
  }
}

function joinModeLabel(m) {
  return Number(m) === 1 ? '需群主验证' : '无需验证'
}

watch(
  () => props.ownerUserId,
  () => load(),
)
onMounted(() => load())
</script>

<template>
  <div class="gwrap">
    <div v-if="!list.length" class="empty">暂无群聊</div>
    <div v-else class="glist">
      <div v-for="g in list" :key="g.id" class="grow">
        <div class="ginfo">
          <div class="gname">{{ g.name }}</div>
          <div class="gmeta">{{ joinModeLabel(g.joinMode) }}</div>
        </div>
        <el-button size="small" type="primary" plain @click="enterGroup(g)">
          {{ g.inGroup ? '进入群聊' : '加入' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.gwrap {
  min-height: 120px;
}
.empty {
  text-align: center;
  color: #999;
  padding: 32px;
  font-size: 14px;
}
.glist {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.grow {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  background: #fafafa;
  border-radius: 10px;
}
.ginfo {
  min-width: 0;
  flex: 1;
}
.gname {
  font-weight: 600;
  font-size: 15px;
  color: #333;
}
.gmeta {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
