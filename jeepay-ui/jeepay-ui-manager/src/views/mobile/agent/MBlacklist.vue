<template>
  <!-- 黑名單（/agentPortal/branch/blacklist）：提現收款帳號、戶名、電話命中即拒絕 -->
  <button class="m-btn" @click="vdata.form.open = true">{{ $t('＋ 新增黑名單') }}</button>
  <div class="m-list">
    <div v-for="b in vdata.list" :key="b.id" class="m-row">
      <div class="main">
        <b class="mono">{{ b.listValue }}</b>
        <small>{{ TYPES[b.listType] || b.listType }}{{ b.remark ? '・' + b.remark : '' }}・{{ b.createdAt }}</small>
      </div>
      <button class="m-btn sm danger" @click="remove(b)">{{ $t('移除') }}</button>
    </div>
    <div v-if="!vdata.list.length" class="m-empty">{{ vdata.loaded ? $t('黑名單是空的') : $t('載入中…') }}</div>
  </div>

  <MSheet v-model:open="vdata.form.open" :title="$t('新增黑名單')">
    <div class="m-seg">
      <button v-for="(label, k) in TYPES" :key="k" :class="{ on: vdata.form.listType === k }" @click="vdata.form.listType = k">{{ label }}</button>
    </div>
    <a-input v-model:value="vdata.form.listValue" size="large" :placeholder="$t('內容')" />
    <a-input v-model:value="vdata.form.remark" size="large" :placeholder="$t('備註（選填）')" />
    <div class="m-actions">
      <button class="m-btn ghost" @click="vdata.form.open = false">{{ $t('取消') }}</button>
      <button class="m-btn" @click="add">{{ $t('新增') }}</button>
    </div>
  </MSheet>
</template>

<script setup lang="ts">
import { reactive, getCurrentInstance } from 'vue'
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import MSheet from '../MSheet.vue'
import { t } from '@/i18n'

const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties
const BASE = API_URL_AGENT_PORTAL + '/branch/blacklist'
const TYPES = { BANK_ACCOUNT: t('收款帳號'), ACCOUNT_NAME: t('戶名'), PHONE: t('電話') }
const vdata: any = reactive({ list: [], loaded: false, form: { open: false, listType: 'BANK_ACCOUNT', listValue: '', remark: '' } })
function load() {
  req.list(BASE, {}).then((r) => {
    vdata.list = r || []
    vdata.loaded = true
  })
}
function add() {
  const f = vdata.form
  if (!f.listValue.trim()) {
    $infoBox.message.warning(t('請填寫內容'))
    return
  }
  req.add(BASE, { listType: f.listType, listValue: f.listValue, remark: f.remark }).then(() => {
    $infoBox.message.success(t('已新增'))
    vdata.form = { open: false, listType: f.listType, listValue: '', remark: '' }
    load()
  })
}
function remove(b) {
  $infoBox.confirmDanger(t('移除這筆黑名單？'), `${TYPES[b.listType] || b.listType}：${b.listValue}`, () => {
    req.delById(BASE, b.id).then(() => {
      $infoBox.message.success(t('已移除'))
      load()
    })
  })
}
load()
</script>
