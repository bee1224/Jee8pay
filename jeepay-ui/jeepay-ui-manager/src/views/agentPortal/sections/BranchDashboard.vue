<template>
  <!-- 統計看板：比照競品首頁，餘額與今日／昨日／本月／上月成交卡片，加上最近五次登入 -->
  <div>
    <a-row :gutter="[16, 16]">
      <a-col v-for="c in cards" :key="c.title" :xs="12" :md="8" :xl="6">
        <a-card size="small"><a-statistic :title="c.title" :value="c.value" /></a-card>
      </a-col>
    </a-row>
    <a-card size="small" :title="$t('上次登入資訊')" style="margin-top: 16px; max-width: 640px">
      <a-table :columns="loginColumns" :data-source="logins.slice(0, 5)" size="small" row-key="sysLogId" :pagination="false">
        <template #emptyText>{{ $t('尚無登入紀錄') }}</template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import { yuan } from '@/components/WalletPanel/walletText'
import { computed, reactive } from 'vue'
import { t } from '@/i18n'
defineProps({ me: { type: Object, required: true }, logins: { type: Array, default: () => [] } })

const vdata: any = reactive({ stat: {}, wallet: {} })
const loginColumns = [
  { title: t('登入時間'), dataIndex: 'createdAt' },
  { title: t('登入 IP 位址'), dataIndex: 'userIp' },
]
const cards = computed(() => {
  const s = vdata.stat
  const w = vdata.wallet
  return [
    { title: t('可用餘額（元）'), value: yuan(w.balance) },
    { title: t('總餘額（元）'), value: yuan(Number(w.balance || 0) + Number(w.frozen || 0)) },
    { title: t('凍結金額（元）'), value: yuan(w.frozen) },
    { title: t('今日商戶手續費（元）'), value: yuan(s.todayFee) },
    { title: t('今日成交量（元）'), value: yuan(s.todayAmount) },
    { title: t('昨日成交量（元）'), value: yuan(s.yesterdayAmount) },
    { title: t('今日成功單數／失敗單數'), value: `${s.todayPaid || 0}／${s.todayFailed || 0}` },
    { title: t('轄下商戶數'), value: s.mchCount || 0 },
    { title: t('本月成交量（元）'), value: yuan(s.monthAmount) },
    { title: t('上月成交量（元）'), value: yuan(s.lastMonthAmount) },
  ]
})

req.list(API_URL_AGENT_PORTAL + '/branch/dashboard', {}).then((res) => (vdata.stat = res || {}))
req.list(API_URL_AGENT_PORTAL + '/wallet', {}).then((res) => (vdata.wallet = res || {}))
</script>
