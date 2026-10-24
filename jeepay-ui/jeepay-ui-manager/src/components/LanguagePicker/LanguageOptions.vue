<template>
  <!-- 語言選項：名稱一律用該語言自己的寫法，下方是目前介面語言的說明與該語言的範例句 -->
  <div :class="['jp-lang-options', layout]" role="radiogroup" :aria-label="$t('介面語言')">
    <button
      v-for="l in LOCALES"
      :key="l.code"
      type="button"
      role="radio"
      :aria-checked="value === l.code"
      :class="['jp-lang-opt', { on: value === l.code }]"
      @click="emit('update:value', l.code)"
    >
      <span class="txt">
        <b :lang="l.code">{{ l.name }}</b>
        <small>{{ $t(l.zh) }}<template v-if="l.code === current">・{{ $t('使用中') }}</template></small>
        <em :lang="l.code">{{ l.sample }}</em>
      </span>
      <CheckCircleFilled v-if="value === l.code" class="mark" />
    </button>
  </div>
</template>

<script setup lang="ts">
import { LOCALES, locale } from '@/i18n'

defineProps({
  value: { type: String, default: '' },
  layout: { type: String, default: 'grid' }, // grid：電腦版三欄；list：手機版單欄
})
const emit = defineEmits(['update:value'])
const current = locale.value
</script>

<style lang="less">
.jp-lang-options {
  display: grid;
  gap: 10px;
  &.grid {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  }
  &.list {
    grid-template-columns: 1fr;
    gap: 0;
    background: var(--jp-surface);
    border: 1px solid var(--jp-border);
    border-radius: 14px;
    overflow: hidden;
    .jp-lang-opt {
      border: 0;
      border-radius: 0;
      padding: 12px 16px;
      & + .jp-lang-opt {
        border-top: 1px solid var(--jp-border);
      }
      &.on {
        box-shadow: none;
        background: var(--jp-primary-soft);
      }
    }
  }
}
.jp-lang-opt {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  text-align: left;
  padding: 12px 14px;
  border: 1px solid var(--jp-border);
  border-radius: 12px;
  background: var(--jp-surface);
  color: var(--jp-text);
  cursor: pointer;
  font: inherit;
  transition:
    border-color 0.15s,
    background 0.15s;
  &:hover {
    border-color: var(--jp-primary);
  }
  &:focus-visible {
    outline: 2px solid var(--jp-primary);
    outline-offset: 2px;
  }
  &.on {
    border-color: var(--jp-primary);
    background: var(--jp-primary-soft);
    box-shadow: 0 0 0 1px var(--jp-primary) inset;
  }
  .txt {
    flex: 1;
    min-width: 0;
    display: grid;
    gap: 2px;
  }
  b {
    font-size: 15px;
    font-weight: 600;
  }
  small {
    font-size: 12px;
    color: var(--jp-text-3);
  }
  em {
    font-style: normal;
    font-size: 12px;
    color: var(--jp-text-2);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .mark {
    color: var(--jp-primary);
    font-size: 18px;
    flex: none;
  }
}
</style>
