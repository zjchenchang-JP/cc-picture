<!-- 封装图片⁡⁡⁡列表组件 Pictu⁠⁠⁠reList 该组件只负责数据的展示、不​​​负责数据的查询，因此⁠⁠⁠要把分页组件单独拉出来 -->
<template>
  <div class="picture-list">
    <!-- 图片列表 -->
    <a-list
      :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 4, xl: 5, xxl: 6 }"
      :data-source="dataList"
      :loading="loading"
    >
      <template #renderItem="{ item: picture }">
        <a-list-item style="padding: 0">
          <!-- 单张图片 性能优化 显示缩略图 懒加载
              :src="picture.thumbnailUrl ?? picture.url"
              loading="lazy" 
          -->
          <a-card hoverable @click="doClickPicture(picture)">
            <template #cover>
              <img
                style="height: 180px; object-fit: cover"
                :alt="picture.name"
                :src="picture.url"
                loading="lazy"
              />
            </template>
            <a-card-meta :title="picture.name">
              <template #description>
                <a-flex>
                  <a-tag color="green">
                    {{ picture.category ?? '默认' }}
                  </a-tag>
                  <a-tag v-for="tag in picture.tags" :key="tag">
                    {{ tag }}
                  </a-tag>
                </a-flex>
              </template>
            </a-card-meta>
            <!-- 弹窗分享组件 -->
            <ShareModal ref="shareModalRef" :link="shareLink" />
            <!-- 空间详情页的图片卡片的下方增加快捷操作栏 -->
            <!-- 本⁡⁡⁡组件是主页和空间详情⁠⁠⁠页公用的，主页不需要展示操作栏、私有空间​​​才展示，所以需要使用⁠⁠⁠ showOp 属性来控制 -->
            <template v-if="showOp" #actions>
              <search-outlined @click="(e) => doSearch(picture, e)" />
              <share-alt-outlined @click="(e) => doShare(picture, e)" />
              <edit-outlined @click="(e) => doEdit(picture, e)" />
              <delete-outlined @click="(e) => doDelete(picture, e)" />
            </template>
          </a-card>
        </a-list-item>
      </template>
    </a-list>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { DeleteOutlined, EditOutlined, SearchOutlined,ShareAltOutlined } from '@ant-design/icons-vue'
import { deletePictureUsingPost } from '@/api/pictureController'
import { message } from 'ant-design-vue'
import ShareModal from './ShareModal.vue'
import { ref } from 'vue'

// 定义属性，接受 data​​​List 数据列表和 l⁠⁠⁠oading 加载状态
interface Props {
  dataList?: API.PictureVO[]
  loading?: boolean
  showOp?: boolean
  onReload: () => void
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showOp: false
})

// 跳转至图片详情
const router = useRouter()
const doClickPicture = picture => {
  router.push({
    path: `/picture/${picture.id}`
  })
}

// 快捷操作栏 编辑
const doEdit = (picture, e) => {
  // 用 e.stopPropagation 阻止事件传播
  // 否则会同时触发卡片点击事件，跳转到图片详情页
  e.stopPropagation()
  router.push({
    path: '/add_picture',
    query: {
      id: picture.id,
      spaceId: picture.spaceId
    }
  })
}

// 快捷操作栏 删除
const doDelete = async (picture, e) => {
  e.stopPropagation()
  const id = picture.id
  if (!id) return
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    // 回调通知外层组件 刷新数据
    props?.onReload()
  } else {
    message.error('删除失败')
  }
}

// 以图搜图
const doSearch = (picture, e) => {
  e.stopPropagation()
  window.open(`/search_picture?pictureId=${picture.id}`) // 点击搜索后打开新页面，进入到以图搜图结果页
}


// 分享弹窗引用
const shareModalRef = ref()
// 分享链接
const shareLink = ref<string>()

// 分享
const doShare = (picture: API.PictureVO, e: Event) => {
  e.stopPropagation()
  shareLink.value = `${window.location.protocol}//${window.location.host}/picture/${picture.id}`
  if (shareModalRef.value) {
    shareModalRef.value.openModal()
  }
}

</script>

<style scoped></style>
