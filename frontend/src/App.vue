<template>
  <!-- 路由视图出口：根据当前 URL 动态渲染登录页或主工作台布局 -->
  <router-view />
  <!-- 全局轻提示悬浮容器：统一承接各页面触发的成功/失败/警告通知 -->
  <ToastContainer />
</template>

<script setup>
/**
 * DataPulse 前端根组件（应用最外层壳子）。
 * 本身不放业务逻辑，只负责渲染路由出口和全局挂载 Toast 通知容器。
 */
import ToastContainer from './components/ToastContainer.vue'
</script>
