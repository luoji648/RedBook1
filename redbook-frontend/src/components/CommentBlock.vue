<script setup>
defineOptions({ name: 'CommentBlock' })

defineProps({
  nodes: { type: Array, default: () => [] },
})
const emit = defineEmits(['reply'])

function replyTarget(node) {
  const c = node.comment
  emit('reply', { id: c.id, userId: c.userId, nick: '用户' + c.userId })
}
</script>

<template>
  <div v-for="node in nodes" :key="node.comment.id" class="c">
    <div class="line">
      <span class="u">用户{{ node.comment.userId }}</span>
      <span class="txt">{{ node.comment.content }}</span>
      <el-button link type="primary" size="small" @click="replyTarget(node)">回复</el-button>
    </div>
    <CommentBlock
      v-if="node.replies && node.replies.length"
      class="sub"
      :nodes="node.replies"
      @reply="(x) => emit('reply', x)"
    />
  </div>
</template>

<style scoped>
.c {
  margin-bottom: 10px;
}
.line {
  font-size: 14px;
  line-height: 1.5;
}
.u {
  color: #666;
  margin-right: 6px;
}
.txt {
  color: #333;
}
.sub {
  padding-left: 16px;
  margin-top: 8px;
  border-left: 2px solid #eee;
}
</style>
