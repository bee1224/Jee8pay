<template>
  <!-- 技術人員：API 測試。以商戶身分打支付服務，完整列出 商戶→平台、平台→商戶、平台→渠道 的請求與回應 -->
  <div class="m-page at-page">
    <div class="m-seg">
      <button v-for="t in TABS" :key="t.key" :class="{ on: vdata.tab === t.key }" @click="vdata.tab = t.key">{{ t.label }}</button>
    </div>

    <div v-if="vdata.tab !== 'verify'" class="m-card at-form">
      <a-form layout="vertical">
        <div class="at-grid">
          <a-form-item :label="$t('商戶')" required>
            <a-select v-model:value="vdata.mchNo" show-search option-filter-prop="label" size="large" :placeholder="$t('選擇商戶')" :options="mchOptions" @change="onMch" />
          </a-form-item>
          <a-form-item :label="$t('應用')" required>
            <a-select v-model:value="vdata.appId" size="large" :placeholder="$t('選擇應用')" :options="appOptions" @change="onApp" />
          </a-form-item>
          <template v-if="vdata.tab === 'order'">
            <a-form-item :label="$t('支付方式')" required>
              <a-select v-model:value="vdata.wayCode" size="large" :placeholder="$t('選擇支付方式')" :options="wayOptions" :not-found-content="vdata.appId ? $t('這個應用沒有開通支付方式') : $t('請先選擇應用')" />
            </a-form-item>
            <a-form-item :label="$t('金額（元）')" required>
              <a-input-number v-model:value="vdata.amount" :min="1" :max="20000" :precision="0" size="large" style="width: 100%" />
            </a-form-item>
          </template>
          <template v-else>
            <a-form-item :label="$t('支付單號')"><a-input v-model:value="vdata.payOrderId" size="large" allow-clear :placeholder="$t('P 開頭')" /></a-form-item>
            <a-form-item :label="$t('商戶單號')"><a-input v-model:value="vdata.mchOrderNo" size="large" allow-clear :placeholder="$t('擇一填寫')" /></a-form-item>
          </template>
        </div>
        <template v-if="vdata.tab === 'order'">
          <p class="m-muted" style="margin: 0 0 10px">{{ $t('繳款人資料（ibon 上游必填；留空的欄位不送出，可用來觀察上游的錯誤回應）') }}</p>
          <div class="at-grid">
            <a-form-item v-for="f in PAYER" :key="f.key" :label="f.label"><a-input v-model:value="vdata.payer[f.key]" size="large" allow-clear /></a-form-item>
          </div>
        </template>
      </a-form>
      <p class="m-muted" style="margin: 0 0 12px">
        {{ vdata.tab === 'order'
          ? $t('會真的建立訂單並送到上游渠道（標題「API 測試單」，不繳費即逾期關閉）。平台不持有商戶私鑰，請求以平台內部金鑰簽名（PLATFORM-ML-DSA-65），其餘與商戶自己串接相同。')
          : $t('查詢的是平台本地訂單狀態（與商戶呼叫查單 API 相同），不會打上游。') }}
      </p>
      <button class="m-btn" :disabled="vdata.running" @click="run">{{ vdata.running ? $t('送出中…') : vdata.tab === 'order' ? $t('送出下單') : $t('送出查單') }}</button>
    </div>

    <div v-else class="m-card at-form">
      <a-form layout="vertical">
        <a-form-item :label="$t('商戶送出的請求 JSON（含 sign）')" required>
          <a-textarea v-model:value="vdata.verifyJson" :rows="8" placeholder='{"mchNo":"M...","appId":"...","signType":"ML-DSA-65","reqTime":"...","sign":"..."}' class="mono" />
        </a-form-item>
      </a-form>
      <p class="m-muted" style="margin: 0 0 12px">{{ $t('貼上商戶實際送出的請求本文，平台會以該應用登記的公鑰驗簽，並列出平台組出的待簽字串，方便和商戶逐字比對。不會送出任何請求。') }}</p>
      <button class="m-btn" :disabled="vdata.running" @click="verify">{{ vdata.running ? $t('驗證中…') : $t('驗證簽名') }}</button>
    </div>

    <!-- 下單／查單結果 -->
    <template v-if="vdata.tab !== 'verify' && vdata.result">
      <div v-if="vdata.result.error" class="m-card at-error">{{ vdata.result.error }}</div>

      <div class="m-section-title"><h3>{{ $t('① 商戶 → 平台（請求）') }}</h3><span class="m-muted">POST</span></div>
      <div class="m-card at-block">
        <div class="at-line mono">{{ vdata.result.method }} {{ vdata.result.url }}</div>
        <CodeBox title="Headers" :text="pretty(vdata.result.requestHeaders)" />
        <CodeBox title="Body" :text="pretty(vdata.result.requestBody)" />
        <CodeBox :title="$t('待簽字串（canonical）')" :text="vdata.result.canonical" collapsed />
      </div>

      <div class="m-section-title">
        <h3>{{ $t('② 平台 → 商戶（回應）') }}</h3>
        <span class="m-muted">{{ vdata.result.httpStatus ? 'HTTP ' + vdata.result.httpStatus : '' }} {{ vdata.result.latencyMillis != null ? vdata.result.latencyMillis + ' ms' : '' }}</span>
      </div>
      <div class="m-card at-block">
        <div v-if="vdata.result.responseSignCheck" class="at-line">
          {{ $t('回應驗簽：') }}<span :class="['m-pill', vdata.result.responseSignCheck.startsWith('通過') ? 'ok' : vdata.result.responseSignCheck.startsWith('失敗') ? 'bad' : 'mute']">{{ vdata.result.responseSignCheck }}</span>
        </div>
        <CodeBox title="Headers" :text="pretty(vdata.result.responseHeaders)" />
        <CodeBox title="Body" :text="pretty(vdata.result.responseBody)" />
      </div>

      <div class="m-section-title"><h3>{{ $t('③ 平台 → 渠道（上游往來）') }}</h3><span class="m-muted">{{ $t('{0} 次', [(vdata.result.channelTrace || []).length]) }}</span></div>
      <div v-if="!(vdata.result.channelTrace || []).length" class="m-empty">
        {{ vdata.tab === 'order' ? $t('這次沒有打到上游（可能在平台端就被擋下，請看上方回應）') : $t('查單只查平台本地訂單，不會打上游') }}
      </div>
      <div v-for="(t, i) in vdata.result.channelTrace || []" :key="i" class="m-card at-block">
        <div class="at-line">
          <b>#{{ i + 1 }} {{ t.provider }}</b>
          <span :class="['m-pill', t.status >= 200 && t.status < 300 ? 'ok' : 'bad']">{{ t.status ? 'HTTP ' + t.status : $t('連線失敗') }}</span>
          <span class="m-muted">{{ t.latencyMillis }} ms</span>
        </div>
        <div class="at-line mono">{{ t.method }} {{ t.url }}</div>
        <div v-if="t.error" class="at-error">{{ t.error }}</div>
        <CodeBox title="Request headers" :text="pretty(t.headers)" />
        <CodeBox title="Request body" :text="pretty(t.body)" />
        <CodeBox title="Response body" :text="pretty(t.responseBody)" />
      </div>
      <p class="m-muted">{{ $t('渠道往來中的密碼、Bearer token、access_token 已遮蔽為 ****。') }}</p>
    </template>

    <!-- 簽名驗證結果 -->
    <template v-if="vdata.tab === 'verify' && vdata.verifyResult">
      <div class="m-list">
        <div v-for="c in vdata.verifyResult.checks" :key="c.item" class="m-row">
          <div class="main"><b class="mono">{{ c.item }}</b><small>{{ c.message }}</small></div>
          <span :class="['m-pill', c.ok ? 'ok' : 'bad']">{{ c.ok ? $t('通過') : $t('不通過') }}</span>
        </div>
      </div>
      <div class="m-card at-block">
        <CodeBox :title="$t('平台組出的待簽字串（canonical）')" :text="vdata.verifyResult.canonical" />
      </div>
      <p class="m-muted">{{ vdata.verifyResult.note }}</p>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, reactive, ref, getCurrentInstance } from 'vue'
import { req } from '@/api/manage'
import '@/views/mobile/mobile.less'
import { t } from '@/i18n'

const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties
const URL = '/api/apiTest'
const TABS = [
  { key: 'order', label: t('下單測試') },
  { key: 'query', label: t('查單測試') },
  { key: 'verify', label: t('簽名驗證') },
]
const PAYER = [
  { key: 'payerName', label: t('繳款人姓名') },
  { key: 'payerMobile', label: t('手機') },
  { key: 'payerEmail', label: 'Email' },
  { key: 'payerPostcode', label: t('郵遞區號') },
  { key: 'payerAddress', label: t('地址') },
]
// 測試用假資料，技術人員可自行修改
const vdata: any = reactive({
  tab: 'order', merchants: [], mchNo: undefined, appId: undefined, wayCode: undefined, amount: 100,
  payer: { payerName: t('測試人員'), payerMobile: '0912345678', payerEmail: 'test@example.com', payerPostcode: '100', payerAddress: t('台北市中正區測試路 1 號') },
  payOrderId: '', mchOrderNo: '', verifyJson: '', running: false, result: null, verifyResult: null,
})

const curMch = computed(() => vdata.merchants.find((m) => m.mchNo === vdata.mchNo))
const curApp = computed(() => (curMch.value ? curMch.value.apps.find((a) => a.appId === vdata.appId) : null))
const mchOptions = computed(() => vdata.merchants.map((m) => ({ value: m.mchNo, label: `${m.mchName}（${m.mchNo}）${m.state === 1 ? '' : t('・停用')}` })))
const appOptions = computed(() => (curMch.value ? curMch.value.apps.map((a) => ({ value: a.appId, label: `${a.appName || a.appId}${a.state === 1 ? '' : t('・停用')}` })) : []))
const wayOptions = computed(() => (curApp.value ? curApp.value.ways.map((w) => ({ value: w.wayCode, label: `${w.wayName}（${w.wayCode}）` })) : []))

function onMch() {
  const apps = curMch.value ? curMch.value.apps : []
  vdata.appId = apps.length === 1 ? apps[0].appId : undefined
  onApp()
}
function onApp() {
  const ways = curApp.value ? curApp.value.ways : []
  vdata.wayCode = ways.length ? ways[0].wayCode : undefined
}

function pretty(v) {
  if (v == null || v === '') return ''
  if (typeof v !== 'string') return JSON.stringify(v, null, 2)
  try {
    return JSON.stringify(JSON.parse(v), null, 2)
  } catch (e) {
    return v
  }
}

function run() {
  if (!vdata.mchNo || !vdata.appId) return $infoBox.message.warning(t('請選擇商戶與應用'))
  let params: any = { mchNo: vdata.mchNo, appId: vdata.appId }
  if (vdata.tab === 'order') {
    if (!vdata.wayCode || !vdata.amount) return $infoBox.message.warning(t('請選擇支付方式並填寫金額'))
    params = { ...params, wayCode: vdata.wayCode, amount: vdata.amount, ...vdata.payer }
  } else {
    if (!vdata.payOrderId && !vdata.mchOrderNo) return $infoBox.message.warning(t('請填寫支付單號或商戶單號'))
    params = { ...params, payOrderId: vdata.payOrderId, mchOrderNo: vdata.mchOrderNo }
  }
  const tab = vdata.tab
  vdata.running = true
  vdata.result = null
  req
    .add(`${URL}/${tab === 'order' ? 'unifiedOrder' : 'query'}`, params)
    .then((r) => {
      vdata.result = r
      // 下單成功後帶入支付單號，方便直接切到查單
      try {
        const id = JSON.parse(r.responseBody).data.payOrderId
        if (tab === 'order' && id) vdata.payOrderId = id
      } catch (e) { /* 回應不是預期格式時略過 */ }
    })
    .finally(() => (vdata.running = false))
}

function verify() {
  if (!vdata.verifyJson.trim()) return $infoBox.message.warning(t('請貼上請求 JSON'))
  vdata.running = true
  vdata.verifyResult = null
  req
    .add(`${URL}/verifySign`, { requestJson: vdata.verifyJson })
    .then((r) => (vdata.verifyResult = r))
    .finally(() => (vdata.running = false))
}

function copy(text) {
  const done = () => $infoBox.message.success(t('已複製'))
  if (navigator.clipboard && window.isSecureContext) return navigator.clipboard.writeText(text).then(done)
  const ta = document.createElement('textarea')
  ta.value = text
  document.body.appendChild(ta)
  ta.select()
  document.execCommand('copy')
  ta.remove()
  done()
}

// 程式碼區塊：標題列含複製與收合，內容自動換行，手機上不會橫向溢出
const CodeBox = defineComponent({
  props: { title: String, text: String, collapsed: Boolean },
  setup(props) {
    const open = ref(!props.collapsed)
    return () =>
      props.text
        ? h('div', { class: 'at-code' }, [
            h('div', { class: 'at-code-head' }, [
              h('button', { class: 'at-code-title', onClick: () => (open.value = !open.value) }, `${open.value ? '▼' : '▶'} ${props.title}`),
              h('button', { class: 'm-btn sm ghost', onClick: () => copy(props.text) }, t('複製')),
            ]),
            open.value ? h('pre', { class: 'mono' }, props.text) : null,
          ])
        : null
  },
})

req.list(`${URL}/options`, {}).then((r) => (vdata.merchants = r || []))
</script>

<style scoped lang="less">
.at-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.at-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 0 16px;
}
.at-block {
  display: grid;
  gap: 10px;
  min-width: 0;
}
.at-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  overflow-wrap: anywhere;
  min-width: 0;
}
.at-error {
  color: var(--jp-bad);
  background: var(--jp-bad-soft);
  border-radius: 10px;
  padding: 10px 12px;
  overflow-wrap: anywhere;
}
:deep(.at-code) {
  border: 1px solid var(--jp-border);
  border-radius: 10px;
  overflow: hidden;
  min-width: 0;
  .at-code-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 8px;
    padding: 6px 8px 6px 12px;
    background: var(--jp-surface-2);
  }
  .at-code-title {
    border: 0;
    background: none;
    color: var(--jp-text-2);
    font: inherit;
    font-size: 13px;
    cursor: pointer;
    padding: 0;
    text-align: left;
  }
  pre {
    margin: 0;
    padding: 10px 12px;
    font-size: 12px;
    line-height: 1.55;
    white-space: pre-wrap;
    word-break: break-all;
    max-height: 420px;
    overflow: auto;
    color: var(--jp-text);
    background: var(--jp-surface);
  }
}
</style>
