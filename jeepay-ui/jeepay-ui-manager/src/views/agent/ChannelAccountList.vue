<template>
  <!-- 渠道管理（上帝）：全部團長的渠道帳號 -->
  <page-header-wrapper>
    <a-card :bordered="false">
      <ChannelAccountPanel />
    </a-card>
  </page-header-wrapper>
</template>

<script setup lang="ts">
import ChannelAccountPanel from './ChannelAccountPanel.vue'
</script>
