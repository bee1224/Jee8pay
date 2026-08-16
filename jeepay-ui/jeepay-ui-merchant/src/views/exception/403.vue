<template>
  <div class="result-err">
    <img src="~@/assets/svg/403.svg" alt="">
    <div>
      抱歉，您無權訪問此頁。
    </div>
    <a-button type="primary" @click="toHome" style="margin-top:30px">
      返回首頁
    </a-button>
  </div>
</template>

<script>
export default {
  name: 'Exception403',
  methods: {
    toHome () {
      this.$router.push({ path: '/' })
    }
  }
}
</script>
