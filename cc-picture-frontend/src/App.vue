<template>
  <div id="app">
    <BasicLayout />
  </div>
</template>

<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
import {useLoginUserStore} from "@/stores/useLoginUserStore.ts";
// 初始化数据(发请求拉数据)
// 首次进入到页面时，一般会尝试获取登录用户信息
const loginUserStore = useLoginUserStore()
loginUserStore.fetchLoginUser()

</script>

<style>
#app {
}
</style>
