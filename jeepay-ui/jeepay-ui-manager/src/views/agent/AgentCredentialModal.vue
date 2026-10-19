<template>
  <a-modal v-model:open="vdata.open" title="登入帳號已建立" :footer="null" :mask-closable="false" width="440px">
    <a-alert
      type="warning"
      show-icon
      style="margin-bottom: 16px"
      message="密碼只會顯示這一次，關閉後無法再查看，請先複製並交給對方。"
    />
    <a-descriptions :column="1" bordered size="small">
      <a-descriptions-item :label="vdata.subjectLabel">{{ vdata.agentName }}</a-descriptions-item>
      <a-descriptions-item label="登入網址">
        <a-typography-text v-if="vdata.loginUrl" :copyable="{ text: vdata.loginUrl, tooltips: ['複製', '已複製'] }">
          <a :href="vdata.loginUrl" target="_blank" rel="noopener noreferrer">{{ vdata.loginUrl }}</a>
        </a-typography-text>
        <span v-else style="color: #999">商戶平台尚未設定對外網址（系統配置「商戶平台網址」）</span>
        <div v-if="vdata.note" style="color: #fa8c16; font-size: 12px">{{ vdata.note }}</div>
      </a-descriptions-item>
      <a-descriptions-item label="帳號">
        <a-typography-text :copyable="{ text: vdata.loginUsername, tooltips: ['複製', '已複製'] }">{{ vdata.loginUsername }}</a-typography-text>
      </a-descriptions-item>
      <a-descriptions-item label="密碼">
        <a-typography-text :copyable="{ text: vdata.initPassword, tooltips: ['複製', '已複製'] }">
          <span style="font-family: monospace; font-size: 15px; letter-spacing: 1px">{{ vdata.initPassword }}</span>
        </a-typography-text>
      </a-descriptions-item>
    </a-descriptions>
    <div style="margin-top: 16px; display: flex; justify-content: space-between; align-items: center">
      <a-button size="small" @click="copy">
        <template #icon><copy-outlined /></template>
        複製網址與帳號密碼
      </a-button>
      <a-button type="primary" @click="vdata.open = false">我已記下</a-button>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
// 登入帳號建立後的一次性帳密視窗（新增代理、開通登入帳號、代理新增商戶共用）
import { reactive, getCurrentInstance } from 'vue'
const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties

const vdata: any = reactive({ open: false, subjectLabel: '代理', loginUrl: '', note: '', agentName: '', loginUsername: '', initPassword: '' })

function show(info) {
  vdata.agentName = info.agentName || ''
  vdata.subjectLabel = info.subjectLabel || '代理'
  // 登入網址：商戶帳號用後端回傳的商戶平台網址；營運平台帳號（團長、隊長、子帳號）就是目前這個網站
  const loginAt = info.loginAt || '營運平台'
  vdata.loginUrl = loginAt.startsWith('商戶') ? info.loginUrl || '' : window.location.origin + '/login'
  vdata.note = loginAt.includes('（') ? loginAt.slice(loginAt.indexOf('（') + 1, loginAt.lastIndexOf('）')) : ''
  vdata.loginUsername = info.loginUsername
  vdata.initPassword = info.initPassword
  vdata.open = true
}

function copy() {
  const text = `${vdata.loginUrl ? `登入網址：${vdata.loginUrl}\n` : ''}帳號：${vdata.loginUsername}\n密碼：${vdata.initPassword}`
  const fallback = () => {
    const el = document.createElement('textarea')
    el.value = text
    el.style.position = 'fixed'
    el.style.opacity = '0'
    document.body.appendChild(el)
    el.select()
    const ok = document.execCommand('copy')
    el.remove()
    ok ? $infoBox.message.success('已複製') : $infoBox.message.error('複製失敗，請手動選取')
  }
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(text).then(() => $infoBox.message.success('已複製'), fallback)
  } else {
    fallback()
  }
}

defineExpose({ show })
</script>
