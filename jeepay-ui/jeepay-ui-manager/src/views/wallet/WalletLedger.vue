<template>
  <page-header-wrapper>
    <a-card :bordered="false">
      <template #title>餘額流水 <a-tag color="purple">規劃中</a-tag></template>

      <!-- 規劃中功能：複式記帳流水（t_ledger_entry）尚未建立，本頁為佔位，不呼叫任何 API -->
      <a-alert
        type="info"
        show-icon
        message="此功能規劃中，尚未實作"
        description="每一筆影響餘額的事件（入帳、手續費、凍結、解凍、提現）將以「變動前 → 變動金額 → 變動後」記錄，並可依帳戶與訂單查詢。凍結金額將獨立分桶，不直接扣減可用餘額。"
        style="margin-bottom: 16px"
      />

      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col v-for="tile in tiles" :key="tile.label" :xs="24" :sm="12" :md="6">
          <a-card size="small">
            <a-statistic :title="tile.label" value="—" />
          </a-card>
        </a-col>
      </a-row>

      <a-form layout="inline" style="margin-bottom: 16px">
        <a-form-item label="帳戶">
          <a-input placeholder="代理／商戶號" style="width: 200px" disabled />
        </a-form-item>
        <a-form-item label="業務類型">
          <a-select placeholder="全部" style="width: 160px" disabled />
        </a-form-item>
        <a-form-item label="期間">
          <a-range-picker disabled />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" disabled>查詢 <a-tag style="margin-left: 6px">尚未實作</a-tag></a-button>
        </a-form-item>
      </a-form>

      <a-table :columns="columns" :data-source="[]" :pagination="false" size="middle">
        <template #emptyText>餘額流水功能開發中，尚無資料</template>
      </a-table>
    </a-card>
  </page-header-wrapper>
</template>
<script setup lang="ts">
const tiles = [
  { label: '可用餘額（新臺幣元）' },
  { label: '凍結金額（新臺幣元）' },
  { label: '今日入帳（新臺幣元）' },
  { label: '平台今日費收（新臺幣元）' },
]

const columns = [
  { key: 'createdAt', title: '時間', dataIndex: 'createdAt' },
  { key: 'account', title: '帳戶', dataIndex: 'account' },
  { key: 'bizType', title: '業務類型', dataIndex: 'bizType' },
  { key: 'balanceBefore', title: '變動前', dataIndex: 'balanceBefore' },
  { key: 'amount', title: '變動金額', dataIndex: 'amount' },
  { key: 'balanceAfter', title: '變動後', dataIndex: 'balanceAfter' },
  { key: 'refOrderId', title: '關聯訂單', dataIndex: 'refOrderId' },
  { key: 'remark', title: '備註', dataIndex: 'remark' },
]
</script>
