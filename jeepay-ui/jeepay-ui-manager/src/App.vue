<template>
  <a-config-provider :locale="antdLocale" :theme="antTheme()">
    <a-spin :spinning="userStore.globalLoading">
      <div id="app" style="height: 100vh">
        <router-view />
      </div>
    </a-spin>
    <!-- 儲存語言後整頁重新載入前的遮罩 -->
    <div v-if="switching" class="jp-lang-switching"><a-spin /><span>{{ $t('正在切換語言…') }}</span></div>
  </a-config-provider>
</template>

<script lang="ts" setup>
import { useUserStore } from '@/store/modules/user'
import { antTheme } from '@/utils/colorScheme'
// 元件語系與 dayjs 語系由 i18n 在啟動時依使用者選的語言載入
import { antdLocale, switching } from '@/i18n'
const userStore = useUserStore()

const styleDom = document.createElement('style')
styleDom.textContent = `
  :root {
    --ant-primary-color: #1677ff
  }
`
// 创建全局的css变量
document.head.appendChild(styleDom)
</script>

<style lang="less">
.jp-lang-switching {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: var(--jp-bg);
  color: var(--jp-text-2);
}
</style>
