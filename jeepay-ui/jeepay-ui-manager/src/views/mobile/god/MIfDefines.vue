<template>
  <!-- 上帝：大渠道（支付接口定義，/api/payIfDefines）。每個上游一張卡：支援的支付方式、渠道帳號數、今天的建單與成功率 -->
  <div class="m-search">
    <SearchOutlined />
    <input v-model="vdata.q" type="search" placeholder="搜尋上游名稱或代碼" />
  </div>
  <div class="m-chips">
    <button v-for="f in FILTERS" :key="f.key" :class="{ on: vdata.filter === f.key }" @click="vdata.filter = f.key">{{ f.label }}</button>
  </div>
  <button v-if="$access('ENT_PC_IF_DEFINE_ADD')" class="m-btn" @click="openForm()">＋ 新增大渠道</button>

  <button v-for="d in shown" :key="d.ifCode" class="m-card m-if" @click="vdata.cur = d">
    <div class="m-if-head">
      <span class="m-if-logo" :style="{ background: d.bgColor || 'var(--jp-primary)' }">
        <img v-if="d.icon" :src="d.icon" alt="" />
        <template v-else>{{ d.ifCode.slice(0, 3).toUpperCase() }}</template>
      </span>
      <div style="flex: 1; min-width: 0; text-align: left">
        <b>{{ d.ifName }}</b>
        <div class="m-muted" style="margin: 2px 0 0"><span class="mono">{{ d.ifCode }}</span>・{{ accountCount(d.ifCode) }} 個渠道帳號</div>
      </div>
      <span :class="['m-pill', d.state === 1 ? 'ok' : 'mute']">{{ d.state === 1 ? '啟用' : '停用' }}</span>
    </div>
    <div class="m-if-ways">
      <span v-for="w in d.wayCodes || []" :key="w.wayCode" class="m-pill mute">{{ wayName(w.wayCode) }}</span>
    </div>
    <div v-if="traffic(d.ifCode)" class="m-if-today num">
      今天 {{ traffic(d.ifCode).total }} 筆・成功率 {{ rate(traffic(d.ifCode)) }}・成交 {{ ntd(traffic(d.ifCode).paidAmount) }}
    </div>
  </button>
  <div v-if="!shown.length" class="m-empty">{{ vdata.loaded ? '沒有符合的大渠道' : '載入中…' }}</div>

  <!-- 明細 -->
  <MSheet :open="!!vdata.cur" :title="vdata.cur ? vdata.cur.ifName : ''" @update:open="(v) => !v && (vdata.cur = null)">
    <template v-if="vdata.cur">
      <dl class="m-kv">
        <dt>代碼</dt><dd class="mono">{{ vdata.cur.ifCode }}</dd>
        <dt>商戶模式</dt><dd>{{ modeText(vdata.cur) }}</dd>
        <dt>支付方式</dt><dd>{{ (vdata.cur.wayCodes || []).map((w) => wayName(w.wayCode)).join('、') || '—' }}</dd>
        <dt>渠道帳號</dt><dd>{{ accountCount(vdata.cur.ifCode) }} 個</dd>
        <dt>金鑰欄位</dt><dd>{{ fieldsOf(vdata.cur.normalMchParams).map((f) => f.desc).join('、') || '—' }}</dd>
        <template v-if="vdata.cur.remark"><dt>備註</dt><dd>{{ vdata.cur.remark }}</dd></template>
      </dl>
      <div v-if="$access('ENT_PC_IF_DEFINE_EDIT')" class="m-row" style="padding: 0">
        <div class="main"><b>啟用這個大渠道</b><small>停用後新的通道設定選不到它；已在使用的商戶通道不受影響</small></div>
        <a-switch :checked="vdata.cur.state === 1" @change="(v) => setState(vdata.cur, v)" />
      </div>
      <div class="m-actions">
        <button v-if="$access('ENT_PC_IF_DEFINE_EDIT')" class="m-btn ghost" @click="openForm(vdata.cur.ifCode)">修改</button>
        <button v-if="$access('ENT_PC_IF_DEFINE_DEL')" class="m-btn danger" @click="remove(vdata.cur)">刪除</button>
      </div>
    </template>
  </MSheet>

  <!-- 新增／修改 -->
  <MSheet v-model:open="vdata.form.open" :title="vdata.form.isAdd ? '新增大渠道' : '修改大渠道'">
    <a-form layout="vertical">
      <a-form-item label="代碼" required extra="小寫英數，例如 ryo；建立後不可修改">
        <a-input v-model:value="vdata.form.data.ifCode" :disabled="!vdata.form.isAdd" size="large" />
      </a-form-item>
      <a-form-item label="名稱" required><a-input v-model:value="vdata.form.data.ifName" size="large" /></a-form-item>
      <a-form-item label="支援的支付方式" required>
        <div class="m-chips" style="flex-wrap: wrap">
          <button v-for="w in vdata.ways" :key="w.wayCode" type="button" :class="{ on: vdata.form.ways.includes(w.wayCode) }" @click="toggleWay(w.wayCode)">{{ w.wayName }}</button>
        </div>
      </a-form-item>
      <a-form-item label="普通商戶模式"><a-switch v-model:checked="vdata.form.mch" /></a-form-item>
      <a-form-item label="服務商子商戶模式"><a-switch v-model:checked="vdata.form.isv" /></a-form-item>
      <a-form-item label="金鑰欄位設定頁">
        <div class="m-seg">
          <button type="button" :class="{ on: vdata.form.data.configPageType === 1 }" @click="vdata.form.data.configPageType = 1">依欄位定義產生</button>
          <button type="button" :class="{ on: vdata.form.data.configPageType === 2 }" @click="vdata.form.data.configPageType = 2">自訂頁面</button>
        </div>
      </a-form-item>
      <template v-if="vdata.form.data.configPageType === 1">
        <a-form-item v-if="vdata.form.mch" label="普通商戶金鑰欄位（JSON）" required>
          <a-textarea v-model:value="vdata.form.data.normalMchParams" :auto-size="{ minRows: 4, maxRows: 12 }" class="mono" />
          <FieldPreview :json="vdata.form.data.normalMchParams" />
        </a-form-item>
        <template v-if="vdata.form.isv">
          <a-form-item label="服務商金鑰欄位（JSON）" required>
            <a-textarea v-model:value="vdata.form.data.isvParams" :auto-size="{ minRows: 3, maxRows: 10 }" class="mono" />
            <FieldPreview :json="vdata.form.data.isvParams" />
          </a-form-item>
          <a-form-item label="特約商戶金鑰欄位（JSON）" required>
            <a-textarea v-model:value="vdata.form.data.isvsubMchParams" :auto-size="{ minRows: 3, maxRows: 10 }" class="mono" />
            <FieldPreview :json="vdata.form.data.isvsubMchParams" />
          </a-form-item>
        </template>
      </template>
      <a-form-item label="卡片顏色"><input v-model="vdata.form.data.bgColor" type="color" style="width: 64px; height: 36px; border: 0; background: none" /></a-form-item>
      <a-form-item label="狀態">
        <a-radio-group v-model:value="vdata.form.data.state"><a-radio :value="1">啟用</a-radio><a-radio :value="0">停用</a-radio></a-radio-group>
      </a-form-item>
      <a-form-item label="備註"><a-input v-model:value="vdata.form.data.remark" size="large" /></a-form-item>
    </a-form>
    <div class="m-actions">
      <button class="m-btn ghost" @click="vdata.form.open = false">取消</button>
      <button class="m-btn" :disabled="vdata.form.saving" @click="save">儲存</button>
    </div>
  </MSheet>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, reactive, getCurrentInstance } from 'vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { API_URL_CHANNEL_ACCOUNT, API_URL_IFDEFINES_LIST, API_URL_PAYWAYS_LIST, req } from '@/api/manage'
import { ntd } from '../mobileKit'
import MSheet from '../MSheet.vue'

const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties
const FILTERS = [
  { key: 'all', label: '全部' },
  { key: 'on', label: '啟用中' },
  { key: 'off', label: '已停用' },
]
const vdata: any = reactive({ list: [], ways: [], accounts: [], channels: [], loaded: false, q: '', filter: 'all', cur: null, form: { open: false, data: {}, ways: [] } })

const shown = computed(() => {
  const q = vdata.q.trim().toLowerCase()
  return vdata.list.filter(
    (d) =>
      (vdata.filter === 'all' || (vdata.filter === 'on' ? d.state === 1 : d.state !== 1)) &&
      (!q || d.ifCode.toLowerCase().includes(q) || (d.ifName || '').toLowerCase().includes(q))
  )
})
const wayName = (code) => (vdata.ways.find((w) => w.wayCode === code) || {}).wayName || code
const accountCount = (ifCode) => vdata.accounts.filter((a) => a.ifCode === ifCode).length
const traffic = (ifCode) => vdata.channels.find((c) => c.ifCode === ifCode && Number(c.total) > 0)
const rate = (c) => (Number(c.total) ? ((Number(c.paid) / Number(c.total)) * 100).toFixed(1) + '%' : '—')
const modeText = (d) => [d.isMchMode === 1 ? '普通商戶' : '', d.isIsvMode === 1 ? '服務商子商戶' : ''].filter(Boolean).join('、') || '—'
function fieldsOf(json) {
  try {
    const list = JSON.parse(json || '[]')
    return Array.isArray(list) ? list : []
  } catch (e) {
    return []
  }
}

// 金鑰欄位 JSON 的可讀預覽：格式錯誤時直接提示
const FieldPreview = defineComponent({
  props: { json: { type: String, default: '' } },
  setup(p) {
    return () => {
      if (!p.json || !p.json.trim()) return null
      let list
      try {
        list = JSON.parse(p.json)
      } catch (e) {
        return h('div', { style: 'color:var(--jp-bad);font-size:12.5px;margin-top:6px' }, 'JSON 格式錯誤，請檢查逗號與引號')
      }
      if (!Array.isArray(list)) return h('div', { style: 'color:var(--jp-bad);font-size:12.5px;margin-top:6px' }, '必須是陣列 [ ... ]')
      return h('div', { style: 'display:flex;flex-wrap:wrap;gap:6px;margin-top:8px' },
        list.map((f) => h('span', { class: 'm-pill ' + (f.verify === 'required' ? 'warn' : 'mute') },
          `${f.desc || f.name}${f.star === '1' ? '（遮罩）' : ''}${f.verify === 'required' ? '・必填' : ''}`)))
    }
  },
})

function load() {
  req.list(API_URL_IFDEFINES_LIST, {}).then((r) => {
    vdata.list = r || []
    vdata.loaded = true
    if (vdata.cur) vdata.cur = vdata.list.find((d) => d.ifCode === vdata.cur.ifCode) || null
  })
}
function setState(d, on) {
  // 後端以 wayCodeStrs 整批覆寫支付方式，切換狀態時一併帶目前的設定
  req.updateById(API_URL_IFDEFINES_LIST, d.ifCode, { state: on ? 1 : 0, wayCodeStrs: (d.wayCodes || []).map((w) => w.wayCode).join(',') }).then(() => {
    $infoBox.message.success(on ? '已啟用' : '已停用')
    load()
  })
}
function remove(d) {
  $infoBox.confirmDanger(`刪除大渠道「${d.ifName}」？`, accountCount(d.ifCode) ? `還有 ${accountCount(d.ifCode)} 個渠道帳號使用它，系統可能拒絕刪除。` : '刪除後無法復原。', () => {
    req.delById(API_URL_IFDEFINES_LIST, d.ifCode).then(() => {
      $infoBox.message.success('已刪除')
      vdata.cur = null
      load()
    })
  })
}
function openForm(ifCode?) {
  const blank = { isMchMode: 1, isIsvMode: 0, state: 1, configPageType: 1, bgColor: '#1677ff', normalMchParams: '', isvParams: '', isvsubMchParams: '' }
  vdata.form = { open: true, saving: false, isAdd: !ifCode, data: { ...blank }, ways: [], mch: true, isv: false }
  vdata.cur = null
  if (ifCode) {
    req.getById(API_URL_IFDEFINES_LIST, ifCode).then((r) => {
      vdata.form.data = { ...r }
      vdata.form.ways = (r.wayCodes || []).map((w) => w.wayCode)
      vdata.form.mch = r.isMchMode === 1
      vdata.form.isv = r.isIsvMode === 1
    })
  }
}
function toggleWay(code) {
  const ws = vdata.form.ways
  vdata.form.ways = ws.includes(code) ? ws.filter((c) => c !== code) : ws.concat(code)
}
function jsonOk(s) {
  try {
    return Array.isArray(JSON.parse(s))
  } catch (e) {
    return false
  }
}
function save() {
  const f = vdata.form
  const d = f.data
  if (!d.ifCode || !d.ifName) return $infoBox.message.warning('請填寫代碼與名稱')
  if (f.isAdd && !/^[a-z0-9]{2,20}$/.test(d.ifCode)) return $infoBox.message.warning('代碼只能用 2～20 碼小寫英數')
  if (!f.ways.length) return $infoBox.message.warning('請至少選一個支付方式')
  if (d.configPageType === 1) {
    if (f.mch && !jsonOk(d.normalMchParams)) return $infoBox.message.warning('普通商戶金鑰欄位不是正確的 JSON 陣列')
    if (f.isv && (!jsonOk(d.isvParams) || !jsonOk(d.isvsubMchParams))) return $infoBox.message.warning('服務商金鑰欄位不是正確的 JSON 陣列')
  }
  const body = { ...d, isMchMode: f.mch ? 1 : 0, isIsvMode: f.isv ? 1 : 0, wayCodeStrs: f.ways.join(','), wayCodes: undefined }
  f.saving = true
  ;(f.isAdd ? req.add(API_URL_IFDEFINES_LIST, body) : req.updateById(API_URL_IFDEFINES_LIST, d.ifCode, body))
    .then(() => {
      $infoBox.message.success('已儲存')
      f.open = false
      load()
    })
    .finally(() => (f.saving = false))
}

load()
req.list(API_URL_PAYWAYS_LIST, { pageSize: -1 }).then((r) => (vdata.ways = r.records || []))
if ($access('ENT_CHANNEL_ACCOUNT_LIST')) req.list(API_URL_CHANNEL_ACCOUNT, {}).then((r) => (vdata.accounts = r || []))
if ($access('ENT_SYS_MONITOR')) req.list('/api/monitor/traffic', {}).then((r) => (vdata.channels = (r && r.byChannel) || []))
</script>

<style scoped lang="less">
.m-if {
  display: grid;
  gap: 10px;
  width: 100%;
  text-align: left;
  &:active {
    background: var(--jp-surface-2);
  }
}
.m-if-head {
  display: flex;
  align-items: center;
  gap: 12px;
}
.m-if-logo {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 700;
  font-size: 13px;
  letter-spacing: 0.04em;
  flex: none;
  overflow: hidden;
  img {
    max-width: 70%;
    max-height: 70%;
  }
}
.m-if-ways {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.m-if-today {
  font-size: 12.5px;
  color: var(--jp-text-2);
  padding-top: 8px;
  border-top: 1px solid var(--jp-border);
}
</style>
