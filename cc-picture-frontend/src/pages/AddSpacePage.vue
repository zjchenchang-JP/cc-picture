// 批量上传图片
<template>
  <div id="addSpacePage">
    <h2 style="margin-bottom: 16px">批量创建图片</h2>
    <a-form layout="vertical" :model="formData" @finish="handleSubmit">
      <a-form-item label="空间名称" name="spaceName">
        <a-input v-model:value="formData.spaceName" placeholder="请输空间名称" allow-clear/>
      </a-form-item>
      <a-form-item label="空间级别" name="spaceLevel">
        <a-select
          v-model:value="formData.spaceLevel"
          :options="SPACE_LEVEL_OPTIONS"
          placeholder="请输入空间级别"
          style="min-width: 180px"
          allow-clear
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%" :loading="loading">
          提交
        </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { SPACE_LEVEL_ENUM, SPACE_LEVEL_OPTIONS } from '@/constants/space'
import { addSpaceUsingPost } from '@/api/spaceController'

const formData = reactive<API.SpaceAddRequest | API.SpaceUpdateRequest>({
  spaceName: '',
  spaceLevel: SPACE_LEVEL_ENUM.COMMON,
})
// 提交任务状态
const loading = ref(false)

const router = useRouter()

/**
 * 提交表单
 */
const handleSubmit = async (values: any) => {
  loading.value = true
  const res = await addSpaceUsingPost({
    ...formData,
  })
  // 操作成功
  if (res.data.code === 0 && res.data.data) {
    message.success('创建空间成功')
    // 跳转到空间信息详情
    router.push({
      path: `/space/${res.data.data}`,
    })
  } else {
    message.error('创建失败，' + res.data.message)
  }
  loading.value = false
}

</script>

<style scoped>
#addSpacePage {
  max-width: 720px;
  margin: 0 auto;
}
</style>

