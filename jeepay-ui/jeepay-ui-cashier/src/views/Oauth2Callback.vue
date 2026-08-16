<template>
  <div>
    <p style="font-size:16px;">正在跳轉...</p>
  </div>
</template>

<script>
import { getChannelUserId } from '@/api/api'
import wayCodeUtils from '@/utils/wayCode'
import channelUserIdUtil from '@/utils/channelUserId'
import config from '@/config'

export default {
  mounted() {
    // Vue Router 4 的 this.$route.query 已包含 URL 查询参数，无需手动解析
    const allQuery = { ...this.$route.query }

    const that = this
    getChannelUserId(allQuery).then(res => {
      channelUserIdUtil.setChannelUserId(res)

      const payWay = wayCodeUtils.getPayWay()
      if (payWay && payWay.routeName) {
        that.$router.push({ name: payWay.routeName })
      } else {
        that.$router.push({ name: config.errorPageRouteName, params: { errInfo: '無法識別支付方式' } })
      }
    }).catch(res => {
      that.$router.push({ name: config.errorPageRouteName, params: { errInfo: (res && res.msg) || '獲取用戶資訊失敗' } })
    })
  }
}
</script>
