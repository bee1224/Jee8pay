<template>
  <div class="result-err">
    <img src="~@/assets/svg/500.svg" alt="">
    <div>
      對不起，伺服器錯誤。
    </div>
    <a-button type="primary" @click="toHome" style="margin-top:30px">
      返回首頁
    </a-button>
  </div>
</template>

<script>
export default {
  name: 'Exception500',
  methods: {
    toHome () {
      this.$router.push({ path: '/' })
    }
  }
}
</script>
