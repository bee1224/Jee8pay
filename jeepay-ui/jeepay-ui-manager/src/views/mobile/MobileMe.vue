<template>
  <!-- 我的：只放摘要（身分、上次登入）；修改密碼、外觀、登入紀錄點進去才展開。代理只看得到時間與完整 IP，裝置只有平台（上帝）看得到 -->
  <div class="m-card m-hello">
    <div class="m-avatar">
      <img v-if="u.avatarUrl" :src="u.avatarUrl" alt="" />
      <template v-else>{{ (u.realname || '?').slice(0, 1) }}</template>
    </div>
    <div style="min-width: 0">
      <b>{{ u.realname }}<span class="m-role">{{ roleLabel }}</span></b>
      <span v-if="vdata.me">代理編號 <span class="mono">{{ vdata.me.agentNo }}</span></span>
      <span v-else class="mono">{{ u.loginUsername }}</span>
    </div>
  </div>

  <div class="m-card">
    <div class="m-section-title" style="margin: 0 0 10px"><h3>上次登入</h3></div>
    <dl v-if="lastLogin" class="m-kv">
      <dt>時間</dt><dd class="num">{{ lastLogin.createdAt }}</dd>
      <dt>IP</dt><dd class="mono">{{ lastLogin.userIp }}</dd>
      <template v-if="!isAgentUser"><dt>裝置</dt><dd>{{ deviceText(lastLogin) }}</dd></template>
    </dl>
    <p v-else class="m-muted" style="margin: 0">{{ vdata.loaded ? '這是第一筆登入紀錄。' : '載入中…' }}</p>
  </div>

  <div class="m-list">
    <button class="m-row tap" @click="vdata.history = true">
      <HistoryOutlined style="font-size: 20px" />
      <div class="main"><b>登入紀錄</b><small>最近 5 次的時間與 IP{{ isAgentUser ? '' : '、裝置' }}</small></div>
      <RightOutlined style="color: var(--jp-text-3)" />
    </button>
    <button class="m-row tap" @click="vdata.theme = true">
      <BgColorsOutlined style="font-size: 20px" />
      <div class="main"><b>外觀</b><small>{{ THEME_LABELS[themePref] }}{{ themePref === 'auto' ? `（目前${isDark ? '深色' : '亮色'}）` : '' }}</small></div>
      <RightOutlined style="color: var(--jp-text-3)" />
    </button>
    <button class="m-row tap" @click="vdata.pwd.open = true">
      <LockOutlined style="font-size: 20px" />
      <div class="main"><b>修改密碼</b></div>
      <RightOutlined style="color: var(--jp-text-3)" />
    </button>
  </div>
  <p v-if="isAgentUser" class="m-muted" style="margin-top: -6px">姓名、手機號等個人資料只能由平台修改。</p>

  <button class="m-btn danger" @click="vdata.logout = true">退出登入</button>

  <MSheet v-model:open="vdata.history" title="登入紀錄">
    <div class="m-list">
      <div v-for="(l, i) in vdata.logins" :key="i" class="m-row">
        <div class="main">
          <b class="num">{{ l.createdAt }}<span v-if="i === 0" class="m-pill ok" style="margin-left: 8px">本次</span></b>
          <small><span class="mono">{{ l.userIp }}</span><template v-if="!isAgentUser">・{{ deviceText(l) }}</template></small>
        </div>
      </div>
      <div v-if="!vdata.logins.length" class="m-empty">還沒有登入紀錄</div>
    </div>
  </MSheet>

  <MSheet v-model:open="vdata.theme" title="外觀">
    <div class="m-list">
      <button v-for="p in prefs" :key="p" class="m-row tap" @click="setThemePref(p)">
        <div class="main"><b>{{ THEME_LABELS[p] }}</b><small>{{ PREF_HINT[p] }}</small></div>
        <CheckOutlined v-if="themePref === p" style="color: var(--jp-primary); font-size: 18px" />
      </button>
    </div>
  </MSheet>

  <MSheet v-model:open="vdata.pwd.open" title="修改密碼">
    <a-form layout="vertical">
      <a-form-item label="原密碼"><a-input-password v-model:value="vdata.pwd.originalPwd" autocomplete="current-password" size="large" /></a-form-item>
      <a-form-item label="新密碼（6～12 碼）"><a-input-password v-model:value="vdata.pwd.newPwd" autocomplete="new-password" size="large" /></a-form-item>
      <a-form-item label="確認新密碼"><a-input-password v-model:value="vdata.pwd.confirmPwd" autocomplete="new-password" size="large" /></a-form-item>
    </a-form>
    <div class="m-actions">
      <button class="m-btn ghost" @click="vdata.pwd.open = false">取消</button>
      <button class="m-btn" :disabled="vdata.pwd.saving" @click="savePwd">儲存</button>
    </div>
  </MSheet>

  <MSheet v-model:open="vdata.logout" title="退出登入？">
    <p class="m-muted" style="text-align: center">退出後需要重新輸入帳號、密碼和驗證碼。</p>
    <div class="m-actions">
      <button class="m-btn ghost" @click="vdata.logout = false">取消</button>
      <button class="m-btn danger" @click="userStore.logout()">退出</button>
    </div>
  </MSheet>
</template>

<script setup lang="ts">
import { computed, reactive, getCurrentInstance } from 'vue'
import { LockOutlined, RightOutlined, BgColorsOutlined, HistoryOutlined, CheckOutlined } from '@ant-design/icons-vue'
import { Base64 } from 'js-base64'
import { req, updateUserPass } from '@/api/manage'
import { useUserStore } from '@/store/modules/user'
import { isDark, themePref, setThemePref, THEME_LABELS, ThemePref } from '@/utils/colorScheme'
import { agentMe } from './mobileKit'
import MSheet from './MSheet.vue'

const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties
const userStore = useUserStore()
const u: any = computed(() => userStore.userInfo)
const belong = userStore.userInfo['belongInfoId']
const isAgentUser = !!belong && belong !== '0'
const prefs: ThemePref[] = ['auto', 'light', 'dark']
const PREF_HINT: Record<ThemePref, string> = { auto: '依手機或電腦的深色模式自動切換', light: '一律使用亮色', dark: '一律使用深色' }
const vdata: any = reactive({
  me: null,
  logins: [],
  loaded: false,
  history: false,
  theme: false,
  logout: false,
  pwd: { open: false, saving: false, originalPwd: '', newPwd: '', confirmPwd: '' },
})

const roleLabel = computed(() => (!isAgentUser ? '平台' : !vdata.me ? '代理' : vdata.me.agentLevel === 1 ? '團長' : '隊長'))
// 第一筆是這次登入，「上次登入」取前一筆
const lastLogin = computed(() => vdata.logins[1] || null)
function deviceText(l) {
  return [l.deviceModel, l.deviceOs].filter(Boolean).join('・') || '未知裝置'
}

if (isAgentUser) agentMe().then((me) => (vdata.me = me)).catch(() => {})
req.list('/api/current/loginDevices', {}).then((r) => {
  vdata.logins = r || []
  vdata.loaded = true
})

function savePwd() {
  const p = vdata.pwd
  if (!p.originalPwd || !p.newPwd || p.newPwd.length < 6 || p.newPwd.length > 12) {
    $infoBox.message.warning('請輸入原密碼與 6～12 碼的新密碼')
    return
  }
  if (p.newPwd !== p.confirmPwd) {
    $infoBox.message.warning('兩次輸入的新密碼不一致')
    return
  }
  p.saving = true
  updateUserPass({ recordId: userStore.userInfo.sysUserId, originalPwd: Base64.encode(p.originalPwd), confirmPwd: Base64.encode(p.confirmPwd), newPwd: '' })
    .then(() => {
      $infoBox.message.success('密碼已更新，請重新登入')
      setTimeout(() => userStore.logout(), 1000)
    })
    .finally(() => (p.saving = false))
}
</script>
