<template>
  <div id="homePage">
    <!-- 搜索框 -->
    <div class="search-bar">
      <a-input-search
        placeholder="从海量图片中搜索"
        v-model:value="searchParams.searchText"
        enter-button="搜索"
        size="large"
        @search="doSearch"
      />
    </div>
    <!-- 分类 -->
    <!-- 为了批量取⁡⁡⁡消选中的分类，⁠⁠新增一个“全部” 分类​ -->
    <a-tabs v-model:activeKey="selectedCategory" @change="doSearch">
      <a-tab-pane key="all" tab="全部" />
      <a-tab-pane
        v-for="category in categoryList"
        :key="category"
        :tab="category"
      />
    </a-tabs>
    <!-- tar 标签 -->
    <div class="tag-bar">
      <span style="margin-right: 8px">标签：</span>
      <a-space :size="[0, 8]" wrap>
        <a-checkable-tag
          v-for="tag in tagList"
          :key="tag"
          :checked="selectedTags.includes(tag)"
          @change="(checked: boolean) => onTagChange(tag, checked)"
        >
          {{ tag }}
        </a-checkable-tag>
      </a-space>
    </div>
    <!-- 图片展示列表 使用自定义组件 -->
    <PictureList :dataList="dataList" :loading="loading" />
    <!-- 分页 -->
    <a-pagination
      style="text-align: right"
      v-model:current="searchParams.current"
      v-model:pageSize="searchParams.pageSize"
      :total="total"
      @change="onPageChange"
    />
  </div>
</template>

<script setup lang="ts">
import { listPictureTagCategoryUsingGet, listPictureVoByPageUsingPost } from '@/api/pictureController'
import { message } from 'ant-design-vue'
import { onMounted, reactive, ref } from 'vue'
import PictureList from '@/components/PictureList.vue' // 定义数据
// const msg = '欢迎来到CC-Picture，开启图库~'

// 数据源
const dataList = ref<API.PictureVO[]>()
const total = ref(0)
const loading = ref(true) // 加载ing……状态

/**
 * 分类 + 标签
 */
const categoryList = ref<string[]>([])
const selectedCategory = ref<string>('all')
const tagList = ref<string[]>([])
const selectedTags = ref<string[]>([])

// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend'
})

/**
 * 获取数据
 */
const fetchData = async () => {
  loading.value = true // 状态重置归零
  
  // 转换搜索参数 搜索时，需要将选中的分类和标签转换为对应的请求参数
  const params = {
    ...searchParams,
    tags: [...selectedTags.value]   // 直接带上已选中的标签名
  }
  if (selectedCategory.value !== 'all') {
    params.category = selectedCategory.value
  }
  const res = await listPictureVoByPageUsingPost(params)
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

// 页面加载时请求数据
onMounted(() => {
  fetchData()
  getTagCategoryOptions()
})

// 分页参数
const onPageChange = (page: number, pageSize: number) => {
  searchParams.current = page
  searchParams.pageSize = pageSize
  fetchData()
}

// 搜索
const doSearch = () => {
  // 重置搜索条件
  searchParams.current = 1
  fetchData()
}

/**
 * 单个标签选中 / 取消选中
 */
const onTagChange = (tag: string, checked: boolean) => {
  if (checked) {
    // 选中,避免重复添加
    if (!selectedTags.value.includes(tag)) {
      selectedTags.value.push(tag)
    }
  } else {
    // 取消,把这个标签从已选列表中移除
    selectedTags.value = selectedTags.value.filter(t => t !== tag)
  }
  doSearch()
}

// 获取标签和分类选项
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    // 转换成下拉选项组件接受的格式
    categoryList.value = res.data.data.categoryList ?? []
    tagList.value = res.data.data.tagList ?? []
  } else {
    message.error('加载分类标签失败，' + res.data.message)
  }
}

</script>

<style scoped>
#homePage {
  margin-bottom: 16px;
}

#homePage .search-bar {
  max-width: 480px;
  margin: 0 auto 16px;
}

#homePage .tag-bar {
  margin-bottom: 16px;
}

/* 图片卡片间距：
  a-list 在 grid 模式下，本该清零 .ant-list-item 的 padding，
  但它的清零规则要求父元素是 .ant-col，而 a-list 实际用普通 <div> 包裹，
   导致清零失效、list-item 残留了约 24px 的左右 padding —— 这才是卡片大缝的真正来源 */
#homePage :deep(.ant-list-item) {
  padding: 0;
}
/* gutter 的水平间距在 a-list 里不生效（同样因为包裹元素是 div 不是 Col），
   所以改用外层 div 的 padding 自己控制间距，改这个 4px 即可调缝大小 */
#homePage :deep(.ant-list .ant-row > div) {
  padding: 4px 8px; /* 上下 8px、左右 12px，两个方向分开调 */
  box-sizing: border-box;
}
</style>
