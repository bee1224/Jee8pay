<template>
  <!-- 上帝：Telegram 機器人整理（名稱、API token、通知對象、用途），token 只顯示遮罩值 -->
  <div class="m-page">
    <button v-if="canEdit" class="m-btn" @click="openForm()">＋ 新增機器人</button>
    <p class="m-muted">API token 只顯示前後幾碼；要更換請在修改時重新貼上。測試連線會呼叫 Telegram 確認 token，有填通知對象時另發一則測試訊息。</p>
    <div class="m-bot-grid">
      <div v-for="b in vdata.list" :key="b.botId" class="m-card" style="display: grid; gap: 10px">
        <div style="display: flex; justify-content: space-between; gap: 8px">
          <b>{{ b.botName }}</b>
          <span :class="['m-pill', b.state === 1 ? 'ok' : 'mute']">{{ b.state === 1 ? '啟用' : '停用' }}</span>
        </div>
        <dl class="m-kv">
          <dt>帳號</dt><dd class="mono">{{ b.botUsername ? '@' + b.botUsername : '—' }}</dd>
          <dt>API token</dt><dd class="mono">{{ b.apiToken }}</dd>
          <dt>通知對象</dt><dd class="mono">{{ b.chatId || '—' }}</dd>
          <dt>用途</dt><dd>{{ b.purpose }}</dd>
          <dt>最近測試</dt><dd>{{ b.lastCheckAt ? `${b.lastCheckAt.slice(5, 16)} ${b.lastCheckResult}` : '尚未測試' }}</dd>
        </dl>
        <div v-if="canEdit" style="display: flex; flex-wrap: wrap; gap: 6px">
          <button class="m-btn sm ghost" @click="openForm(b)">修改</button>
          <button class="m-btn sm" :disabled="vdata.checking === b.botId" @click="check(b)">{{ vdata.checking === b.botId ? '測試中…' : '測試連線' }}</button>
          <button class="m-btn sm danger" @click="remove(b)">刪除</button>
        </div>
      </div>
    </div>
    <div v-if="!vdata.list.length" class="m-empty">{{ vdata.loaded ? '還沒有登記任何機器人' : '載入中…' }}</div>

    <MSheet v-model:open="vdata.form.open" :title="vdata.form.botId ? '修改機器人' : '新增機器人'">
      <a-form layout="vertical">
        <a-form-item label="名稱" required><a-input v-model:value="vdata.form.botName" :maxlength="64" size="large" /></a-form-item>
        <a-form-item :label="vdata.form.botId ? 'API token（不更換請留空）' : 'API token（向 @BotFather 取得）'" :required="!vdata.form.botId">
          <a-input-password v-model:value="vdata.form.apiToken" placeholder="123456789:AA..." autocomplete="off" size="large" />
        </a-form-item>
        <a-form-item label="通知對象 chat_id（選填）" extra="群組通常是 -100 開頭的數字">
          <a-input v-model:value="vdata.form.chatId" :maxlength="64" size="large" />
        </a-form-item>
        <a-form-item label="用途" required><a-textarea v-model:value="vdata.form.purpose" :maxlength="256" :auto-size="{ minRows: 2, maxRows: 5 }" /></a-form-item>
        <a-form-item label="狀態">
          <a-radio-group v-model:value="vdata.form.state"><a-radio :value="1">啟用</a-radio><a-radio :value="0">停用</a-radio></a-radio-group>
        </a-form-item>
        <a-form-item label="備註"><a-input v-model:value="vdata.form.remark" :maxlength="256" size="large" /></a-form-item>
      </a-form>
      <div class="m-actions">
        <button class="m-btn ghost" @click="vdata.form.open = false">取消</button>
        <button class="m-btn" :disabled="vdata.form.saving" @click="save">儲存</button>
      </div>
    </MSheet>
  </div>
</template>

<script setup lang="ts">
import { reactive, getCurrentInstance } from 'vue'
import { req } from '@/api/manage'
import MSheet from '@/views/mobile/MSheet.vue'
import '@/views/mobile/mobile.less'

const { $infoBox, $access } = getCurrentInstance()!.appContext.config.globalProperties
const URL = '/api/telegramBots'
const canEdit = $access('ENT_TELEGRAM_BOT_EDIT')
const vdata: any = reactive({ list: [], loaded: false, checking: null, form: { open: false } })

function load() {
  req.list(URL, {}).then((r) => {
    vdata.list = r || []
    vdata.loaded = true
  })
}
function openForm(b?) {
  vdata.form = b
    ? { open: true, saving: false, botId: b.botId, botName: b.botName, apiToken: '', chatId: b.chatId, purpose: b.purpose, state: b.state, remark: b.remark }
    : { open: true, saving: false, botId: null, botName: '', apiToken: '', chatId: '', purpose: '', state: 1, remark: '' }
}
function save() {
  const f = vdata.form
  if (!f.botName || !f.purpose || (!f.botId && !f.apiToken)) {
    $infoBox.message.warning('請填寫名稱、用途' + (f.botId ? '' : '與 API token'))
    return
  }
  const body = { botName: f.botName, apiToken: f.apiToken || undefined, chatId: f.chatId, purpose: f.purpose, state: f.state, remark: f.remark }
  f.saving = true
  ;(f.botId ? req.updateById(URL, f.botId, body) : req.add(URL, body))
    .then(() => {
      $infoBox.message.success('已儲存')
      f.open = false
      load()
    })
    .finally(() => (f.saving = false))
}
function check(b) {
  vdata.checking = b.botId
  req
    .add(`${URL}/${b.botId}/check`, { sendTest: true })
    .then((r) => {
      $infoBox.message.info(r.result)
      load()
    })
    .finally(() => (vdata.checking = null))
}
function remove(b) {
  $infoBox.confirmDanger(`刪除「${b.botName}」？`, '只刪除這裡的登記，不會影響 Telegram 上的機器人本身。', () => {
    req.delById(URL, b.botId).then(() => {
      $infoBox.message.success('已刪除')
      load()
    })
  })
}
load()
</script>

<style scoped lang="less">
.m-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.m-bot-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
  align-items: start;
}
@media (max-width: 400px) {
  .m-bot-grid {
    grid-template-columns: 1fr;
  }
}
</style>
