<template>
  <!-- 組合鍵顯示成一顆顆鍵帽；沒有設定時顯示「未設定」 -->
  <span v-if="keys.length" class="jp-kbd-row" :title="comboText(combo)">
    <kbd v-for="(k, i) in keys" :key="i" :class="['jp-kbd', { small }]">{{ k }}</kbd>
  </span>
  <span v-else-if="!hideEmpty" class="jp-kbd-none">{{ $t('未設定') }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { comboKeys, comboText } from './index'

const props = defineProps({
  combo: { type: String, default: '' },
  small: { type: Boolean, default: false },
  hideEmpty: { type: Boolean, default: false },
})
const keys = computed(() => comboKeys(props.combo))
</script>

<style lang="less">
.jp-kbd-row {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  vertical-align: middle;
}
.jp-kbd {
  min-width: 24px;
  height: 24px;
  padding: 0 7px;
  display: inline-grid;
  place-items: center;
  border-radius: 7px;
  font: 600 12px/1 var(--jp-font);
  color: var(--jp-text);
  background: var(--jp-card-bg);
  border: 1px solid var(--jp-card-edge);
  box-shadow:
    0 1px 0 var(--jp-card-line),
    0 2px 0 rgba(15, 23, 42, 0.08),
    var(--jp-card-gloss);
  &.small {
    min-width: 20px;
    height: 20px;
    padding: 0 5px;
    font-size: 11px;
    border-radius: 6px;
  }
}
.jp-kbd-none {
  color: var(--jp-text-3);
  font-size: 13px;
}
</style>
