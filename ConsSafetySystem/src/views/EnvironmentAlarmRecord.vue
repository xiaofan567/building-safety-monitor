<template>
  <TaskPlaceholder
    breadcrumb-root="施工环境"
    :page-title="pageTitle"
    :description="description"
  />
</template>

<script setup>
import TaskPlaceholder from './TaskPlaceholder.vue'

const pageTitle = "环境监测报警记录"
const description = "环境监测报警记录与处理流程正在开发中，敬请期待。"
</script>
