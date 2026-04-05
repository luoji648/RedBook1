import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAuthStore } from './auth'
import { chatUnreadTotal } from '../api'

export const useMessageUnreadStore = defineStore('messageUnread', () => {
  const totalUnread = ref(0)
  const noticeLikeCollect = ref(0)
  const noticeFollow = ref(0)
  const noticeComment = ref(0)

  async function refresh() {
    const auth = useAuthStore()
    if (!auth.token) {
      reset()
      return
    }
    try {
      const { data } = await chatUnreadTotal()
      const d = data && typeof data === 'object' ? data : {}
      totalUnread.value = Number(d.total) || 0
      // 兼容 camelCase / snake_case（代理或旧端可能改写字段名）
      noticeLikeCollect.value =
        Number(d.likeCollect ?? d.like_collect) || 0
      noticeFollow.value = Number(d.follow) || 0
      noticeComment.value = Number(d.comment) || 0
    } catch {
      totalUnread.value = 0
      noticeLikeCollect.value = 0
      noticeFollow.value = 0
      noticeComment.value = 0
    }
  }

  function reset() {
    totalUnread.value = 0
    noticeLikeCollect.value = 0
    noticeFollow.value = 0
    noticeComment.value = 0
  }

  return {
    totalUnread,
    noticeLikeCollect,
    noticeFollow,
    noticeComment,
    refresh,
    reset,
  }
})
