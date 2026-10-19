<template>
  <!-- 上帝頁面共用的手機版清單：摘要 → 搜尋 → 篩選 → 新增 → 卡片清單 → 點開看明細與操作。每頁只寫設定（godPages.ts） -->
  <div v-if="summary.length" class="m-stats">
    <div v-for="s in summary" :key="s.label" class="m-stat"><small>{{ s.label }}</small><strong class="num">{{ s.value }}</strong><small v-if="s.hint">{{ s.hint }}</small></div>
  </div>
  <p v-if="cfg.note" class="m-muted">{{ cfg.note }}</p>
  <div v-if="cfg.search" class="m-search">
    <SearchOutlined />
    <input v-model="vdata.q" type="search" :placeholder="cfg.search.placeholder" @keyup.enter="reload" @search="reload" />
  </div>
  <div v-if="cfg.dates" class="m-chips">
    <button v-for="d in DATES" :key="d.key" :class="{ on: vdata.date === d.key }" @click="setDate(d.key)">{{ d.label }}</button>
  </div>
  <div v-if="cfg.filters" class="m-chips">
    <button v-for="(f, i) in cfg.filters" :key="f.label" :class="{ on: vdata.filter === i }" @click="setFilter(i)">{{ f.label }}</button>
  </div>
  <button v-if="cfg.add && allowed(cfg.add)" class="m-btn" @click="runAction(cfg.add, null)">＋ {{ cfg.add.label }}</button>
  <p v-if="vdata.loaded && cfg.countText" class="m-muted">{{ cfg.countText(vdata.total, rangeLabel) }}</p>

  <div class="m-list">
    <button v-for="(r, i) in shown" :key="cfg.key ? r[cfg.key] : i" class="m-row tap" :style="{ paddingLeft: 16 + (r._depth || 0) * 18 + 'px' }" @click="open(r)">
      <div class="main">
        <b>{{ cfg.row.title(r) }}</b>
        <small v-for="(line, j) in (cfg.row.sub ? cfg.row.sub(r) : []).filter(Boolean)" :key="j">{{ line }}</small>
      </div>
      <div class="end">
        <b v-if="cfg.row.amount && cfg.row.amount(r) != null" class="num" :style="{ color: cfg.row.amountColor ? cfg.row.amountColor(r) : '' }">{{ cfg.row.amount(r) }}</b>
        <span v-if="cfg.row.pill && cfg.row.pill(r)" :class="['m-pill', cfg.row.pill(r)[1]]">{{ cfg.row.pill(r)[0] }}</span>
      </div>
    </button>
    <div v-if="!shown.length" class="m-empty">{{ vdata.loading ? '載入中…' : cfg.empty || '沒有資料' }}</div>
  </div>
  <button v-if="vdata.rows.length < vdata.total" class="m-btn ghost" :disabled="vdata.loading" @click="more">載入更多</button>

  <!-- 明細 -->
  <MSheet :open="!!vdata.cur" :title="vdata.cur ? cfg.row.title(vdata.cur) : ''" @update:open="(v) => !v && (vdata.cur = null)">
    <template v-if="vdata.cur">
      <dl class="m-kv">
        <template v-for="([k, v], i) in (cfg.detail ? cfg.detail(vdata.cur) : []).filter((x) => x && x[1] !== undefined && x[1] !== null && x[1] !== '')" :key="i">
          <dt>{{ k }}</dt><dd :class="{ mono: String(v).length > 24 && !/\s/.test(String(v)) }">{{ v }}</dd>
        </template>
      </dl>
      <pre v-if="cfg.detailBlock && cfg.detailBlock(vdata.cur)" class="m-pre">{{ cfg.detailBlock(vdata.cur) }}</pre>
      <div v-if="rowActions.length" class="m-act-grid">
        <button v-for="a in rowActions" :key="a.label" :class="['m-btn', a.cls || 'ghost']" @click="runAction(a, vdata.cur)">{{ a.label }}</button>
      </div>
    </template>
  </MSheet>

  <!-- 表單 -->
  <MSheet v-model:open="vdata.form.open" :title="vdata.form.title">
    <a-form layout="vertical">
      <a-form-item v-for="f in vdata.form.fields" :key="f.key" :label="f.label" :required="f.required" :extra="f.extra">
        <a-select v-if="f.type === 'select'" v-model:value="vdata.form.values[f.key]" :options="f.options" :mode="f.multiple ? 'multiple' : undefined" show-search option-filter-prop="label" size="large" />
        <div v-else-if="f.type === 'seg'" class="m-seg">
          <button v-for="o in f.options" :key="o.value" type="button" :class="{ on: vdata.form.values[f.key] === o.value }" @click="vdata.form.values[f.key] = o.value">{{ o.label }}</button>
        </div>
        <a-input-number v-else-if="f.type === 'number'" v-model:value="vdata.form.values[f.key]" :min="f.min" :max="f.max" :precision="f.precision ?? 2" :addon-after="f.suffix" size="large" style="width: 100%" />
        <a-textarea v-else-if="f.type === 'textarea'" v-model:value="vdata.form.values[f.key]" :auto-size="{ minRows: 2, maxRows: 8 }" :placeholder="f.placeholder" />
        <a-input v-else v-model:value="vdata.form.values[f.key]" :placeholder="f.placeholder" size="large" autocomplete="off" />
      </a-form-item>
    </a-form>
    <p v-if="vdata.form.note" class="m-muted">{{ vdata.form.note }}</p>
    <div class="m-actions">
      <button class="m-btn ghost" @click="vdata.form.open = false">取消</button>
      <button :class="['m-btn', vdata.form.cls]" :disabled="vdata.form.saving" @click="submitForm">{{ vdata.form.submitText || '儲存' }}</button>
    </div>
  </MSheet>

  <!-- 沿用電腦版的編輯器（抽屜在手機上為全寬） -->
  <component :is="c" v-for="(c, name) in cfg.editors || {}" :key="name" :ref="(el) => (editorRefs[name] = el)" :callback-func="reload" />
</template>

<script setup lang="ts">
import { computed, reactive, getCurrentInstance } from 'vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { req } from '@/api/manage'
import { todayStr } from '../mobileKit'
import MSheet from '../MSheet.vue'

const props = defineProps({ cfg: { type: Object, required: true } })
const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties
const cfg: any = props.cfg
const editorRefs: any = {}
const DATES = [
  { key: 'today', label: '今天' },
  { key: '7d', label: '近 7 天' },
  { key: 'month', label: '本月' },
  { key: 'all', label: '全部' },
]
const vdata: any = reactive({ q: '', filter: 0, date: cfg.dates ? cfg.defaultDate || 'today' : 'all', rows: [], total: 0, page: 1, loading: false, loaded: false, cur: null, extra: {}, form: { open: false, fields: [], values: {} } })

const ctx: any = { req, $infoBox, $access, reload: () => reload(), editors: editorRefs, vdata, openForm: (spec, row) => openForm(spec, row) }
const allowed = (a) => !a.access || $access(a.access)
const rowActions = computed(() => (cfg.actions || []).filter((a) => allowed(a) && (!a.show || a.show(vdata.cur, ctx))))
const summary = computed(() => (cfg.summary ? cfg.summary(vdata, ctx) || [] : []))
// 前端關鍵字過濾（cfg.search.local）用於一次載入全部的清單
const shown = computed(() => {
  const q = vdata.q.trim().toLowerCase()
  if (!cfg.search || !cfg.search.local || !q) return vdata.rows
  return vdata.rows.filter((r) => cfg.search.local(r).toLowerCase().includes(q))
})
const pad = (n) => String(n).padStart(2, '0')
const fmt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
function range(): [string, string] | null {
  const t = todayStr()
  const d = new Date()
  if (vdata.date === 'today') return [t, t]
  if (vdata.date === '7d') {
    d.setDate(d.getDate() - 6)
    return [fmt(d), t]
  }
  if (vdata.date === 'month') {
    d.setDate(1)
    return [fmt(d), t]
  }
  return null
}
const rangeLabel = computed(() => ({ today: '今天', '7d': '近 7 天', month: '本月', all: '全部' })[vdata.date] || '')

function params() {
  const p: any = { ...(cfg.params || {}), ...((cfg.filters && cfg.filters[vdata.filter].params) || {}) }
  const q = vdata.q.trim()
  if (cfg.search && !cfg.search.local && q) Object.assign(p, typeof cfg.search.param === 'function' ? cfg.search.param(q) : { [cfg.search.param]: q })
  const r = range()
  if (r && cfg.dates === 'created') Object.assign(p, { createdStart: `${r[0]} 00:00:00`, createdEnd: `${r[1]} 23:59:59` })
  if (r && cfg.dates === 'date') Object.assign(p, { startDate: r[0], endDate: r[1] })
  return p
}
function load(append = false) {
  vdata.loading = true
  const p = cfg.paged === false ? params() : { ...params(), pageNumber: vdata.page, pageSize: cfg.pageSize || 30 }
  const call = cfg.load ? cfg.load(p, ctx) : req.list(cfg.url, p)
  return Promise.resolve(call)
    .then((res) => {
      const list = Array.isArray(res) ? res : (res && res.records) || []
      const rows = cfg.transform ? cfg.transform(list, ctx) : list
      vdata.rows = append ? vdata.rows.concat(rows) : rows
      vdata.total = Array.isArray(res) ? rows.length : (res && res.total) || rows.length
      vdata.loaded = true
    })
    .finally(() => (vdata.loading = false))
}
function reload() {
  vdata.page = 1
  vdata.cur = null
  if (cfg.onReload) cfg.onReload(ctx)
  return load()
}
function more() {
  vdata.page += 1
  load(true)
}
function setDate(k) {
  vdata.date = k
  reload()
}
function setFilter(i) {
  vdata.filter = i
  reload()
}
function open(r) {
  if (cfg.onOpen) return cfg.onOpen(r, ctx)
  vdata.cur = r
  if (cfg.fetchDetail) cfg.fetchDetail(r, ctx).then((d) => vdata.cur && (vdata.cur = { ...r, ...d }))
}
function runAction(a, row) {
  if (a.form) return openForm(a, row)
  const go = () =>
    Promise.resolve(a.run(row, ctx)).then((msg) => {
      if (msg !== false) {
        if (typeof msg === 'string') $infoBox.message.success(msg)
        if (!a.keepOpen) reload()
      }
    })
  if (a.confirm) $infoBox.confirmDanger(typeof a.confirm === 'function' ? a.confirm(row) : a.confirm, '', go)
  else go()
}
function openForm(a, row) {
  const fields = (typeof a.form === 'function' ? a.form(row, ctx) : a.form).map((f) => ({ ...f, options: typeof f.options === 'function' ? f.options(ctx) : f.options }))
  const values = {}
  fields.forEach((f) => (values[f.key] = typeof f.value === 'function' ? f.value(row) : f.value))
  vdata.cur = null
  vdata.form = { open: true, saving: false, title: a.formTitle ? a.formTitle(row) : a.label, fields, values, row, submit: a.submit, note: a.note, cls: a.cls === 'danger' ? 'danger' : '', submitText: a.submitText }
}
function submitForm() {
  const f = vdata.form
  const miss = f.fields.find((x) => x.required && (f.values[x.key] === undefined || f.values[x.key] === null || f.values[x.key] === '' || (Array.isArray(f.values[x.key]) && !f.values[x.key].length)))
  if (miss) return $infoBox.message.warning('請填寫' + miss.label)
  f.saving = true
  Promise.resolve(f.submit(f.values, f.row, ctx))
    .then((msg) => {
      if (msg === false) return
      $infoBox.message.success(typeof msg === 'string' ? msg : '已儲存')
      f.open = false
      reload()
    })
    .finally(() => (f.saving = false))
}

if (cfg.init) cfg.init(ctx)
load()
</script>

<style scoped lang="less">
.m-act-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 10px;
}
.m-pre {
  margin: 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--jp-surface-2);
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 260px;
  overflow: auto;
}
</style>
