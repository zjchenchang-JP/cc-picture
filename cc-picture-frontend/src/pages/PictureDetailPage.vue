<template>
  <div id="pictureDetailPage">
    <a-row :gutter="[16, 16]">
      <!-- 图片展示区 -->
      <a-col :sm="24" :md="16" :xl="18">
        <a-card title="图片预览">
          <a-image
            style="max-height: 600px; object-fit: contain"
            :src="picture.url"
          />
        </a-card>
      </a-col>
      <!-- 图片信息区 -->
      <a-col :sm="24" :md="8" :xl="6">
        <a-card title="图片信息">
          <a-descriptions :column="1">
            <a-descriptions-item label="作者">
              <a-space>
                <a-avatar :size="24" :src="picture.user?.userAvatar" />
                <div>{{ picture.user?.userName }}</div>
              </a-space>
            </a-descriptions-item>
            <a-descriptions-item label="名称">
              {{ picture.name ?? '未命名' }}
            </a-descriptions-item>
            <a-descriptions-item label="简介">
              {{ picture.introduction ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="分类">
              {{ picture.category ?? '默认' }}
            </a-descriptions-item>
            <a-descriptions-item label="标签">
              <a-tag v-for="tag in picture.tags" :key="tag">
                {{ tag }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="格式">
              {{ picture.picFormat ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="宽度">
              {{ picture.picWidth ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="高度">
              {{ picture.picHeight ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="宽高比">
              {{ picture.picScale ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="大小">
              {{ formatSize(picture.picSize) }}
            </a-descriptions-item>
          </a-descriptions>
          <!-- 操作⁠按⁠钮⁠，对于图片上传者或管理员​，可以​编辑和​删⁠除图片 -->
          <a-space wrap>
            <a-button v-if="canEdit" type="default" @click="doEdit">
              编辑
              <template #icon>
                <EditOutlined />
              </template>
            </a-button>
            <a-button v-if="canEdit" danger @click="doDelete">
              删除
              <template #icon>
                <DeleteOutlined />
              </template>
            </a-button>
            <a-button type="primary" @click="doDownload">
              免费下载
              <template #icon>
                <DownloadOutlined />
              </template>
            </a-button>
          </a-space>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import {
  deletePictureUsingPost,
  getPictureVoByIdUsingGet
} from '@/api/pictureController'
import { message, Modal } from 'ant-design-vue'
import { ExclamationCircleOutlined } from '@ant-design/icons-vue'
import { computed, h, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { downloadImage, formatSize } from '@/utils'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

// 图片详情⁡⁡⁡页要展示的图片是根据 ⁠⁠⁠id 而变化的，使用动态路由。
// 在页面​​​中可以使用 props⁠⁠⁠ 获取到动态的参数
const props = defineProps<{
  // TS 的联合类型(Union Type),|
  // 表示 id 可以是 string 或 number 任一种
  id: string | number
}>()

const picture = ref<API.PictureVO>({})

// 获取图片详情
const fetchPictureDetail = async () => {
  try {
    const res = await getPictureVoByIdUsingGet({
      // 假转换:只骗编译器
      id: props.id as unknown as number
    })
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
    } else {
      message.error('获取图片详情失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取图片详情失败：' + e.message)
  }
}

/**
 * 操作模块 编辑或删除图片
 */
// 权限判⁡⁡⁡断逻辑, can⁠E⁠d⁠it 的值为 true ​表示有​编辑和​删⁠除权限
const loginUserStore = useLoginUserStore()
// 是否具有编辑权限
const canEdit = computed(() => {
  const loginUser = loginUserStore.loginUser
  // 未登录
  if (!loginUser) {
    return false
  }
  // 仅本人或管理员可编辑
  const user = picture.value.user ?? {}
  return loginUser.id === user.id || loginUser.userRole === 'admin'
})

const router = useRouter()

// 编辑 跳转
const doEdit = () => {
  router.push('/add_picture?id=' + picture.value.id)
}
// 删除
// TODO 如果后面添加 <keep-alive> 缓存
// (比如为了保留首页滚动位置),router.back()
//  回去的来源页就不会重新拉数据,列表里还会残留已删的图
const doDelete = () => {
  const id = picture.value.id
  if (!id) {
    return
  }
  Modal.confirm({
    title: '确认删除这张图片吗？',
    icon: h(ExclamationCircleOutlined),
    content: '删除后不可恢复',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        const res = await deletePictureUsingPost({ id })
        if (res.data.code === 0) {
          message.success('删除成功')
          // 回来源页;没有历史(比如直接粘贴 URL 进来)就回首页兜底
          if (window.history.length > 1) {
            router.back()
          } else {
            router.push('/')
          }
        } else {
          message.error('删除失败，' + res.data.message)
        }
      } catch (e: any) {
        message.error('删除失败：' + (e?.message ?? ''))
      }
    }
  })
}

// 图片下载
const doDownload = () => {
  downloadImage(picture.value.url, picture.value.name)
}

onMounted(() => {
  fetchPictureDetail()
})
</script>

<style scoped>
#pictureDetailPage {
}
</style>
