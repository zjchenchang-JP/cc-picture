<!-- AI 扩图 一行⁠⁠⁠⁠两列栅格布局，左边显示原始图片、右边​​​​显示扩图结果，下方⁠⁠⁠⁠展示扩图操作按钮-->
<template>
  <a-modal
    class="image-out-painting"
    v-model:visible="visible"
    title="AI 扩图"
    :footer="false"
    @cancel="closeModal"
  >
    <a-row gutter="16">
      <a-col span="12">
        <h4>原始图片</h4>
        <img :src="picture?.url" :alt="picture?.name" style="max-width: 100%" />
      </a-col>
      <a-col span="12">
        <h4>扩图结果</h4>
        <img
          v-if="resultImageUrl"
          :src="resultImageUrl"
          :alt="picture?.name"
          style="max-width: 100%"
        />
      </a-col>
    </a-row>
    <div style="margin-bottom: 16px" />
    
    <a-flex gap="16" justify="center">
      <!-- 有任务 id 时，禁止按钮点击，可以防止重复提​​​​交任务
          !! 的作用就是"把任意值规范化成布尔值"
          v-if 自己会判断真假,不用你帮它转;:loading 是给组件传值,你要负责传对类型
      -->
      <a-button type="primary" :loading="!!taskId" ghost @click="createTask">生成图片</a-button>
      <!-- 有图片结果时才显示 “应用结果” 按钮 -->
      <a-button type="primary" v-if="resultImageUrl" :loading="uploadLoading" @click="handleUpload">应用结果</a-button>
    </a-flex>
  </a-modal>
</template>

<script setup lang="ts">
import { onUnmounted, ref } from 'vue'
import { createPictureOutPaintingTaskUsingPost, getPictureOutPaintingTaskUsingGet, uploadPictureByUrlUsingPost } from '@/api/pictureController'
import { message } from 'ant-design-vue'
import { EditOutlined } from '@ant-design/icons-vue'

interface Props {
  picture?: API.PictureVO
  spaceId?: number
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()

// 是否可见
const visible = ref(false)

// 打开弹窗
const openModal = () => {
  visible.value = true
}

// 关闭弹窗
const closeModal = () => {
  visible.value = false
}

// 暴露函数给父组件
defineExpose({
  openModal,
})


/**
 * ----- AI扩图相关 -----
 */
const resultImageUrl = ref<string>()
let taskId = ref<string>()

// 创建扩图任务
const createTask = async () => {
  if (!props.picture?.id) return
  const res = await createPictureOutPaintingTaskUsingPost({
    pictureId: props.picture.id,
    // 可以根据需要设置扩图参数
    parameters: {
      xScale: 2,
      yScale: 2,
    },
  })
  if (res.data.code === 0 && res.data.data) {
    message.success('创建任务成功，请耐心等待，不要退出界面')
    console.log(res.data.data.output?.taskId)
    taskId.value = res.data.data.output?.taskId
    // 开启轮询 阿里云百炼文档
    startPolling()
  } else {
    message.error(`创建任务失败，${res.data.message}`)
  }
}

// 轮询扩图结果
/**
 * 无论⁠⁠⁠任⁠务执行成功或失败、还是退出当前​​​页面时​，都需要执⁠⁠⁠行清理逻⁠辑，包括：
    清理定时器
    将定时器变量设置为 null
    将任务 id 设置为 null，这样允许前端多次执行任务
 */

// 轮询定时器：用 setTimeout 递归，保证“上一次请求返回后”再排下一次，
// 避免 setInterval 那种“不管上次完没完成都发下一次”的并发问题。
let pollingTimer: NodeJS.Timeout | null = null
// 连续失败计数：网络抖动等偶发错误不该直接中断轮询，连续失败超过阈值才停。
let pollingFailCount = 0
const MAX_POLLING_FAIL = 3

// 彻底停止轮询并清理状态（清定时器、清 taskId、清失败计数）。
// 把 taskId 置空后，“生成图片”按钮才会解除 loading，允许再次发起任务。
const clearPolling = () => {
  if (pollingTimer) {
    clearTimeout(pollingTimer)
    pollingTimer = null
  }
  taskId.value = undefined
  pollingFailCount = 0
}

// 单次轮询
const pollOnce = async () => {
  // 组件卸载或任务被清理后就不再继续，防止“已停止”还排了下一次
  if (!taskId.value) return
  let shouldStop = false
  try {
    const res = await getPictureOutPaintingTaskUsingGet({
      taskId: taskId.value,
    })
    if (res.data.code === 0 && res.data.data) {
      const taskResult = res.data.data.output
      if (taskResult?.taskStatus === 'SUCCEEDED') {
        message.success('扩图任务执行成功')
        resultImageUrl.value = taskResult.outputImageUrl
        shouldStop = true
      } else if (taskResult?.taskStatus === 'FAILED') {
        message.error('扩图任务失败')
        shouldStop = true
      } else {
        // 其它状态（如 RUNNING）：任务还在跑，重置失败计数，继续轮询
        pollingFailCount = 0
      }
    }
  } catch (error) {
    // 偶发的网络错误 / 后端超时，先别放弃，累计失败次数再决定是否停止
    console.error('轮询任务状态失败', error)
    pollingFailCount += 1
    if (pollingFailCount >= MAX_POLLING_FAIL) {
      message.error('多次检测任务状态失败，请稍后重试')
      shouldStop = true
    }
  }
  if (shouldStop) {
    clearPolling()
    return
  }
  // 还没拿到终态，3 秒后再查一次
  pollingTimer = setTimeout(pollOnce, 3000)
}

// 开启轮询
const startPolling = () => {
  if (!taskId.value) return
  // 防御：若上一次轮询的定时器还在，先清掉旧的（注意这里不能清空 taskId）
  if (pollingTimer) {
    clearTimeout(pollingTimer)
    pollingTimer = null
  }
  pollingFailCount = 0
  pollingTimer = setTimeout(pollOnce, 3000)
}


// 上传AI扩图结果
// 当任务执行成功⁡⁡⁡⁡后，可以得到图片结果，此时就可以点⁠⁠⁠⁠击 “应用结果” 按钮，调用图片 URL 上传接口
const uploadLoading = ref<boolean>(false)

const handleUpload = async () => {
  uploadLoading.value = true
  try {
    const params: API.PictureUploadRequest = {
      fileUrl: resultImageUrl.value,
      spaceId: props.spaceId,
    }
    if (props.picture) {
      params.id = props.picture.id
    }
    const res = await uploadPictureByUrlUsingPost(params)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      // 将上传成功的图片信息传递给父组件
      props.onSuccess?.(res.data.data)
      // 关闭弹窗
      closeModal()
    } else {
      message.error('图片上传失败，' + res.data.message)
    }
  } catch (error) {
    message.error('图片上传失败')
  } finally {
    uploadLoading.value = false
  }
}


// 清理定时器，避免内存泄漏
onUnmounted(() => {
  clearPolling()
})
</script>

<style scoped>
.image-out-painting {
  text-align: center;
}
</style>