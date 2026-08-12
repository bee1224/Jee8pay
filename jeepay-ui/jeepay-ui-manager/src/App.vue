<template>
  <a-config-provider :locale="zhTW">
    <a-spin :spinning="userStore.globalLoading">
      <div id="app" style="height: 100vh">
        <router-view />
      </div>
    </a-spin>
  </a-config-provider>
</template>

<script lang="ts" setup>
import zhTW from 'ant-design-vue/es/locale/zh_TW'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-tw'
import { useUserStore } from '@/store/modules/user'
const userStore = useUserStore()

dayjs.locale('zh-tw')

const styleDom = document.createElement('style')
styleDom.textContent = `
  :root {
    --ant-primary-color: #1677ff
  }
`
// 创建全局的css变量
document.head.appendChild(styleDom)
</script>
