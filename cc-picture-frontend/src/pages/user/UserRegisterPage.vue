<template>
  <div id="userRegisterPage">
    <h2 class="title">CC云图库 - 用户登录</h2>
    <div class="desc">企业级智能协同云图库</div>
    <a-form
    :model="formState"
    name="basic"
    label-align="left"
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
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
      </a-form-item>
      <a-form-item
        name="checkPassword"
        :rules="[
          { required: true, message: '请输入确认密码' },
          { min: 6, message: '确认密码不能小于 6 位' },
        ]"
      >
        <a-input-password v-model:value="formState.checkPassword" placeholder="请输入确认密码" />
      </a-form-item>
      <div class="tips">
        已有账号？
        <RouterLink to="/user/login">去登录</RouterLink>
      </div>
      <a-form-item >
        <a-button type="primary" html-type="submit" style="width: 100%" >注册</a-button>
      </a-form-item>
    </a-form>
  </div>
  
</template>
<script lang="ts" setup>
import { reactive } from 'vue';
import { useLoginUserStore } from '@/stores/useLoginUserStore';
import { useRouter } from 'vue-router';
import { userRegisterUsingPost } from '@/api/userController';
import { message } from 'ant-design-vue';

// 接收表单输出值
const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: ''
})

const loginUserStore = useLoginUserStore()
const router = useRouter()

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (value: any) => {
  // 判断两次输入的密码是否一致
  if (formState.userPassword !== formState.checkPassword){
    message.error('二次输入的密码不一致')
    return
  }
  const res = await userRegisterUsingPost(value)
  // 登录成功 保存登录态到全局状态Pinia中
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('注册成功')
    router.push({
      path: '/user/login',
      replace: true
    })
  } else {
    message.error('注册失败，' + res.data.message)
  }
}



</script>

<style scoped>
#userRegisterPage {
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