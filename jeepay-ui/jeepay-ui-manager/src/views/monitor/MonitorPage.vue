<template>
  <!-- 上帝：流量監控與容量上限（GET /api/monitor/traffic，每 30 秒自動更新；上限只提醒不擋單） -->
  <div class="m-page">
    <div class="m-stats">
      <div class="m-stat">
        <small>最近 1 分鐘建單</small><strong class="num">{{ d.lastMinute || 0 }}</strong>
        <CapBar :value="d.lastMinute" :max="cap.monitorMaxOrdersPerMinute" :warn="cap.monitorWarnPercent" />
      </div>
      <div class="m-stat">
        <small>今日建單</small><strong class="num">{{ num(t.total) }}</strong>
        <CapBar :value="t.total" :max="cap.monitorMaxDailyOrders" :warn="cap.monitorWarnPercent" />
      </div>
      <div class="m-stat">
        <small>今日成交</small><strong class="num">{{ ntd(t.paidAmount) }}</strong>
        <CapBar :value="t.paidAmount" :max="cap.monitorMaxDailyAmount" :warn="cap.monitorWarnPercent" />
      </div>
      <div class="m-stat">
        <small>今日成功率</small><strong class="num">{{ rate(t.paid, t.total) }}</strong>
        <small>支付中 {{ num(t.waiting) }} 筆</small>
      </div>
    </div>

    <div class="m-section-title">
      <h3>最近 60 分鐘（每分鐘建單）</h3>
      <span class="m-muted">今日尖峰 {{ d.peakMinute ? `${d.peakMinute.m}・${d.peakMinute.c}` : '—' }}</span>
    </div>
    <div class="m-card">
      <svg :viewBox="`0 -6 ${W} ${H + 22}`" width="100%" role="img" aria-label="最近 60 分鐘每分鐘建單數長條圖">
        <defs><linearGradient id="jpBarMon" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#7dd3fc" /><stop offset="0.55" stop-color="#2563eb" /><stop offset="1" stop-color="#1e40af" /></linearGradient></defs>
        <rect v-for="(b, i) in minuteBars" :key="i" :x="i * bw" :y="H - b.h" :width="bw - 1" :height="b.h" :fill="b.warn ? 'var(--jp-warn)' : 'url(#jpBarMon)'" :opacity="i === minuteBars.length - 1 ? 1 : 0.55" />
        <template v-if="cap.monitorMaxOrdersPerMinute">
          <line x1="0" :x2="W" :y1="capY" :y2="capY" stroke="var(--jp-bad)" stroke-dasharray="4 3" />
          <text :x="W - 2" :y="capY - 3" text-anchor="end" font-size="10" fill="var(--jp-bad)">上限 {{ cap.monitorMaxOrdersPerMinute }}</text>
        </template>
        <text x="0" :y="H + 16" font-size="10" fill="var(--jp-text-3)">{{ minuteBars.length ? minuteBars[0].m : '' }}</text>
        <text :x="W" :y="H + 16" text-anchor="end" font-size="10" fill="var(--jp-text-3)">{{ minuteBars.length ? minuteBars[minuteBars.length - 1].m : '' }}</text>
      </svg>
      <p v-if="!minuteBars.length" class="m-muted" style="margin: 0">最近 60 分鐘沒有建單</p>
    </div>

    <div class="m-section-title"><h3>今天每小時</h3></div>
    <div class="m-card m-table-wrap">
      <table class="num">
        <thead><tr><th>時段</th><th>建單</th><th>成功</th><th>成交</th></tr></thead>
        <tbody>
          <tr v-for="h in d.perHour || []" :key="h.h"><td>{{ String(h.h).padStart(2, '0') }}:00</td><td>{{ num(h.total) }}</td><td>{{ num(h.paid) }}</td><td>{{ ntd(h.paidAmount) }}</td></tr>
          <tr v-if="!(d.perHour || []).length"><td colspan="4" style="text-align: center">今天還沒有訂單</td></tr>
        </tbody>
      </table>
    </div>

    <div class="m-section-title"><h3>各上游（今天）</h3></div>
    <div class="m-card m-table-wrap">
      <table class="num">
        <thead><tr><th>上游</th><th>建單</th><th>成功率</th><th>失敗</th><th>成交</th></tr></thead>
        <tbody>
          <tr v-for="c in d.byChannel || []" :key="c.ifCode"><td>{{ (c.ifCode || '未分配').toUpperCase() }}</td><td>{{ num(c.total) }}</td><td>{{ rate(c.paid, c.total) }}</td><td>{{ num(c.failed) }}</td><td>{{ ntd(c.paidAmount) }}</td></tr>
          <tr v-if="!(d.byChannel || []).length"><td colspan="5" style="text-align: center">今天還沒有訂單</td></tr>
        </tbody>
      </table>
    </div>

    <div class="m-section-title"><h3>積壓與系統</h3><span class="m-muted">更新於 {{ (d.now || '').slice(11) }}</span></div>
    <div class="m-card">
      <dl class="m-kv">
        <dt>通知商戶重試中</dt><dd class="num" :style="{ color: d.notifyPending > 20 ? 'var(--jp-bad)' : '' }">{{ num(d.notifyPending) }} 筆</dd>
        <dt>管理服務記憶體</dt><dd class="num">{{ sys.jvmUsedMb || 0 }}／{{ sys.jvmMaxMb || 0 }} MB</dd>
        <dt>資料庫連線</dt><dd class="num">{{ sys.dbConnections ?? '—' }}／{{ sys.dbMaxConnections ?? '—' }}</dd>
      </dl>
    </div>

    <div class="m-section-title">
      <h3>容量上限</h3>
      <button v-if="$access('ENT_SYS_MONITOR_EDIT')" class="m-btn sm ghost" @click="openCap">修改</button>
    </div>
    <div class="m-card">
      <dl class="m-kv">
        <dt>每分鐘建單</dt><dd class="num">{{ cap.monitorMaxOrdersPerMinute || '不設' }}</dd>
        <dt>每日建單</dt><dd class="num">{{ cap.monitorMaxDailyOrders ? num(cap.monitorMaxDailyOrders) : '不設' }}</dd>
        <dt>每日成交</dt><dd class="num">{{ cap.monitorMaxDailyAmount ? ntd(cap.monitorMaxDailyAmount) : '不設' }}</dd>
        <dt>警示門檻</dt><dd class="num">{{ cap.monitorWarnPercent }}%</dd>
      </dl>
    </div>
    <p class="m-muted">容量上限只用來提醒（超過警示門檻變黃、超過上限變紅），不會擋單。</p>

    <MSheet v-model:open="vdata.form.open" title="容量上限">
      <a-form layout="vertical">
        <a-form-item label="每分鐘建單上限（0 = 不設）"><a-input-number v-model:value="vdata.form.perMinute" :min="0" :precision="0" size="large" style="width: 100%" /></a-form-item>
        <a-form-item label="每日建單上限"><a-input-number v-model:value="vdata.form.dailyOrders" :min="0" :precision="0" size="large" style="width: 100%" /></a-form-item>
        <a-form-item label="每日成交上限（元）"><a-input-number v-model:value="vdata.form.dailyYuan" :min="0" :precision="0" size="large" style="width: 100%" /></a-form-item>
        <a-form-item label="警示門檻（%）"><a-input-number v-model:value="vdata.form.warn" :min="1" :max="100" :precision="0" size="large" style="width: 100%" /></a-form-item>
      </a-form>
      <div class="m-actions">
        <button class="m-btn ghost" @click="vdata.form.open = false">取消</button>
        <button class="m-btn" @click="saveCap">儲存</button>
      </div>
    </MSheet>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onBeforeUnmount, reactive, getCurrentInstance } from 'vue'
import { req } from '@/api/manage'
import request from '@/http/request'
import MSheet from '@/views/mobile/MSheet.vue'
import { ntd } from '@/views/mobile/mobileKit'
import '@/views/mobile/mobile.less'

const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties
const W = 320
const H = 90
const vdata: any = reactive({ data: {}, form: { open: false } })
const d = computed(() => vdata.data || {})
const t = computed(() => d.value.today || {})
const sys = computed(() => d.value.system || {})
const cap = computed(() => d.value.capacity || { monitorWarnPercent: 80 })

const num = (v) => Number(v || 0).toLocaleString('zh-TW')
const rate = (p, total) => (Number(total) ? ((Number(p) / Number(total)) * 100).toFixed(1) + '%' : '—')
const maxPM = computed(() => Math.max(1, Number(cap.value.monitorMaxOrdersPerMinute || 0), ...(d.value.perMinute || []).map((x) => Number(x.total))))
const bw = computed(() => W / Math.max(60, (d.value.perMinute || []).length))
const capY = computed(() => H - (Number(cap.value.monitorMaxOrdersPerMinute || 0) / maxPM.value) * H)
const minuteBars = computed(() =>
  (d.value.perMinute || []).map((x) => {
    const limit = Number(cap.value.monitorMaxOrdersPerMinute || 0)
    return { m: x.m, h: (Number(x.total) / maxPM.value) * H, warn: limit > 0 && Number(x.total) >= (limit * Number(cap.value.monitorWarnPercent || 80)) / 100 }
  })
)

// 使用率條：未設上限顯示灰字；超過警示門檻黃色、超過上限紅色
const CapBar = defineComponent({
  props: { value: { type: Number, default: 0 }, max: { type: Number, default: 0 }, warn: { type: Number, default: 80 } },
  setup(p) {
    return () => {
      if (!p.max) return h('small', { class: 'm-muted', style: 'margin:0' }, '未設上限')
      const pct = Math.round((Number(p.value || 0) / p.max) * 100)
      const color = pct >= 100 ? 'var(--jp-bad)' : pct >= p.warn ? 'var(--jp-warn)' : 'var(--jp-ok)'
      return h('div', { style: 'display:grid;gap:3px' }, [
        h('div', { style: 'height:8px;border-radius:4px;background:var(--jp-surface-2);overflow:hidden' }, [h('div', { style: `width:${Math.min(100, pct)}%;height:100%;background:${color}` })]),
        h('small', { style: `color:${color}` }, `${pct}%`),
      ])
    }
  },
})

function load() {
  req.list('/api/monitor/traffic', {}).then((r) => (vdata.data = r || {}))
}
function openCap() {
  const c = cap.value
  vdata.form = { open: true, perMinute: c.monitorMaxOrdersPerMinute, dailyOrders: c.monitorMaxDailyOrders, dailyYuan: Math.round((c.monitorMaxDailyAmount || 0) / 100), warn: c.monitorWarnPercent }
}
function saveCap() {
  const f = vdata.form
  request
    .request({
      url: '/api/monitor/capacity',
      method: 'put',
      data: { monitorMaxOrdersPerMinute: f.perMinute || 0, monitorMaxDailyOrders: f.dailyOrders || 0, monitorMaxDailyAmount: Math.round((f.dailyYuan || 0) * 100), monitorWarnPercent: f.warn || 80 },
    })
    .then(() => {
      $infoBox.message.success('已儲存')
      f.open = false
      load()
    })
}
load()
const timer = setInterval(load, 30000)
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped lang="less">
.m-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 960px;
}
</style>
