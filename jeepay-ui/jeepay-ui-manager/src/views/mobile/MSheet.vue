<template>
  <!-- 手機版底部彈出面板：明細、表單、確認都用這個 -->
  <a-drawer
    :open="open"
    placement="bottom"
    :closable="false"
    height="auto"
    root-class-name="m-sheet"
    :body-style="{ maxHeight: '82vh', overflowY: 'auto' }"
    @close="emit('update:open', false)"
  >
    <div class="m-grab" />
    <h3 v-if="title">{{ title }}</h3>
    <slot />
  </a-drawer>
</template>

<script setup lang="ts">
defineProps({ open: { type: Boolean, default: false }, title: { type: String, default: '' } })
const emit = defineEmits(['update:open'])
</script>
