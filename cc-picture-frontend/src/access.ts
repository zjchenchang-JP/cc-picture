// 全局权限控制文件。可以利用 Vue Router 的路由守卫实现，
// 每次切换并进入页面前，都会检查一下当前用户是否具有特定页面的权限
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { message } from 'ant-design-vue'
import router from '@/router'

// 是否为首次获取登录用户
let firstFetchLoginUser = true

/**
 * 全局权限校验
 */
router.beforeEach(async (to,from,next) => {
  const loginUserStore = useLoginUserStore()
  let loginUser = loginUserStore.loginUser
  // 确保页面刷新，首次加载时，能够等后端返回用户信息后再校验权限
  // 为了防止每次切换路由都从远程获取用户​​​信息，定义了 firstFetchLoginUser ⁠⁠⁠变量
  // 用于控制在刷新页面后只会请求后端一次
  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()
    loginUser = loginUserStore.loginUser
    firstFetchLoginUser = false
  }
  // 不是首次
  const toUrl = to.fullPath
  // 管理员权限
  if (toUrl.startsWith('/admin')) {
    if (!loginUser || loginUser.userRole !== 'admin') {
      message.error('没有权限')
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  }
  next()
})
