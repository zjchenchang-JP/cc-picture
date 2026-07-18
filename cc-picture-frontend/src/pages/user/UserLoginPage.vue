<template>
  <div id="userLoginPage">
    <h2 class="title">CC云图库 - 用户登录</h2>
    <div class="desc">企业级智能协同云图库</div>
    <a-form
    :model="formState"
    name="basic"
    autocomplete="off"
    @finish="handleSubmit" >
      <a-form-item
        name="userAccount"
        :rules="[{ required: true, message: '请输入账号' }]" >
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
      </a-form-item>
      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 6, message: '密码不能小于 6 位' },
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入账号" />
      </a-form-item>
      <div class="tips">
        没有账号？
        <RouterLink to="/user/register">去注册</RouterLink>
      </div>
      <a-form-item >
        <a-button type="primary" html-type="submit" style="width: 100%" >登录</a-button>
      </a-form-item>
    </a-form>
  </div>
  
</template>
<script lang="ts" setup>
import { reactive } from 'vue';
// import router from '@/router' // 用于接受表单输入的值
import { useLoginUserStore } from '@/stores/useLoginUserStore';
import { useRouter } from 'vue-router';
import { userLoginUsingPost } from '@/api/userController';
import { message } from 'ant-design-vue';

// 接收表单输出值
const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: ''
})

const loginUserStore = useLoginUserStore()
const router = useRouter()

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (value: any) => {
  const res = await userLoginUsingPost(value)
  // 登录成功 保存登录态到全局状态Pinia中
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('登录成功')
    router.push({
      path: '/',
      replace: true
    })
  } else {
    message.error('登录失败，' + res.data.message)
  }
}



</script>

<style scoped>
#userLoginPage {
  max-width: 360px;
  margin: 0 auto;
}

.title {
  text-align: center;
  margin-bottom: 16px;
}

.desc {
  text-align: center;
  color: #bbb;
  margin-bottom: 16px;
}

.tips {
  margin-bottom: 16px;
  color: #bbb;
  font-size: 13px;
  text-align: right;
}
</style>