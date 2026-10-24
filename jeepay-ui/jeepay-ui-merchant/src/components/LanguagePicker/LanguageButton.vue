<template>
  <!-- 頂部列的語言按鈕：地球圖示＋目前語言，點開語言設定 -->
  <button type="button" :class="['jp-lang-btn', variant]" :title="$t('語言設定')" @click="open = true">
    <GlobalOutlined />
    <span :lang="cur.code">{{ cur.name }}</span>
  </button>
  <LanguageModal v-model:open="open" />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { currentLocale } from '@/i18n'
import LanguageModal from './LanguageModal.vue'

defineProps({ variant: { type: String, default: '' } }) // floating：登入頁右上角
const cur = currentLocale()
const open = ref(false)
</script>

<style lang="less">
.jp-lang-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--jp-text-2);
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  &:hover {
    color: var(--jp-primary);
    background: var(--jp-primary-soft);
  }
  &:focus-visible {
    outline: 2px solid var(--jp-primary);
  }
  &.floating {
    position: absolute;
    top: 16px;
    right: 16px;
    z-index: 2;
    background: var(--jp-glass);
    border-color: var(--jp-border);
    backdrop-filter: blur(8px);
  }
  // 手機寬度的頂部列放不下語言名稱，只留地球圖示
  @media (max-width: 767px) {
    &:not(.floating) span {
      display: none;
    }
  }
}
</style>
