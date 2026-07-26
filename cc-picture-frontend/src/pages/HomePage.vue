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
    <!-- 图片展示列表 -->
    <a-list
      :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 4, xl: 5, xxl: 6 }"
      :data-source="dataList"
      :pagination="pagination"
      :loading="loading"
    >
      <template #renderItem="{ item: pictureVo }">
        <a-list-item>
          <!-- 单张图片 -->
          <a-card hoverable>
            <template #cover>
              <!-- object-fit: cover 优化图片的展示效果，不会受到挤压 -->
              <!-- 图片的宽高都是不同的，为了防止页面 “参差不齐”，给所有图片统一设置相同的高度 -->
              <img
                style="height: 180px; object-fit: cover"
                :alt="pictureVo.name"
                :src="pictureVo.url" />
            </template>
            <a-card-meta :title="pictureVo.name">
              <template #description>
                <a-flex>
                  <a-tag color="green">
                    {{ pictureVo.category ?? '默认' }}
                  </a-tag>
                  <a-tag v-for="tag in pictureVo.tags" :key="tag">
                    {{ tag }}
                  </a-tag>
                </a-flex>
              </template>
            </a-card-meta>
          </a-card>
        </a-list-item>
      </template>
    </a-list>
  </div>
</template>

<script setup lang="ts">
import { listPictureVoByPageUsingPost } from '@/api/pictureController'
import { message } from 'ant-design-vue'
import { computed, onMounted, reactive, ref } from 'vue'
// const msg = '欢迎来到CC-Picture，开启图库~'

// 数据源
const dataList = ref<API.PictureVO[]>()
const total = ref(0)
const loading = ref(true) // 加载ing……状态

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
  const res = await listPictureVoByPageUsingPost(searchParams)
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.current ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    // 切换页号 页面size时,实时修改搜索参数并获取数据
    onChange: (page: number, pageSize: number) => {
      searchParams.current = page
      searchParams.pageSize = pageSize
      fetchData()
    }
  }
})

// 页面加载时请求数据
onMounted(() => {
  fetchData()
})


// 搜索
const doSearch = () => {
  // 重置搜索条件
  searchParams.current = 1
  fetchData()
}
</script>

<style scoped>
#homePage .search-bar {
  max-width: 480px;
  margin: 0 auto 16px;
}
</style>
