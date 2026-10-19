<template>
  <!-- 依目前頁面（route name）挑選 godPages.ts 的設定，交給 MCrud 呈現 -->
  <MCrud v-if="cfg" :key="String(route.name)" :cfg="cfg" />
  <p v-else class="m-empty">這一頁還沒有手機版設定</p>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import MCrud from './MCrud.vue'
import { GOD_PAGES } from './godPages'

const route = useRoute()
const cfg = computed(() => GOD_PAGES[route.name as string])
</script>
