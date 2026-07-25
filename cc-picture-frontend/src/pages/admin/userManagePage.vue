<template>
  <div id="userManagePage">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="账号">
        <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" allow-clear />
      </a-form-item>
      <a-form-item label="用户名">
        <a-input v-model:value="searchParams.userName" placeholder="输入用户名" allow-clear />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>
    <div style="margin-bottom: 16px" />
    <!-- 数据展示表格 -->
    <a-table 
      :columns="columns" 
      :data-source="dataList"
      :pagination="pagination"
      @change="doTableChange"
    >
      <template #headerCell="{ column }">
        <template v-if="column.dataIndex === 'userName'">
          <span>
            <smile-outlined />
            用户名
          </span>
        </template>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'userAvatar'">
          <a-image :src="record.userAvatar" :width="90" />
        </template>  
        <template v-if="column.dataIndex === 'userRole'">
          <div v-if="record.userRole === 'admin'">
            <a-tag color="green">系统管理员</a-tag>
          </div>
          <div v-else>
            <a-tag color="blue">普通用户</a-tag>
          </div>
        </template>
        <template v-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button danger @click="doDelete(record.id)">删除</a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { deleteUserUsingPost, listUserVoByPageUsingPost } from '@/api/userController';
import { SmileOutlined, DownOutlined } from '@ant-design/icons-vue';
import { message } from 'ant-design-vue';
import { computed, onMounted, reactive, ref, watch } from 'vue';
import dayjs from 'dayjs'
// 定义表格列
const columns = [
  {
    title: 'id',
    dataIndex: 'id',
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
  },
  {
    title: '用户名',
    dataIndex: 'userName',
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

// 数据
const dataList = ref<API.UserVO[]>([])
const total = ref(0)

// 搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  current: 1,
  pageSize: 10,
})

// 获取数据
const fetchData = async () => {
  const res = await listUserVoByPageUsingPost({
    ...searchParams
  })
  if (res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}

// 分页参数
// 编写一个分页变量，指定当前页号、页面大小、数据总数、展示总数的文案
// 由于这些⁡⁡⁡参数都是动态变化的，需要使⁠⁠⁠用 Vue 的 computed 计算属性, 否则当searchParams⁠⁠改变时, 分页参数并不会更新
const pagination = computed(() => {
  return {
    current: searchParams.current ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total) => `共 ${total} 条`,
  }
})

// 表格变化之后，重新获取数据
// d⁡⁡⁡oTableChange⁠⁠⁠函数, 当用户切换页号和页面大小时, 需要更新s​​​earchParams ⁠⁠⁠搜索条件的值，并触发搜索
const doTableChange = (page: any) => {
  searchParams.current = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索数据
const doSearch = () => {
  // 重置页码
  searchParams.current = 1
  fetchData()
}

// 清除搜索条件后自动搜索
// 点 allow-clear 的 × 会把对应字段置为空串，watch 捕获到"值变空"即自动搜索（直接监听数据，不依赖组件事件）
// 有值时（含正常输入）不触发，仍走手动点"搜索"按钮
watch(
  () => searchParams.userAccount,
  val => {
    if (!val) doSearch()
  }
)
watch(
  () => searchParams.userName,
  val => {
    if (!val) doSearch()
  }
)


// 删除用户
const doDelete = async (id: string) => {
  if (!id) {
    return
  }
  // id 运行时是 string（后端 Long 雪花大数经 ToStringSerializer 序列化为字符串，避免 JS number 精度丢失），
  // 但OpenAPI生成的 DeleteRequest.id 类型标注为 number，类型与运行时不一致，这里用断言对齐
  // ⚠️ 不能用 Number(id) 转换：会丢精度（末尾变 0），导致删错记录
  const res = await deleteUserUsingPost({ id } as unknown as API.DeleteRequest)
  if (res.data.code === 0) {
    message.success('删除成功')
    // 刷新页面数据
    fetchData()
  } else {
    message.error('删除失败')
  }
}



// 页面加载时请求一次数据
onMounted(()=>{
  fetchData()
})

</script>
<style scoped>
</style>
