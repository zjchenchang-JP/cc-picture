<!-- 我的空间页面是一个 “中间页”，作用是根据用户是否已有空间，重定向 到对应的页面 -->
<template>
  <div id="mySpace">
    <p>正在跳转，请稍候...</p>
  </div>
</template>
<script lang="ts" setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 检查用户是否有个人空间
const checkUserSpace = async () => {
  const loginUser = loginUserStore.loginUser
  if (!loginUser?.id) {
    // 未登录 跳转到登录页面
    router.replace('/user/login')
    return
  }
  // 获取用户空间信息
  const res = await listSpaceVoByPageUsingPost({
    userId: loginUser.id,
    current: 1,
    pageSize: 1,
  })
  if (res.data.code === 0) {
    // 如果用户已登录，会获取该用户已创建的空间
    // 如果有，则进入第一个空间
    // .replace 重定向页面，这样点击浏览器的后退按钮时，不会回到中间页
    const records = res.data.data?.records ?? []
    if (records.length > 0) {
      const space = records[0]
      router.replace(`/space/${space?.id}`)
    } else {
      // 如果没有，则跳转到创建空间页面
      router.replace('/add_space')
      message.warn('请先创建空间')
    }
  } else {
    message.error('加载我的空间失败，' + res.data.message)
  }
}

// 在页面加载时检查用户空间
onMounted(() => {
  checkUserSpace()
})

</script>