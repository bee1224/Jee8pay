<template>
  <!-- 手機版外框：頂部標題列（沒有重新整理按鈕）、可捲動內容、底部分頁列（主選單＝Home） -->
  <div class="m-app">
    <header class="m-topbar">
      <div>
        <button v-if="showBack" class="m-back" :aria-label="$t('返回主選單')" @click="emit('back')"><LeftOutlined /></button>
      </div>
      <h2>{{ title }}</h2>
      <div />
    </header>
    <main ref="contentRef" class="m-content">
      <slot />
    </main>
    <nav class="m-tabbar" :aria-label="$t('主要分頁')">
      <button v-for="t in tabs" :key="t.key" :class="{ active: t.key === activeTab }" @click="emit('tab', t)">
        <component :is="t.icon" style="font-size: 21px" />
        <span>{{ t.label }}</span>
      </button>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { LeftOutlined } from '@ant-design/icons-vue'
import './mobile.less'

const props = defineProps({
  title: { type: String, default: '' },
  showBack: { type: Boolean, default: false },
  tabs: { type: Array as () => any[], default: () => [] },
  activeTab: { type: String, default: '' },
  // 換頁時回到最上方
  pageKey: { type: String, default: '' },
})
const emit = defineEmits(['back', 'tab'])
const contentRef = ref()
watch(
  () => props.pageKey,
  () => contentRef.value && (contentRef.value.scrollTop = 0)
)
</script>
