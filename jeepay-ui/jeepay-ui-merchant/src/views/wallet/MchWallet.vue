<template>
  <page-header-wrapper>
    <a-card :bordered="false">
      <template #title>我的錢包 <a-tag color="purple">規劃中</a-tag></template>

      <!-- 規劃中功能：商戶錢包與提現申請尚未建立，本頁為佔位，不呼叫任何 API -->
      <a-alert
        type="info"
        show-icon
        message="此功能規劃中，尚未實作"
        description="訂單成功後的實收金額將累積為錢包餘額，可主動申請提現；提現前會經過平台風控檢查。目前款項仍依既有方式結算。"
        style="margin-bottom: 16px"
      />

      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col v-for="tile in tiles" :key="tile" :xs="24" :sm="8">
          <a-card size="small">
            <a-statistic :title="tile" value="—" />
          </a-card>
        </a-col>
      </a-row>

      <div style="margin-bottom: 16px">
        <a-button type="primary" disabled>申請提現</a-button>
        <a-tag style="margin-left: 8px">尚未實作</a-tag>
      </div>

      <a-table :columns="columns" :data-source="[]" :pagination="false" size="middle">
        <template #emptyText>錢包功能開發中，尚無資料</template>
      </a-table>
    </a-card>
  </page-header-wrapper>
</template>
<script setup lang="ts">
const tiles = ['可用餘額（新臺幣元）', '凍結金額（新臺幣元）', '提現處理中（新臺幣元）']

const columns = [
  { key: 'createdAt', title: '時間', dataIndex: 'createdAt' },
  { key: 'bizType', title: '類型', dataIndex: 'bizType' },
  { key: 'amount', title: '變動金額', dataIndex: 'amount' },
  { key: 'balanceAfter', title: '變動後餘額', dataIndex: 'balanceAfter' },
  { key: 'refOrderId', title: '關聯訂單／申請單', dataIndex: 'refOrderId' },
]
</script>
