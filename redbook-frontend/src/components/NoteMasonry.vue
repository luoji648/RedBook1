<script setup>
import NoteCard from './NoteCard.vue'
import { computed } from 'vue'

const props = defineProps({
  list: { type: Array, default: () => [] },
})

const left = computed(() => props.list.filter((_, i) => i % 2 === 0))
const right = computed(() => props.list.filter((_, i) => i % 2 === 1))
</script>

<template>
  <div class="masonry">
    <div class="col">
      <NoteCard v-for="(vo, i) in left" :key="(vo.note && vo.note.id) || i" :vo="vo" />
    </div>
    <div class="col">
      <NoteCard v-for="(vo, i) in right" :key="(vo.note && vo.note.id) || i" :vo="vo" />
    </div>
  </div>
</template>

<style scoped>
.masonry {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.col {
  flex: 1;
  min-width: 0;
}
</style>
