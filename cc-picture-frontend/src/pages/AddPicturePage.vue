<template>
  <div id="addPicturePage">
    <h2 style="margin-bottom: 16px">
      {{ route.query?.id ? '修改图片' : '创建图片' }}
    </h2>
    <a-typography-paragraph v-if="spaceId" type="secondary">
      保存至空间：<a :href="`/space/${spaceId}`" target="_blank">{{spaceId}}</a>
    </a-typography-paragraph>
    <!-- 选择上传方式 -->
    <a-tabs v-model:activeKey="uploadType"
      >>
      <a-tab-pane key="file" tab="文件上传">
        <PictureUpload
          :picture="picture"
          :onSuccess="onSuccess"
          :spaceId="spaceId"
        />
      </a-tab-pane>
      <a-tab-pane key="url" tab="URL 上传" force-render>
        <UrlPictureUpload
          :picture="picture"
          :onSuccess="onSuccess"
          :spaceId="spaceId"
        />
      </a-tab-pane>
    </a-tabs>
    <!-- 图片信息表单 -->
    <a-form
      v-if="picture"
      layout="vertical"
      :model="pictureForm"
      @finish="handleSubmit"
    >
      <a-form-item label="名称" name="name">
        <a-input v-model:value="pictureForm.name" placeholder="请输入名称" />
      </a-form-item>
      <a-form-item label="简介" name="introduction">
        <a-textarea
          v-model:value="pictureForm.introduction"
          placeholder="请输入简介"
          :rows="2"
          autoSize
          allowClear
        />
      </a-form-item>
      <a-form-item label="分类" name="category">
        <a-auto-complete
          v-model:value="pictureForm.category"
          :options="categoryOptions"
          placeholder="请输入分类"
          allowClear
        />
      </a-form-item>
      <a-form-item label="标签" name="tags">
        <a-select
          v-model:value="pictureForm.tags"
          mode="tags"
          :options="tagOptions"
          placeholder="请输入标签"
          allowClear
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%"
          >创建</a-button
        >
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import {
  editPictureUsingPost,
  getPictureVoByIdUsingGet,
  listPictureTagCategoryUsingGet
} from '@/api/pictureController'
import PictureUpload from '@/components/PictureUpload.vue'
import UrlPictureUpload from '@/components/UrlPictureUpload.vue'
import { message } from 'ant-design-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()

const picture = ref<API.PictureVO>()
const pictureForm = reactive<API.PictureEditRequest>({})

const uploadType = ref<'file' | 'url'>('file')
// 从URL获取空间id
const spaceId = computed(() => {
  return route.query?.spaceId
})

/**
 * 图片上传成功
 * @param newPicture
 */
const onSuccess = (newPicture: API.PictureVO) => {
  picture.value = newPicture
  // 上传图⁡⁡⁡片后，可以将得到⁠的⁠图片信⁠息（比如名称）回显 填充到表单
  pictureForm.name = newPicture.name
}

/**
 * 提交表单
 * 需要带着上传图片得到的 pictureId 来调用后端 修改图片接口（而不是上传接口）
 */
const handleSubmit = async (values: any) => {
  const pictureId = picture.value?.id
  if (!pictureId) return
  const res = await editPictureUsingPost({
    id: pictureId,
    ...values
  })
  if (res.data.code === 0 && res.data.data) {
    message.success('图片创建成功')
    // 跳转到图片详情页
    router.push({
      path: `/picture/${pictureId}`
    })
  } else {
    message.error('创建失败，' + res.data.message)
  }
}

// 分类⁡⁡⁡和标签选择框补⁠充⁠选⁠项数据
const categoryOptions = ref<{ value: string; label: string }[]>([])
const tagOptions = ref<{ value: string; label: string }[]>([])

/**
 * 获取标签和分类选项
 */
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    // 转换成下拉选项组件接受的格式
    tagOptions.value = (res.data.data.tagList ?? []).map((data: string) => {
      return {
        value: data,
        label: data
      }
    })
    categoryOptions.value = (res.data.data.categoryList ?? []).map(
      (data: string) => {
        return {
          value: data,
          label: data
        }
      }
    )
  } else {
    message.error('加载选项失败，' + res.data.message)
  }
}
// 「页面挂载时初始化」
onMounted(() => {
  getTagCategoryOptions()
})

const route = useRoute()
/**
 * 修改图片信息
 */
// 获取要修改的图片信息
const getOldPicture = async () => {
  // 获取到 id（query 参数本身就是字符串）
  // ⚠️ 不能用 Number()：雪花 id 超过 JS 安全整数(2^53)，会丢精度
  // （2080910661127073794 → 2080910661127073800），后端就查不到了。
  // 后端已配置 Long→String 序列化(JsonConfig)，id 全程保持字符串即可。
  const id = route.query?.id
  if (id) {
    const res = await getPictureVoByIdUsingGet({
      // 运行时 id 是 string，但 openapi 生成的类型是 number，用断言绕过
      id: id as unknown as number
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      picture.value = data
      pictureForm.name = data.name
      pictureForm.introduction = data.introduction
      pictureForm.category = data.category
      pictureForm.tags = data.tags
    }
  }
}
/**
 * 多个 onMounted 的回调互相独立,一个里的 await 管不到另一个。
 * 但如果 B 依赖 A 的结果,必须放进同一个 onMounted 用 await 串起来
 */
onMounted(() => {
  getOldPicture()
})
</script>

<style scoped>
/* 限制下页面最大宽度，让用户视角更集中 */
#addPicturePage {
  max-width: 720px;
  margin: 0 auto;
}
</style>
