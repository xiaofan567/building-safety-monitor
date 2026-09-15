<template>
  <div class="esr-page">
    <div class="breadcrumb">
      <span class="crumb-tag">施工环境</span>
      <span class="crumb-sep">/</span>
      <span class="crumb-current">自动喷淋记录</span>
    </div>

    <div class="page-header">
      <div>
        <h2>自动喷淋记录</h2>
        <div class="header-tip">查询喷淋设备的启停历史记录，追溯操作原因与时间</div>
      </div>
      <div class="record-summary">
        <span class="summary-dot"></span>
        共 {{ pagination.total }} 条记录
      </div>
    </div>

    <section class="search-card">
      <div class="filter-grid">
        <label class="filter-item">
          <span class="filter-label">操作类型</span>
          <el-select
            v-model="filters.operationType"
            placeholder="请选择"
            clearable
            style="width: 100%;"
          >
            <el-option label="启动" value="1" />
            <el-option label="停止" value="0" />
            <el-option label="操作异常" value="2" />
          </el-select>
        </label>

        <label class="filter-item">
          <span class="filter-label">设备名称</span>
          <el-input
            v-model="filters.equipmentName"
            placeholder="请输入设备名称"
            clearable
            @keyup.enter="handleSearch"
          />
        </label>

        <div class="filter-item">
          <span class="filter-label">操作时间</span>
          <div class="date-range">
            <el-date-picker
              v-model="filters.startDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="开始日期"
              clearable
            />
            <span class="date-separator">至</span>
            <el-date-picker
              v-model="filters.endDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="结束日期"
              clearable
            />
          </div>
        </div>

        <div class="filter-actions">
          <button class="btn btn-primary" :disabled="loading" @click="handleSearch">
            <svg viewBox="0 0 24 24" fill="none" width="14" height="14">
              <circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="2"/>
              <path d="M20 20l-3.5-3.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
            搜索
          </button>
          <button class="btn btn-ghost" :disabled="loading" @click="handleReset">重置</button>
        </div>
      </div>
    </section>

    <section class="table-panel">
      <div class="table-panel-header">
        <h3>喷淋操作记录</h3>
        <div class="state-legend">
          <span class="legend-item"><span class="legend-dot legend-dot--start"></span>启动</span>
          <span class="legend-item"><span class="legend-dot legend-dot--stop"></span>停止</span>
          <span class="legend-item"><span class="legend-dot legend-dot--fail"></span>异常</span>
        </div>
      </div>

      <div class="table-wrap">
        <el-table
          v-loading="loading"
          :data="tableData"
          stripe
          height="100%"
          style="width: 100%"
        >
          <el-table-column
            type="index"
            label="序号"
            width="64"
            align="center"
            :index="getRowIndex"
          />
          <el-table-column prop="assetName" label="设备名称" min-width="160">
            <template #default="{ row }">{{ displayValue(row.assetName) }}</template>
          </el-table-column>
          <el-table-column prop="assetLocation" label="设备位置" min-width="160">
            <template #default="{ row }">{{ displayValue(row.assetLocation) }}</template>
          </el-table-column>
          <el-table-column prop="operationType" label="操作类型" min-width="110" align="center">
            <template #default="{ row }">
              <span :class="['state-tag', stateClass(row.operationType)]">
                {{ operationTypeLabel(row.operationType) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="operationReason" label="操作原因" min-width="280">
            <template #default="{ row }">{{ displayValue(row.operationReason) }}</template>
          </el-table-column>
          <el-table-column prop="operationTime" label="操作时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.operationTime) }}</template>
          </el-table-column>

          <template #empty>
            <div class="empty-state">
              <span class="empty-icon"></span>
              <span>暂无喷淋操作记录</span>
            </div>
          </template>
        </el-table>
      </div>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          :pager-count="5"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { selectSprayData } from '@/api/environmentMnt'

const filters = reactive({
  operationType: '',
  equipmentName: '',
  startDate: '',
  endDate: ''
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])
const loading = ref(false)
let requestSequence = 0

const totalPages = computed(() => Math.ceil(pagination.total / pagination.pageSize))

const operationTypeMap = {
  '1': '启动',
  '0': '��止',
  '2': '操作异常'
}

function operationTypeLabel(value) {
  if (value === null || value === undefined || value === '') return '-'
  return operationTypeMap[String(value)] || '-'
}

function stateClass(value) {
  const v = String(value)
  if (v === '1') return 'state-on'
  if (v === '0') return 'state-off'
  if (v === '2') return 'state-fail'
  return ''
}

function displayValue(value) {
  if (value === '' || value === null || value === undefined) return '--'
  return value
}

function formatDateTime(value) {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 19)
}

function getRowIndex(index) {
  return (pagination.currentPage - 1) * pagination.pageSize + index + 1
}

async function loadSprayRecord() {
  const currentSequence = ++requestSequence
  loading.value = true
  try {
    const requestData = {
      page: pagination.currentPage,
      limit: pagination.pageSize,
      pageType: 1,
      fieldName: 'operation_time',
      sortingType: 'DESC'
    }
    if (filters.operationType) {
      requestData.operationType = filters.operationType
    }
    if (filters.equipmentName.trim()) {
      requestData.assetName = filters.equipmentName.trim()
    }
    if (filters.startDate) {
      requestData.startTime = filters.startDate + ' 00:00:00'
    }
    if (filters.endDate) {
      requestData.endTime = filters.endDate + ' 23:59:59'
    }

    const response = await selectSprayData(requestData)
    if (currentSequence !== requestSequence) return

    const records = Array.isArray(response?.records)
      ? response.records
      : Array.isArray(response)
        ? response
        : []

    const total = Number(response?.total)
    tableData.value = records
    pagination.total = Number.isFinite(total) ? total : records.length

    if (pagination.total > 0 && pagination.currentPage > totalPages.value) {
      pagination.currentPage = totalPages.value
      await loadSprayRecord()
      return
    }
  } catch (error) {
    if (currentSequence !== requestSequence) return
    tableData.value = []
    pagination.total = 0
    console.error('自动喷淋记录加载失败:', error)
  } finally {
    if (currentSequence === requestSequence) {
      loading.value = false
    }
  }
}

function handleSearch() {
  if (filters.startDate && filters.endDate && filters.startDate > filters.endDate) {
    ElMessage.warning('开始日期不能晚于结束日期')
    return
  }
  pagination.currentPage = 1
  loadSprayRecord()
}

function handleReset() {
  filters.operationType = ''
  filters.equipmentName = ''
  filters.startDate = ''
  filters.endDate = ''
  pagination.currentPage = 1
  loadSprayRecord()
}

function handlePageChange() {
  loadSprayRecord()
}

function handleSizeChange(size) {
  pagination.pageSize = size
  pagination.currentPage = 1
  loadSprayRecord()
}

onMounted(() => {
  loadSprayRecord()
})
</script>

<style scoped>
.esr-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  padding: 20px;
  box-sizing: border-box;
  overflow: hidden;
  background: var(--bg-page);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  color: var(--text-muted);
  font-size: 12px;
  flex-shrink: 0;
}
.crumb-tag { color: var(--text-secondary); }
.crumb-sep { color: var(--text-faint); }
.crumb-current { color: var(--primary-text); font-weight: 500; }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  flex-shrink: 0;
}
.page-header h2 {
  margin: 0;
  color: var(--text-strong);
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 1px;
}
.header-tip {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 12px;
}
.record-summary {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 7px 12px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--bg-soft);
  color: var(--text-secondary);
  font-size: 12px;
  white-space: nowrap;
}
.summary-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-tint);
}

.search-card {
  padding: 16px 18px;
  margin-bottom: 14px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-white);
  box-shadow: var(--shadow-sm);
  flex-shrink: 0;
}
.filter-grid {
  display: grid;
  grid-template-columns: minmax(200px, 0.8fr) minmax(240px, 1fr) minmax(360px, 1.5fr) auto;
  align-items: end;
  gap: 18px;
}
.filter-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 8px;
}
.filter-label {
  color: var(--text-secondary);
  font-size: 13px;
}
.date-range {
  display: grid;
  grid-template-columns: minmax(140px, 1fr) auto minmax(140px, 1fr);
  align-items: center;
  gap: 8px;
}
.date-range :deep(.el-date-editor) {
  width: 100%;
}
.date-separator {
  color: var(--text-muted);
  font-size: 12px;
}
.filter-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 76px;
  padding: 9px 16px;
  border: 1px solid transparent;
  border-radius: 9px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
.btn-primary {
  background: var(--primary-grad);
  color: #fff;
  box-shadow: 0 4px 12px rgba(176, 123, 62, 0.22);
}
.btn-primary:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(176, 123, 62, 0.3);
}
.btn-ghost {
  padding: 8px 14px;
  border-color: var(--border);
  background: var(--bg-soft);
  color: var(--text-secondary);
}
.btn-ghost:not(:disabled):hover {
  border-color: var(--primary);
  color: var(--primary);
}

.table-panel {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-white);
  box-shadow: var(--shadow-sm);
}
.table-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 13px 18px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}
.table-panel-header h3 {
  margin: 0;
  color: var(--text-strong);
  font-size: 14px;
  font-weight: 700;
}
.state-legend {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: var(--text-muted);
  font-size: 11px;
}
.state-legend .legend-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.legend-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}
.legend-dot--start {
  background: #3e8a4a;
  box-shadow: 0 0 0 3px rgba(82, 158, 91, 0.18);
}
.legend-dot--stop {
  background: var(--primary);
  box-shadow: 0 0 0 3px rgba(176, 123, 62, 0.18);
}
.legend-dot--fail {
  background: var(--danger);
  box-shadow: 0 0 0 3px var(--danger-tint);
}
.table-wrap {
  min-height: 0;
  flex: 1;
}
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: 14px 18px;
  border-top: 1px solid var(--border);
  flex-shrink: 0;
}

.state-tag {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}
.state-on {
  background: rgba(82, 158, 91, 0.14);
  color: #3e8a4a;
}
.state-off {
  background: rgba(176, 123, 62, 0.12);
  color: var(--primary-text);
}
.state-fail {
  background: var(--danger-tint);
  color: var(--danger);
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  min-height: 180px;
  color: var(--text-muted);
  font-size: 13px;
}
.empty-icon {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--border-strong);
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-date-editor.el-input__wrapper) {
  background: var(--bg-soft);
  border-color: var(--border);
  box-shadow: 0 0 0 1px var(--border) inset;
  border-radius: 8px;
}
:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover),
:deep(.el-date-editor.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--border-strong) inset;
}
:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused),
:deep(.el-date-editor.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--primary) inset;
}
:deep(.el-table) {
  --el-table-header-bg-color: var(--bg-soft);
  --el-table-border-color: var(--border);
  --el-table-row-hover-bg-color: var(--bg-hover);
  color: var(--text-main);
}
:deep(.el-table th.el-table__cell) {
  background: var(--bg-soft) !important;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
}
:deep(.el-table td.el-table__cell) {
  color: var(--text-main);
  font-size: 12px;
}
:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: var(--bg-soft);
}
:deep(.el-table .el-table__row:hover > td) {
  background: var(--bg-hover) !important;
}
:deep(.el-pagination) {
  --el-pagination-button-bg-color: var(--bg-soft);
  --el-pagination-hover-color: var(--primary);
}
:deep(.el-pagination.is-background .el-pager li.is-active) {
  background: var(--primary);
}
:deep(.el-pagination.is-background .btn-prev:not(:disabled):hover),
:deep(.el-pagination.is-background .btn-next:not(:disabled):hover),
:deep(.el-pagination.is-background .el-pager li:not(.is-active):hover) {
  color: var(--primary);
}

@media (max-width: 1280px) {
  .filter-grid {
    grid-template-columns: minmax(200px, 1fr) minmax(240px, 1fr);
  }
  .filter-grid .filter-actions {
    grid-column: 1 / -1;
  }
}

@media (max-width: 760px) {
  .esr-page {
    padding: 14px;
    overflow-y: auto;
  }
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }
  .filter-grid {
    grid-template-columns: 1fr;
  }
  .date-range {
    grid-template-columns: 1fr;
  }
  .date-separator {
    display: none;
  }
  .filter-actions {
    grid-column: auto;
    justify-content: flex-start;
  }
  .table-panel {
    min-height: 560px;
    flex: none;
  }
  .table-panel-header {
    align-items: flex-start;
    flex-direction: column;
  }
  .pagination-bar {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
