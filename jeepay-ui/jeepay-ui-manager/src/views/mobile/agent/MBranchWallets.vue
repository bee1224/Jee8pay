<template>
  <!-- 旗下錢包（/agentPortal/branch/wallets）：餘額、人工凍結／解凍 -->
  <div class="m-list">
    <button v-for="w in vdata.wallets" :key="w.ownerType + w.ownerId" class="m-row tap" @click="vdata.cur = w">
      <div class="main">
        <b>{{ w.ownerName || w.ownerId }}</b>
        <small>{{ OWNER_TYPE_NAMES[w.ownerType] }}・<span class="mono">{{ w.ownerId }}</span></small>
      </div>
      <div class="end">
        <b class="num">{{ ntd(w.balance, 2) }}</b>
        <span v-if="Number(w.manualFrozen) > 0" class="m-pill bad">{{ $t('凍結 {0}', [ntd(w.manualFrozen)]) }}</span>
      </div>
    </button>
    <div v-if="!vdata.wallets.length" class="m-empty">{{ vdata.loaded ? $t('旗下還沒有錢包') : $t('載入中…') }}</div>
  </div>
  <p class="m-muted">{{ $t('凍結後該筆金額不能提現，分潤照常入帳。') }}</p>

  <MSheet :open="!!vdata.cur" :title="vdata.cur ? vdata.cur.ownerName || vdata.cur.ownerId : ''" @update:open="(v) => !v && close()">
    <template v-if="vdata.cur">
      <dl class="m-kv">
        <dt>{{ $t('可用餘額') }}</dt><dd class="num">{{ ntd(vdata.cur.balance, 2) }}</dd>
        <dt>{{ $t('凍結合計') }}</dt><dd class="num">{{ ntd(vdata.cur.frozen, 2) }}</dd>
        <dt>{{ $t('其中人工凍結') }}</dt><dd class="num">{{ ntd(vdata.cur.manualFrozen, 2) }}</dd>
      </dl>
      <template v-if="$access('ENT_AGENT_PORTAL_FREEZE')">
        <div class="m-seg">
          <button :class="{ on: vdata.mode === 'freeze' }" @click="vdata.mode = 'freeze'">{{ $t('凍結') }}</button>
          <button :class="{ on: vdata.mode === 'unfreeze' }" :disabled="!Number(vdata.cur.manualFrozen)" @click="vdata.mode = 'unfreeze'">{{ $t('解凍') }}</button>
        </div>
        <a-input-number v-model:value="vdata.amount" :min="0.01" :precision="2" :addon-after="$t('元')" size="large" style="width: 100%" :placeholder="$t('金額')" />
        <a-input v-model:value="vdata.remark" :maxlength="60" size="large" :placeholder="$t('原因（必填）')" />
        <button class="m-btn" :class="{ danger: vdata.mode === 'freeze' }" :disabled="vdata.saving" @click="submit">
          {{ vdata.mode === 'freeze' ? $t('凍結資金') : $t('解凍資金') }}
        </button>
      </template>
    </template>
  </MSheet>
</template>

<script setup lang="ts">
import { reactive, getCurrentInstance } from 'vue'
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import { OWNER_TYPE_NAMES } from '@/components/WalletPanel/walletText'
import { ntd } from '../mobileKit'
import MSheet from '../MSheet.vue'
import { t } from '@/i18n'

const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties
const BASE = API_URL_AGENT_PORTAL + '/branch'
const vdata: any = reactive({ wallets: [], loaded: false, cur: null, mode: 'freeze', amount: null, remark: '', saving: false })

function load() {
  req.list(BASE + '/wallets', {}).then((r) => {
    vdata.wallets = r || []
    vdata.loaded = true
  })
}
function close() {
  vdata.cur = null
  vdata.mode = 'freeze'
  vdata.amount = null
  vdata.remark = ''
}
function submit() {
  if (!vdata.amount || vdata.amount <= 0 || !vdata.remark.trim()) {
    $infoBox.message.warning(t('請填寫金額與原因'))
    return
  }
  vdata.saving = true
  req
    .add(`${BASE}/wallets/${vdata.mode}`, { ownerType: vdata.cur.ownerType, ownerId: vdata.cur.ownerId, amount: String(vdata.amount), remark: vdata.remark })
    .then(() => {
      $infoBox.message.success(vdata.mode === 'freeze' ? t('已凍結') : t('已解凍'))
      close()
      load()
    })
    .finally(() => (vdata.saving = false))
}
load()
</script>
