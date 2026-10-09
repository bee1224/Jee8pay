<template>
  <!-- 品牌設定（白標）：團長的站台名稱與 Logo，自己與旗下代理登入後套用 -->
  <div style="max-width: 560px">
    <a-alert type="info" show-icon style="margin-bottom: 16px"
      message="啟用後，你和旗下代理登入營運平台時，左上角與分頁標題會換成這裡設定的名稱與 Logo。商戶平台不受影響。" />
    <a-form layout="vertical">
      <a-form-item label="啟用品牌"><a-switch v-model:checked="vdata.enabled" /></a-form-item>
      <a-form-item label="站台名稱（最多 32 字）"><a-input v-model:value="vdata.title" :maxlength="32" placeholder="例如：某某金流" /></a-form-item>
      <a-form-item label="Logo（建議 190×40，透明底 PNG）">
        <div style="display: flex; align-items: center; gap: 12px">
          <img v-if="vdata.logo" :src="vdata.logo" alt="logo" style="height: 40px; max-width: 220px; border: 1px dashed #ddd; padding: 2px" />
          <a-upload :show-upload-list="false" accept=".png,.jpg,.jpeg,.gif" :custom-request="upload">
            <a-button :loading="vdata.uploading">{{ vdata.logo ? '更換圖片' : '上傳圖片' }}</a-button>
          </a-upload>
          <a-button v-if="vdata.logo" type="link" danger @click="vdata.logo = ''">移除</a-button>
        </div>
      </a-form-item>
      <a-form-item><a-button type="primary" :loading="vdata.saving" @click="save">儲存</a-button></a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { API_URL_AGENT_PORTAL, req } from '@/api/manage'
import request from '@/http/request'
import { reactive, getCurrentInstance } from 'vue'
const { $infoBox } = getCurrentInstance()!.appContext.config.globalProperties
const props = defineProps({ me: { type: Object, required: true } })
const emit = defineEmits(['saved'])

const vdata: any = reactive({
  enabled: props.me.brandEnabled === 1,
  title: props.me.brandTitle || '',
  logo: props.me.brandLogo || '',
  uploading: false,
  saving: false,
})
function upload({ file }) {
  const form = new FormData()
  form.append('file', file)
  vdata.uploading = true
  request
    .request({ url: '/api/ossFiles/avatar', method: 'POST', data: form, headers: { 'Content-Type': 'multipart/form-data' } })
    .then((url) => (vdata.logo = url))
    .catch(() => $infoBox.message.error('上傳失敗'))
    .finally(() => (vdata.uploading = false))
}
function save() {
  vdata.saving = true
  req.updateById(API_URL_AGENT_PORTAL + '/branch', 'brand', { brandEnabled: vdata.enabled ? 1 : 0, brandTitle: vdata.title, brandLogo: vdata.logo })
    .then(() => {
      $infoBox.message.success('已儲存，重新整理頁面後生效')
      emit('saved')
    })
    .finally(() => (vdata.saving = false))
}
</script>
