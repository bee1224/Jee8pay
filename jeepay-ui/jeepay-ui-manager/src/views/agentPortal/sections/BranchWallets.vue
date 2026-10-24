<template>
  <!-- 旗下錢包：後代代理與轄下商戶的餘額、流水；團長可凍結／解凍 -->
  <div>
    <a-table :columns="columns" :data-source="vdata.wallets" size="small" row-key="key" :pagination="false" style="margin-bottom: 20px">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'owner'"><a-tag>{{ OWNER_TYPE_NAMES[record.ownerType] }}</a-tag>{{ record.ownerName }}（{{ record.ownerId }}）</template>
        <template v-if="column.key === 'balance'"><b>{{ yuan(record.balance) }}</b></template>
        <template v-if="column.key === 'frozen'">{{ yuan(record.frozen) }}</template>
        <template v-if="column.key === 'manualFrozen'">{{ yuan(record.manualFrozen) }}</template>
        <template v-if="column.key === 'op'">
          <a-button type="link" @click="showLedger(record)">{{ $t('流水') }}</a-button>
          <template v-if="canFreeze">
            <a-button type="link" @click="openFreeze(record, 'freeze')">{{ $t('凍結') }}</a-button>
            <a-button type="link" :disabled="!record.manualFrozen" @click="openFreeze(record, 'unfreeze')">{{ $t('解凍') }}</a-button>
          </template>
        </template>
      </template>
      <template #emptyText>{{ $t('旗下還沒有代理或商戶') }}</template>
    </a-table>

    <a-card size="small" :title="vdata.ledgerOwner ? $t('流水：{0}', [vdata.ledgerOwner.ownerName]) : $t('旗下全部流水')">
      <template #extra><a-button v-if="vdata.ledgerOwner" type="link" @click="showLedger(null)">{{ $t('看全部') }}</a-button></template>
      <a-table :columns="ledgerColumns" :data-source="vdata.ledger" size="small" row-key="ledgerId"
        :pagination="{ current: vdata.page, pageSize: 20, total: vdata.total, showTotal: (t) => $t('共{0}筆', [t]), onChange: (p) => { vdata.page = p; loadLedger() } }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'owner'"><a-tag>{{ OWNER_TYPE_NAMES[record.ownerType] }}</a-tag>{{ record.ownerId }}</template>
          <template v-if="column.key === 'bizType'">{{ BIZ_TYPE_NAMES[record.bizType] || record.bizType }}</template>
          <template v-if="column.key === 'amount'">{{ yuan(record.amount) }}</template>
          <template v-if="column.key === 'frozenChange'">{{ yuan(record.frozenChange) }}</template>
          <template v-if="column.key === 'balance'">{{ yuan(record.balanceBefore) }} → {{ yuan(record.balanceAfter) }}</template>
        </template>
        <template #emptyText>{{ $t('沒有流水') }}</template>
      </a-table>
    </a-card>

    <a-modal v-model:open="vdata.freeze.open" :title="vdata.freeze.mode === 'freeze' ? $t('凍結資金') : $t('解凍資金')" :confirm-loading="vdata.freeze.saving" :ok-text="$t('確定')" :cancel-text="$t('取消')" @ok="submitFreeze">
      <p v-if="vdata.freeze.row">{{ $t('對象：{0}（{1}）', [vdata.freeze.row.ownerName, vdata.freeze.row.ownerId]) }}<br />
        {{ vdata.freeze.mode === 'freeze' ? $t('可用餘額 {0} 元', [yuan(vdata.freeze.row.balance)]) : $t('人工凍結中 {0} 元', [yuan(vdata.freeze.row.manualFrozen)]) }}</p>
      <a-form layout="vertical">
        <a-form-item :label="$t('金額（元）')" required><a-input-number v-model:value="vdata.freeze.amount" :min="0.01" :precision="2" style="width: 100%" /></a-form-item>
        <a-form-item :label="$t('原因')" required><a-input v-model:value="vdata.freeze.remark" :maxlength="60" /></a-form-item>
      </a-form>
      <div style="color: #888; font-size: 12px">{{ $t('凍結只是把錢從可用移到凍結，總額不變；對方無法提現被凍結的金額。解凍只能解開人工凍結的部分。') }}</div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import { BIZ_TYPE_NAMES, OWNER_TYPE_NAMES, yuan } from '@/components/WalletPanel/walletText'
import { computed, reactive, getCurrentInstance } from 'vue'
import { t } from '@/i18n'
const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties
const props = defineProps({ me: { type: Object, required: true } })
const BASE = API_URL_AGENT_PORTAL + '/branch'

const canFreeze = computed(() => props.me.agentLevel === 1 && $access('ENT_AGENT_PORTAL_FREEZE'))
const columns = [
  { key: 'owner', title: t('對象') },
  { key: 'balance', title: t('可用餘額（元）') },
  { key: 'frozen', title: t('凍結合計（元）') },
  { key: 'manualFrozen', title: t('其中人工凍結（元）') },
  { key: 'op', title: t('操作'), width: 200 },
]
const ledgerColumns = [
  { title: t('時間'), dataIndex: 'createdAt', width: 170 },
  { key: 'owner', title: t('對象') },
  { key: 'bizType', title: t('類型') },
  { key: 'amount', title: t('可用變動') },
  { key: 'frozenChange', title: t('凍結變動') },
  { key: 'balance', title: t('可用餘額') },
  { title: t('說明'), dataIndex: 'remark' },
]
const vdata: any = reactive({
  wallets: [], ledger: [], total: 0, page: 1, ledgerOwner: null,
  freeze: { open: false, saving: false, mode: 'freeze', row: null, amount: null, remark: '' },
})

function loadWallets() {
  req.list(BASE + '/wallets', {}).then((res) => (vdata.wallets = (res || []).map((r) => ({ ...r, key: r.ownerType + r.ownerId }))))
}
function loadLedger() {
  const o = vdata.ledgerOwner
  req.list(BASE + '/ledger', { pageNumber: vdata.page, pageSize: 20, ownerType: o ? o.ownerType : undefined, ownerId: o ? o.ownerId : undefined })
    .then((res) => {
      vdata.ledger = res.records || []
      vdata.total = res.total || 0
    })
}
function showLedger(row) {
  vdata.ledgerOwner = row
  vdata.page = 1
  loadLedger()
}
function openFreeze(row, mode) {
  vdata.freeze = { open: true, saving: false, mode, row, amount: null, remark: '' }
}
function submitFreeze() {
  const f = vdata.freeze
  if (!f.amount || f.amount <= 0 || !f.remark.trim()) {
    $infoBox.message.warning(t('請填寫金額與原因'))
    return
  }
  f.saving = true
  req.add(`${BASE}/wallets/${f.mode}`, { ownerType: f.row.ownerType, ownerId: f.row.ownerId, amount: String(f.amount), remark: f.remark })
    .then(() => {
      $infoBox.message.success(f.mode === 'freeze' ? t('已凍結') : t('已解凍'))
      f.open = false
      loadWallets()
      loadLedger()
    })
    .finally(() => (f.saving = false))
}
loadWallets()
loadLedger()
</script>
