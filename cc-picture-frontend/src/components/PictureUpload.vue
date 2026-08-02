<template>
  <div id="pictureUpload" class="picture-upload" >
      <a-upload
        list-type="picture-card"
        :show-upload-list="false"
        :customRequest="handleUpload"
        :before-upload="beforeUpload"
      >
        <img v-if="picture?.url" :src="picture?.url" alt="avatar" />
        <div v-else>
          <loading-outlined v-if="loading"></loading-outlined>
          <plus-outlined v-else></plus-outlined>
          <div class="ant-upload-text">点击或拖拽上传图片</div>
        </div>
      </a-upload>
  </div>
</template>
<script lang="ts" setup>
import { ref } from 'vue';
import { PlusOutlined, LoadingOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { uploadPictureUsingPost } from '@/api/pictureController';

/**
 * 受控组件
 * 由⁠父⁠组⁠件 (图片创建页面AddPicturePage) 来管理
 */
interface Props {
  picture?: API.PictureVO
  spaceId: number
  onSuccess?: (newPicture: API.PictureVO) => void
}

const props = defineProps<Props>()

/**
 * 上传前校验
 */
const beforeUpload = (file: File) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png';
  if (!isJpgOrPng) {
    message.error('不支持上传该格式的图片，推荐 jpg 或 png');
  }
  const isLt2M = file.size / 1024 / 1024 < 2;
  if (!isLt2M) {
    message.error('不能上传超过 2M 的图片');
  }
  return isJpgOrPng && isLt2M;
};

const loading = ref<boolean>(false)

/**
 * 上传
 */
const handleUpload = async ({file}: any) => {
  loading.value = true
  try {
    // 调用后端⁡⁡⁡上传图片接口时，如果已经有⁠⁠⁠ pictureId
    // 表示对已上传的图片进行更新，需​​​要将该参数也添加到请求中，⁠⁠⁠否则每次都会新增图片记录
    const params = props.picture ? {id: props.picture.id} : {};
    params.spaceId = props.spaceId // 添加空间ID到请求中 实现私有空间图片上传功能
    const res = await uploadPictureUsingPost(params, {}, file)
    if (res.data.code === 0 && res.data.data){
      message.success('图片上传成功')
      // 将上传成功的图片信息传递给父组件
      props.onSuccess?.(res.data.data)
    } else {
      message.error('图片上传失败，' + res.data.message)
    }
  } catch (error: any) {
    message.error('图片上传失败：' + (error?.message ?? ''))
  } finally {
    loading.value = false
  }
}

</script>
<style scoped>
/* :deep 语法来修改 Upload 组件内置的样式
  让组件​的宽高​等⁠同于父组⁠件的宽高
*/
.picture-upload :deep(.ant-upload) {
  width: 100% !important;
  height: 100% !important;
  min-width: 152px;
  min-height: 152px;
}

.picture-upload img {
  max-width: 100%;
  max-height: 480px;
}

.ant-upload-select-picture-card i {
  font-size: 32px;
  color: #999;
}

.ant-upload-select-picture-card .ant-upload-text {
  margin-top: 8px;
  color: #666;
}
</style>
