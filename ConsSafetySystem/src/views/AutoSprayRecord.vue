<template>
  <TaskPlaceholder
    breadcrumb-root="施工环境"
    :page-title="pageTitle"
    :description="description"
  />
</template>

<script setup>
import TaskPlaceholder from './TaskPlaceholder.vue'

const pageTitle = "自动喷淋记录"
const description = "自动喷淋设备的启停记录与执行追溯正在开发中，后续将在此集中展示喷淋设备历史数据。"
</script>
