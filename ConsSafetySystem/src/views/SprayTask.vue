<template>
  <TaskPlaceholder
    breadcrumb-root="施工环境"
    :page-title="pageTitle"
    :description="description"
  />
</template>

<script setup>
import TaskPlaceholder from './TaskPlaceholder.vue'

const pageTitle = "喷淋任务"
const description = "喷淋任务的定时计划与参数配置正在开发中，完成后可在该页面维护自动喷淋策略。"
</script>
