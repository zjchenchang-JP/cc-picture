// 批量上传图片
<template>
  <div id="addSpacePage">
    <h2 style="margin-bottom: 16px">
      {{ route.query?.id ? '修改' : '创建' }} {{ SPACE_TYPE_MAP[spaceType] }}
    </h2>
    <a-form layout="vertical" :model="formData" @finish="handleSubmit">
      <a-form-item label="空间名称" name="spaceName">
        <a-input
          v-model:value="formData.spaceName"
          placeholder="请输空间名称"
          allow-clear
        />
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
        <a-button
          type="primary"
          html-type="submit"
          style="width: 100%"
          :loading="loading"
        >
          提交
        </a-button>
      </a-form-item>
    </a-form>
    <a-card title="空间级别介绍">
      <a-typography-paragraph>
        * 目前仅支持开通普通版，如需升级空间，请联系
        <a href="https://github.com/zjchenchang-JP" target="_blank">CC_JP</a>。
      </a-typography-paragraph>
      <a-typography-paragraph v-for="spaceLevel in spaceLevelList">
        {{ spaceLevel.text }}： 大小 {{ formatSize(spaceLevel.maxSize) }}， 数量
        {{ spaceLevel.maxCount }}
      </a-typography-paragraph>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { SPACE_LEVEL_ENUM, SPACE_LEVEL_OPTIONS, SPACE_TYPE_ENUM, SPACE_TYPE_MAP } from '@/constants/space'
import { addSpaceUsingPost, getSpaceVoByIdUsingGet, listSpaceLevelUsingGet, updateSpaceUsingPost } from '@/api/spaceController'
import { formatSize } from '@/utils'

const formData = reactive<API.SpaceAddRequest | API.SpaceUpdateRequest>({
  spaceName: '',
  spaceLevel: SPACE_LEVEL_ENUM.COMMON
})
// 提交任务状态
const loading = ref(false)

const router = useRouter()

/**
 * 提交表单
 */
const handleSubmit = async (values: any) => {
  // 据是⁠否⁠有⁠ spaceId 决定是​调用更​新接口​还⁠是创建接⁠口
  const spaceId = oldSpace.value?.id
  loading.value = true
  let res
  // 更新
  if (spaceId) {
    res = await updateSpaceUsingPost({
      id: spaceId,
      ...formData
    })
  } else {
    // 创建
    res = await addSpaceUsingPost({
      ...formData,
      spaceType: spaceType.value // 空间类型
    })
  }
  // 操作成功
  if (res.data.code === 0 && res.data.data) {
    message.success('操作成功')
    // 跳转到空间信息详情展示
    router.push({
      path: `/space/${spaceId ?? res.data.data}`
    })
  } else {
    message.error('创建失败，' + res.data.message)
  }
  loading.value = false
}


/**
 * 空间级别展示
 */
const spaceLevelList = ref<API.SpaceLevel[]>([])
// 获取空间级别
const fetchSpaceLevelList = async () => {
  const res = await listSpaceLevelUsingGet()
  if (res.data.code === 0 && res.data.data) {
    spaceLevelList.value = res.data.data
  } else {
    message.error('加载空间级别失败，' + res.data.message)
  }
}

onMounted(()=> {
  fetchSpaceLevelList()
})

/**
 * 更新空间
 */
const route = useRoute()
const oldSpace = ref<API.SpaceVO>()

// 获取老数据
const getOldSpace = async () => {
  // 获取数据
  const id = route.query?.id
  if (id) {
    const res = await getSpaceVoByIdUsingGet({
      id: id as unknown as number
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      oldSpace.value = data
      formData.spaceName = data.spaceName
      formData.spaceLevel = data.spaceLevel
    }
  }
}

// 页面加载时，请求老数据
onMounted(() => {
  getOldSpace()
})

// --- 团队空间 复用本页面 ---
// 空间类别，默认为私有空间
const spaceType = computed(() => {
  if (route.query?.type) {
    // 当我们访问 http://localhost:5173/add_space?type=1 的时候就创建团队空间
    return Number(route.query.type)
  }
  // 编辑场景：URL 没带 type，以接口返回的真实类型为准
  if (oldSpace.value?.spaceType != null) {
    return oldSpace.value.spaceType
  }
  return SPACE_TYPE_ENUM.PRIVATE
})

</script>

<style scoped>
#addSpacePage {
  max-width: 720px;
  margin: 0 auto;
}
</style>
