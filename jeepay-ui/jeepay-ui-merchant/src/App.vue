<template>
  <a-config-provider :locale="zhTW">
    <div id="app">
      <router-view />
    </div>
  </a-config-provider>
</template>

<script lang="ts" setup>
import zhTW from 'ant-design-vue/es/locale/zh_TW'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-tw'
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
