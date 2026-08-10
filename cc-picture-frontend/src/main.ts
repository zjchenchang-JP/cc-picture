import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import '@/access.ts'
import VueCropper from 'vue-cropper'
import 'vue-cropper/dist/index.css'

import VChart from 'vue-echarts' // 图表组件
import 'echarts' // 全量注册所有图表能力（一次即可，全局生效）

const app = createApp(App)
app.use(Antd)
app.use(VueCropper) // 引入图片编辑功能库
app.use(createPinia())
app.use(router)

app.component('VChart', VChart) // 全局注册，<v-chart> 在任意页面可用

app.mount('#app')
