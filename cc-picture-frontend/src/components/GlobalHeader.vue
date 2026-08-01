<template>
  <div id="globalHeader">
    <a-row :wrap="false">
      <a-col flex="200px">
        <!-- Rou⁡⁡terLink 组件的作⁠⁠⁠用是支持超链接跳转（不刷新页面） -->
        <router-link to="/">
          <div class="title-bar">
            <img class="logo" src="../assets/logo.png" alt="logo" />
            <div class="title">CC云图库</div>
          </div>
        </router-link>
      </a-col>
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="current"
          mode="horizontal"
          @click="onMenuClick"
          :items="items"
        />
      </a-col>
      <a-col flex="120px">
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a-space>
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>
<script lang="ts" setup>
import { computed, h, ref } from 'vue'
import { HomeOutlined } from '@ant-design/icons-vue'
import { message, type MenuProps } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { userLogoutUsingPost } from '@/api/userController'

// 引入全局pinia
const loginUserStore = useLoginUserStore()
// 用户注销
const doLogout = async () => {
  const res = await userLogoutUsingPost()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录'
    })
    message.success('退出登录成功')
    // 等价于 router.push({ path: '/user/login', replace: true })
    await router.replace('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}

const originItems = ref<MenuProps['items']>([
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页'
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理'
  },
  {
    key: '/add_picture',
    label: '创建图片',
    title: '创建图片'
  },
  {
    key: '/admin/pictureManage',
    label: '图片管理',
    title: '图片管理'
  },
  {
    key: '/admin/spaceManage',
    label: '空间管理',
    title: '空间管理',
  },
  {
    key: 'others',
    label: h(
      'a',
      { href: 'https://github.com/zjchenchang-JP', target: '_blank' },
      'Author Github'
    ),
    title: 'Author Github'
  }
])
// 权限控制 未登录 or 非admin不应该看见 ‘用户管理’ 菜单
// 过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter(menu => {
    if (menu.key?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

// 展示在菜单的路由数组（注意 originItems 是 ref，要传 .value 解包后的真实数组）
const items = computed<MenuProps['items']>(() => filterMenus(originItems.value))

const router = useRouter()
// 当前选中菜单
const current = ref<string[]>([])
// 路由跳转事件
const onMenuClick = ({ key }) => {
  router.push({
    path: key
  })
}
// 监听路由变化，高亮当前选中菜单
router.afterEach((to, from, next) => {
  current.value = [to.path]
})
</script>

<style scoped>
.title-bar {
  display: flex;
  align-items: center;
}

.title {
  color: black;
  font-size: 18px;
  margin-left: 16px;
}

.logo {
  height: 48px;
}
</style>
