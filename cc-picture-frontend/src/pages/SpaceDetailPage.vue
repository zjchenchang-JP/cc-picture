<!-- 空间详情页 -->
<template>
  <div id="spaceDetailPage">
    <!-- 空间信息 -->
    <a-flex justify="space-between">
      <h2>{{ space.spaceName }}（{{ SPACE_TYPE_MAP[space.spaceType] }}）</h2>
      <a-space size="middle">
        <a-button
          type="primary"
          :href="`/add_picture?spaceId=${id}`"
          target="_blank"
        >
          + 创建图片
        </a-button>
        <a-button
          v-if="space.spaceType == 1"
          type="primary"
          ghost
          :icon="h(TeamOutlined)"
          :href="`/spaceUserManage/${id}`"
          target="_blank"
        >
          成员管理
        </a-button>
        <a-button
          type="primary"
          ghost
          :icon="h(BarChartOutlined)"
          :href="`/space_analyze?spaceId=${id}`"
          target="_blank"
        >
          空间分析
        </a-button>
        <a-button :icon="h(EditOutlined)" @click="doBatchEdit"> 批量编辑</a-button>
        <a-tooltip
          placement="left"
          :title="`占用空间 ${formatSize(space.totalSize)} / ${formatSize(space.maxSize)}`"
        >
          <a-progress
            type="circle"
            :size="42"
            :percent="spacePercent.toFixed(1)"
            :status="spaceStatus"
          />
        </a-tooltip>
      </a-space>
    </a-flex>
    <div style="margin-bottom: 5px" />
    <!-- 搜索表单 -->
    <PictureSearchForm :onSearch="onSearch" />
    
    <!-- 按颜色搜索 -->
    <!-- fo⁡⁡⁡rmat 要设⁠置⁠为⁠ hex 得到十六进制的颜​色值 便于后端计算颜色相似值​ -->
    <a-form-item label="按颜色搜索" style="margin-top: 16px">
      <color-picker format="hex" @pureColorChange="onColorChange" />
    </a-form-item>
    <!-- 图片列表 -->
    <PictureList :dataList="dataList" :loading="loading" showOp :onReload="fetchData"/>
    <!-- 分页 -->
    <a-pagination
      style="text-align: right"
      v-model:current="searchParams.current"
      v-model:pageSize="searchParams.pageSize"
      :total="total"
      :show-total="() => `图片总数 ${total} / ${space.maxCount}`"
      @change="onPageChange"
    />
    <!-- 引入批量编辑图片组件 -->
    <BatchEditPictureModal
      ref="batchEditPictureModalRef"
      :spaceId="id"
      :pictureList="dataList"
      :onSuccess="onBatchEditPictureSuccess"
    />
  </div>
</template>
<script setup lang="ts">
import { listPictureVoByPageUsingPost, searchPictureByColorUsingPost } from '@/api/pictureController';
import { getSpaceVoByIdUsingGet } from '@/api/spaceController';
import { formatSize } from '@/utils'
import { message } from 'ant-design-vue';
import { computed, onMounted, reactive, ref, h, watch } from 'vue'
import PictureList from '@/components/PictureList.vue'
import PictureSearchForm from '@/components/PictureSearchForm.vue'
import { ColorPicker } from 'vue3-colorpicker'
import 'vue3-colorpicker/style.css'
import BatchEditPictureModal from '@/components/BatchEditPictureModal.vue';
import { EditOutlined,BarChartOutlined,TeamOutlined } from '@ant-design/icons-vue'
import { SPACE_TYPE_MAP } from '@/constants/space';

interface Props {
  id: string | number
}

const props = defineProps<Props>()
const space = ref<API.SpaceVO>({})

// 已用空间百分比：数据没回来 / 分母为 0 时显示 0；允许超出 100%（超容提醒，类似云盘）
const spacePercent = computed(() => {
  const total = space.value.totalSize
  const max = space.value.maxSize
  if (!total || !max) return 0
  return (total * 100) / max
})

// 超出上限时圆环变红，提醒用户该充值了；未超出时不传，交给 a-progress 用默认色
const spaceStatus = computed(() => (spacePercent.value > 100 ? 'exception' : undefined))

// -------- 获取空间详情 --------
const fetchSpaceDetail = async () => {
  try {
    const res = await getSpaceVoByIdUsingGet({
      id: props.id,
    })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data
    } else {
      message.error('获取空间详情失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取空间详情失败：' + e.message)
  }
}

onMounted(() => {
  fetchSpaceDetail()
})


// --------- 获取图片列表 --------

// 定义数据 响应式变量 和 searchParams 怎么决策使用？区别在哪？？
// 界面上要显示、要跟着变的，用响应式
const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(true)

// 搜索条件
const searchParams = ref<API.PictureQueryRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 获取数据
const fetchData = async () =>{
  loading.value = true
  // 转换搜索参数
  const params = {
    spaceId: props.id, // const id = route.query?.id
    ...searchParams.value
  }
  const res = await listPictureVoByPageUsingPost(params)
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  // 请求结束（无论成功失败）都要关掉 loading，否则 a-list 一直显示骨架屏，图片不渲染
  loading.value = false
}

// 页面加载时获取数据，请求一次
onMounted(() => {
  fetchData()
})

// ⭐ 处理「同组件复用」：从 /space/A 跳到 /space/B 时，Vue Router 复用同一个 SpaceDetailPage 实例，
// 组件不会重新挂载、onMounted 不再触发 → 不重新拉数据，页面仍显示上一个空间的图片。
// 用 watch 监听 id 变化，手动重置搜索条件并重新加载。
watch(() => props.id, () => {
  // 重置搜索条件，避免上个空间的分页/搜索关键词残留（如 A 翻到第 5 页、B 只有 2 页会查空）
  searchParams.value = {
    current: 1,
    pageSize: 12,
    sortField: 'createTime',
    sortOrder: 'descend',
  }
  dataList.value = []
  total.value = 0
  fetchSpaceDetail()
  fetchData()
})

// 分页参数 调用处没有传入，数据从哪来？
// a-pagination 组件自己给的。
// 当用户点页码、改每页条数时，它会主动抛出一个 change 事件，并且按规定带上两个参数：
const onPageChange = (page: number, pageSize: number) => {
  // searchParams.current = page
  // searchParams.pageSize = pageSize
  // v-model 已经帮你把 searchParams 改好了，这里只管重新请求数据
  fetchData()
}

// ------ 搜索 -------
const onSearch = (newSearchParams: API.PictureQueryRequest) => {
  searchParams.value = {
    ...searchParams.value,
    ...newSearchParams,
    current: 1 // 展开运算符 后面的覆盖前面的同名键
  }
  fetchData()
}

// ----- 按颜色 搜索 -----
const onColorChange = async (color: string) => {
  loading.value = true
  const res = await searchPictureByColorUsingPost({
    picColor: color,
    spaceId: props.id
  })
  if (res.data.code === 0 && res.data.data) {
    const data = res.data.data ?? []
    dataList.value = data
    total.value = data.length;
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}


// ----- 批量编辑图片组件 弹窗引用 -----
// 通过 ⁡⁡⁡ref 获取到⁠弹⁠窗⁠的引用，在批量编辑成功后​要刷新​图片列​表⁠数据
const batchEditPictureModalRef = ref()

// 批量编辑成功后，刷新数据
const onBatchEditPictureSuccess = () => {
  fetchData()
}

// 打开弹窗组件
const doBatchEdit = () => {
  if (batchEditPictureModalRef.value) batchEditPictureModalRef.value.openModal()
}

</script>
<style scoped></style>
