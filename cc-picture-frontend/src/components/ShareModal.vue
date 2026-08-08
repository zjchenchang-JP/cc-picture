<!-- 弹窗分享组件 -->
<template>
  <a-modal v-model:visible="visible" title="分享图片" :footer="false" @cancel="closeModal">
    <h4>复制分享链接</h4>
    <a-typography-link copyable>
      {{ link }}
    </a-typography-link>
    <div style="margin-bottom: 16px" />
    <h4>手机扫码查看</h4>
    <!-- qrcode 组件 将分享链接转化为二维码图片，用户扫描二维码后即可访问链接 -->
    <a-qrcode :value="link" />
  </a-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue'

/**
 * 定义组件属性类型
 */
interface Props {
  title: string
  link: string
}

/**
 * 给组件指定初始值
 */
const props = withDefaults(defineProps<Props>(), {
  title: () => '分享',
  link: () => 'https://github.com/zjchenchang-JP',
})

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

// 为了方⁡⁡⁡便其他页面使用⁠组⁠件⁠，需要暴露出 openM​oda​l 函​数⁠
// 暴露函数给父组件
defineExpose({
  openModal,
});

</script>
