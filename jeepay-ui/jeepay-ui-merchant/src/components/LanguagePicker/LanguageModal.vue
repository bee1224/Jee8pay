<template>
  <!-- 電腦版語言設定：選好按「儲存並套用」，整頁重新載入後全部換成新語言 -->
  <a-modal
    :open="open"
    :title="$t('語言設定')"
    :width="720"
    :ok-text="$t('儲存並套用')"
    :cancel-text="$t('取消')"
    :ok-button-props="{ disabled: picked === locale }"
    :confirm-loading="switching"
    @ok="saveLocale(picked)"
    @cancel="emit('update:open', false)"
  >
    <p class="jp-lang-hint">
      <GlobalOutlined />
      {{ $t('儲存後會重新載入頁面，選單、表格、按鈕、提示訊息與日期格式會一起換成新語言。這個設定只存在目前這台裝置。') }}
    </p>
    <LanguageOptions v-model:value="picked" layout="grid" />
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { locale, saveLocale, switching } from '@/i18n'
import LanguageOptions from './LanguageOptions.vue'

const props = defineProps({ open: { type: Boolean, default: false } })
const emit = defineEmits(['update:open'])
const picked = ref(locale.value)
watch(
  () => props.open,
  (v) => v && (picked.value = locale.value),
)
</script>

<style lang="less">
.jp-lang-hint {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  color: var(--jp-text-2);
  font-size: 13px;
  margin: 0 0 14px;
  line-height: 1.6;
  .anticon {
    margin-top: 4px;
    color: var(--jp-primary);
  }
}
</style>
