<template>
  <!-- 系統設定：依群組分頁，每個設定一格，改完按「儲存這一組」 -->
  <div class="m-seg">
    <button v-for="t in TABS" :key="t.key" :class="{ on: vdata.group === t.key }" @click="setGroup(t.key)">{{ t.name }}</button>
  </div>
  <div class="m-card" style="display: grid; gap: 14px">
    <label v-for="c in vdata.items" :key="c.configKey" class="m-cfg">
      <span class="m-cfg-name">{{ c.configName }}</span>
      <a-textarea v-if="c.type === 'textarea'" v-model:value="c.configVal" :auto-size="{ minRows: 2, maxRows: 6 }" />
      <a-input v-else v-model:value="c.configVal" size="large" autocomplete="off" />
      <small v-if="c.configDesc" class="m-muted" style="margin: 0">{{ c.configDesc }}</small>
    </label>
    <p v-if="!vdata.items.length" class="m-muted" style="margin: 0">{{ vdata.loaded ? $t('這一組沒有設定') : $t('載入中…') }}</p>
  </div>
  <button v-if="$access('ENT_SYS_CONFIG_EDIT') && vdata.items.length" class="m-btn" :disabled="vdata.saving" @click="save">{{ $t('儲存這一組') }}</button>
</template>

<script setup lang="ts">
import { reactive, getCurrentInstance } from 'vue'
import { API_URL_SYS_CONFIG, getConfigs, req } from '@/api/manage'
import { t } from '@/i18n'

const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties
// 與電腦版相同的群組（t_sys_config.group_key）；容量上限在「流量監控」裡修改
const TABS = [
  { key: 'applicationConfig', name: t('應用設定') },
  { key: 'walletConfig', name: t('錢包與提現') },
]
const vdata: any = reactive({ group: 'applicationConfig', items: [], loaded: false, saving: false })
function load() {
  vdata.loaded = false
  getConfigs(vdata.group).then((r) => {
    vdata.items = r || []
    vdata.loaded = true
  })
}
function setGroup(g) {
  vdata.group = g
  load()
}
function save() {
  $infoBox.confirmPrimary(t('確認修改這組設定嗎？'), '', () => {
    const form = new FormData()
    vdata.items.forEach((c) => form.append(c.configKey, c.configVal))
    vdata.saving = true
    req.updateById(API_URL_SYS_CONFIG, vdata.group, form).then(() => $infoBox.message.success(t('已儲存'))).finally(() => (vdata.saving = false))
  })
}
load()
</script>

<style scoped lang="less">
.m-cfg {
  display: grid;
  gap: 6px;
}
.m-cfg-name {
  font-size: 13.5px;
  color: var(--jp-text-2);
}
</style>
