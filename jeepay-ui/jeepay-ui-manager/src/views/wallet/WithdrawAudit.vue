<template>
  <page-header-wrapper>
    <a-card :bordered="false">
      <template #title>提現審核 <a-tag color="purple">規劃中</a-tag></template>

      <!-- 規劃中功能：提現申請狀態機（t_withdraw_request）與代付風控尚未建立，本頁為佔位，不呼叫任何 API -->
      <a-alert
        type="info"
        show-icon
        message="此功能規劃中，尚未實作"
        description="提現申請將依「待審核 → 風控檢查（排除名單／卡片頻率／限制銀行／黑名單／二次驗證）→ 人工複核或自動派發 → 執行中 → 成功／失敗」流轉；每筆申請帶冪等鍵，避免重複送出造成雙重扣款。風控逾時一律轉人工，不視為通過。"
        style="margin-bottom: 16px"
      />

      <a-card size="small" title="自動代付設定" style="margin-bottom: 16px">
        <a-form layout="vertical">
          <a-form-item>
            <template #label>全局自動代付 <a-tag style="margin-left: 6px">尚未實作</a-tag></template>
            <a-switch disabled />
          </a-form-item>
          <a-form-item label="強制人工審核名單（每行一個商戶號／代理 ID）">
            <a-textarea :rows="3" placeholder="規劃中" disabled />
          </a-form-item>
        </a-form>
      </a-card>

      <a-tabs>
        <a-tab-pane v-for="tab in tabs" :key="tab" :tab="tab">
          <a-table :columns="columns" :data-source="[]" :pagination="false" size="middle">
            <template #emptyText>提現審核功能開發中，尚無資料</template>
          </a-table>
        </a-tab-pane>
      </a-tabs>

      <div style="margin-top: 8px">
        <a-button type="primary" disabled style="margin-right: 8px">核准</a-button>
        <a-button danger disabled>駁回</a-button>
        <a-tag style="margin-left: 8px">尚未實作</a-tag>
      </div>
    </a-card>
  </page-header-wrapper>
</template>
<script setup lang="ts">
const tabs = ['待審核', '處理中', '已完成']

const columns = [
  { key: 'withdrawId', title: '申請單號', dataIndex: 'withdrawId' },
  { key: 'applicant', title: '申請人', dataIndex: 'applicant' },
  { key: 'amount', title: '金額（新臺幣元）', dataIndex: 'amount' },
  { key: 'fee', title: '手續費', dataIndex: 'fee' },
  { key: 'bankAccount', title: '收款帳戶', dataIndex: 'bankAccount' },
  { key: 'riskResult', title: '風控結果', dataIndex: 'riskResult' },
  { key: 'state', title: '狀態', dataIndex: 'state' },
  { key: 'createdAt', title: '申請時間', dataIndex: 'createdAt' },
]
</script>
