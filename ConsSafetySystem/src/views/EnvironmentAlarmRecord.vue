<template>
  <div class="ear-page">
    <div class="breadcrumb">
      <span class="crumb-tag">施工环境</span>
      <span class="crumb-sep">/</span>
      <span class="crumb-current">环境监测报警记录</span>
    </div>

    <div class="page-header">
      <div>
        <h2>环境监测报警记录</h2>
        <div class="header-tip">查询环境监测预警与报警信息，及时处理异常事件</div>
      </div>
      <div class="record-summary">
        <span class="summary-dot"></span>
        共 {{ pagination.total }} 条记录
      </div>
    </div>

    <section class="search-card">
      <div class="filter-grid">
        <label class="filter-item">
          <span class="filter-label">报警类型</span>
          <el-select
            v-model="filters.alertType"
            placeholder="请选择"
            clearable
            style="width: 100%;"
          >
            <el-option label="预警" value="0" />
            <el-option label="报警" value="1" />
          </el-select>
        </label>

        <label class="filter-item">
          <span class="filter-label">处理状态</span>
          <el-select
            v-model="filters.handleState"
            placeholder="请选择"
            clearable
            style="width: 100%;"
          >
            <el-option label="未处理" value="0" />
            <el-option label="已处理" value="1" />
          </el-select>
        </label>

        <div class="filter-item">
          <span class="filter-label">报警时间</span>
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
        <h3>报警记录列表</h3>
        <div class="state-legend">
          <span class="legend-item"><span class="legend-dot legend-dot--warning"></span>预警</span>
          <span class="legend-item"><span class="legend-dot legend-dot--alarm"></span>报警</span>
          <span class="legend-item"><span class="legend-dot legend-dot--done"></span>已处理</span>
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
          <el-table-column prop="alertType" label="报警类型" min-width="100" align="center">
            <template #default="{ row }">
              <span :class="['state-tag', alertTypeClass(row.alertType)]">
                {{ alertTypeLabel(row.alertType) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="报警内容" min-width="170">
            <template #default="{ row }">{{ displayValue(row.content) }}</template>
          </el-table-column>
          <el-table-column prop="monitorData" label="报警值" min-width="110" align="center">
            <template #default="{ row }">{{ displayValue(row.monitorData) }}</template>
          </el-table-column>
          <el-table-column prop="unit" label="单位" width="82" align="center">
            <template #default="{ row }">{{ displayValue(row.unit) }}</template>
          </el-table-column>
          <el-table-column label="报警上限值" min-width="118" align="center">
            <template #default="{ row }">{{ displayLimit(row, 'upper') }}</template>
          </el-table-column>
          <el-table-column label="报警下限值" min-width="118" align="center">
            <template #default="{ row }">{{ displayLimit(row, 'lower') }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="报警时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column prop="handleState" label="处理状态" min-width="106" align="center">
            <template #default="{ row }">
              <span :class="['state-tag', handleStateClass(row.handleState)]">
                {{ handleStateLabel(row.handleState) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center" fixed="right">
            <template #default="{ row }">
              <button
                class="handle-btn"
                :class="{ 'handle-btn--done': String(row.handleState) === '1' }"
                @click="handleAlarm(row)"
              >
                {{ String(row.handleState) === '1' ? '查看' : '处理' }}
              </button>
            </template>
          </el-table-column>

          <template #empty>
            <div class="empty-state">
              <span class="empty-icon"></span>
              <span>暂无环境监测报警记录</span>
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

    <div v-if="showHandleModal" class="modal-mask" @click.self="closeHandleModal">
      <div class="handle-modal" role="dialog" aria-modal="true" aria-label="处理报警">
        <div class="modal-header">
          <div>
            <h3>{{ isHandled ? '查看报警处理' : '处理报警' }}</h3>
            <p>请填写处理信息，提交后将更新为已处理状态</p>
          </div>
          <button class="icon-close" type="button" :disabled="saving" @click="closeHandleModal">
            <svg viewBox="0 0 24 24" fill="none" width="18" height="18">
              <path d="M6 6l12 12M18 6L6 18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
        </div>

        <div class="modal-body">
          <div v-if="currentAlarm" class="alarm-detail">
            <div class="detail-item">
              <span class="detail-label">报警类型</span>
              <span :class="['state-tag', alertTypeClass(currentAlarm.alertType)]">
                {{ alertTypeLabel(currentAlarm.alertType) }}
              </span>
            </div>
            <div class="detail-item">
              <span class="detail-label">报警时间</span>
              <span class="detail-value">{{ formatDateTime(currentAlarm.createTime) }}</span>
            </div>
            <div class="detail-item detail-item--wide">
              <span class="detail-label">报警内容</span>
              <span class="detail-value">{{ displayValue(currentAlarm.content) }}</span>
            </div>
            <div class="detail-item detail-item--wide">
              <span class="detail-label">报警值</span>
              <span class="detail-value">
                {{ displayValue(currentAlarm.monitorData) }}
                <template v-if="currentAlarm.unit"> {{ currentAlarm.unit }}</template>
              </span>
            </div>
          </div>

          <div class="form-item">
            <label for="alarm-handle-by">处理人 <em>*</em></label>
            <el-input
              id="alarm-handle-by"
              v-model="handleForm.handleBy"
              maxlength="30"
              placeholder="请输入处理人姓名"
              :disabled="saving"
            />
          </div>

          <div class="form-item">
            <label for="alarm-handle-measures">处理内容 <em>*</em></label>
            <el-input
              id="alarm-handle-measures"
              v-model="handleForm.handleMeasures"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              placeholder="请输入处理内容"
              :disabled="saving"
            />
          </div>

          <div class="form-item">
            <label for="alarm-handle-conclu">处理总结 <em>*</em></label>
            <el-input
              id="alarm-handle-conclu"
              v-model="handleForm.handleConclu"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              placeholder="请输入处理总结"
              :disabled="saving"
            />
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn btn-ghost" type="button" :disabled="saving" @click="closeHandleModal">取消</button>
          <button class="btn btn-primary" type="button" :disabled="saving" @click="saveHandle">
            {{ saving ? '提交中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { selectEnvAlarmRecord, updHandleData } from '@/api/environmentMnt'

const filters = reactive({
  alertType: '',
  handleState: '',
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
const saving = ref(false)
const showHandleModal = ref(false)
const currentAlarm = ref(null)
const handleForm = reactive({
  id: null,
  handleBy: '',
  handleMeasures: '',
  handleConclu: ''
})

const isHandled = computed(() => String(currentAlarm.value?.handleState) === '1')
let requestSequence = 0

function displayValue(value) {
  if (value === '' || value === null || value === undefined) return '--'
  return value
}

function displayLimit(row, type) {
  const preferred = type === 'upper' ? row.upperValue : row.lowerValue
  const fallback = type === 'upper' ? row.alarmUpper : row.alarmLower
  return displayValue(preferred ?? fallback)
}

function formatDateTime(value) {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 19)
}

function getRowIndex(index) {
  return (pagination.currentPage - 1) * pagination.pageSize + index + 1
}

function alertTypeLabel(value) {
  const type = String(value)
  if (type === '1') return '报警'
  if (type === '0') return '预警'
  if (type === '2') return '控制'
  return '--'
}

function alertTypeClass(value) {
  const type = String(value)
  if (type === '1') return 'alarm-type--alarm'
  if (type === '0') return 'alarm-type--warning'
  return 'alarm-type--muted'
}

function handleStateLabel(value) {
  return String(value) === '1' ? '已处理' : '未处理'
}

function handleStateClass(value) {
  return String(value) === '1' ? 'handle-state--done' : 'handle-state--pending'
}

async function loadAlarmRecords() {
  const currentSequence = ++requestSequence
  loading.value = true

  try {
    const requestData = {
      page: pagination.currentPage,
      limit: pagination.pageSize,
      pageType: 1,
      monitorAlertType: '4'
    }

    if (filters.alertType) {
      requestData.alertType = filters.alertType
    }
    if (filters.handleState) {
      requestData.handleState = filters.handleState
    }
    if (filters.startDate) {
      requestData.startTime = filters.startDate + ' 00:00:00'
    }
    if (filters.endDate) {
      requestData.endTime = filters.endDate + ' 23:59:59'
    }

    const response = await selectEnvAlarmRecord(requestData)
    if (currentSequence !== requestSequence) return

    const records = Array.isArray(response?.records)
      ? response.records
      : Array.isArray(response)
        ? response
        : []
    const total = Number(response?.total)

    tableData.value = records
    pagination.total = Number.isFinite(total) ? total : records.length

    const totalPages = Math.ceil(pagination.total / pagination.pageSize)
    if (pagination.total > 0 && pagination.currentPage > totalPages) {
      pagination.currentPage = totalPages
      await loadAlarmRecords()
      return
    }
  } catch (error) {
    if (currentSequence !== requestSequence) return
    tableData.value = []
    pagination.total = 0
    console.error('环境监测报警记录加载失败:', error)
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
  loadAlarmRecords()
}

function handleReset() {
  filters.alertType = ''
  filters.handleState = ''
  filters.startDate = ''
  filters.endDate = ''
  pagination.currentPage = 1
  loadAlarmRecords()
}

function handlePageChange() {
  loadAlarmRecords()
}

function handleSizeChange(size) {
  pagination.pageSize = size
  pagination.currentPage = 1
  loadAlarmRecords()
}

function resetHandleForm() {
  handleForm.id = null
  handleForm.handleBy = ''
  handleForm.handleMeasures = ''
  handleForm.handleConclu = ''
}

function handleAlarm(row) {
  currentAlarm.value = { ...row }
  handleForm.id = row.id
  handleForm.handleBy = row.handleBy || ''
  handleForm.handleMeasures = row.handleMeasures || ''
  handleForm.handleConclu = row.handleConclu || ''
  showHandleModal.value = true
}

function closeHandleModal() {
  if (saving.value) return
  showHandleModal.value = false
  currentAlarm.value = null
  resetHandleForm()
}

async function saveHandle() {
  const handleBy = handleForm.handleBy.trim()
  const handleMeasures = handleForm.handleMeasures.trim()
  const handleConclu = handleForm.handleConclu.trim()

  if (!handleBy) {
    ElMessage.warning('请输入处理人')
    return
  }
  if (!handleMeasures) {
    ElMessage.warning('请输入处理内容')
    return
  }
  if (!handleConclu) {
    ElMessage.warning('请输入处理总结')
    return
  }

  saving.value = true
  try {
    await updHandleData({
      id: handleForm.id,
      handleBy,
      handleMeasures,
      handleConclu
    })
    ElMessage.success('报警处理成功')
    showHandleModal.value = false
    currentAlarm.value = null
    resetHandleForm()
    await loadAlarmRecords()
  } catch (error) {
    console.error('报警处理失败:', error)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadAlarmRecords()
})
</script>

<style scoped>
.ear-page {
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
  grid-template-columns: minmax(180px, 0.75fr) minmax(180px, 0.75fr) minmax(360px, 1.5fr) auto;
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
.legend-dot--warning {
  background: var(--primary);
  box-shadow: 0 0 0 3px rgba(176, 123, 62, 0.18);
}
.legend-dot--alarm {
  background: var(--danger);
  box-shadow: 0 0 0 3px var(--danger-tint);
}
.legend-dot--done {
  background: #3e8a4a;
  box-shadow: 0 0 0 3px rgba(82, 158, 91, 0.18);
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
  justify-content: center;
  min-height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}
.alarm-type--warning {
  background: rgba(176, 123, 62, 0.12);
  color: var(--primary-text);
}
.alarm-type--alarm {
  background: var(--danger-tint);
  color: var(--danger);
}
.alarm-type--muted {
  background: var(--bg-hover);
  color: var(--text-muted);
}
.handle-state--pending {
  background: rgba(235, 184, 52, 0.18);
  color: #a46b08;
}
.handle-state--done {
  background: rgba(82, 158, 91, 0.14);
  color: #3e8a4a;
}

.handle-btn {
  border: none;
  background: transparent;
  color: var(--primary-text);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.2s;
}
.handle-btn:hover {
  color: var(--primary);
}
.handle-btn--done {
  color: #3e8a4a;
}
.handle-btn--done:hover {
  color: #2f6f39;
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

.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(44, 36, 26, 0.46);
  backdrop-filter: blur(2px);
}
.handle-modal {
  display: flex;
  width: min(680px, 100%);
  max-height: calc(100vh - 48px);
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: var(--bg-white);
  box-shadow: 0 24px 64px rgba(55, 42, 28, 0.26);
}
.modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--border);
}
.modal-header h3 {
  margin: 0;
  color: var(--text-strong);
  font-size: 16px;
  font-weight: 700;
}
.modal-header p {
  margin: 5px 0 0;
  color: var(--text-muted);
  font-size: 12px;
}
.icon-close {
  display: inline-flex;
  width: 30px;
  height: 30px;
  align-items: center;
  justify-content: center;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s;
}
.icon-close:not(:disabled):hover {
  border-color: var(--border);
  background: var(--bg-hover);
  color: var(--text-main);
}
.icon-close:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
.modal-body {
  overflow-y: auto;
  padding: 18px 20px 20px;
}
.alarm-detail {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
  padding: 14px;
  margin-bottom: 18px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-soft);
}
.detail-item {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}
.detail-item--wide {
  grid-column: 1 / -1;
  align-items: flex-start;
}
.detail-label {
  flex-shrink: 0;
  color: var(--text-muted);
  font-size: 12px;
}
.detail-value {
  min-width: 0;
  color: var(--text-main);
  font-size: 12px;
  word-break: break-word;
}
.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}
.form-item:last-child {
  margin-bottom: 0;
}
.form-item label {
  color: var(--text-secondary);
  font-size: 13px;
}
.form-item label em {
  color: var(--danger);
  font-style: normal;
}
.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid var(--border);
  background: var(--bg-soft);
  flex-shrink: 0;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-date-editor.el-input__wrapper),
:deep(.el-textarea__inner) {
  background: var(--bg-soft);
  border-color: var(--border);
  box-shadow: 0 0 0 1px var(--border) inset;
  border-radius: 8px;
}
:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover),
:deep(.el-date-editor.el-input__wrapper:hover),
:deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px var(--border-strong) inset;
}
:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused),
:deep(.el-date-editor.el-input__wrapper.is-focus),
:deep(.el-textarea__inner:focus) {
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
  .ear-page {
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
  .modal-mask {
    align-items: flex-start;
    padding: 12px;
    overflow-y: auto;
  }
  .handle-modal {
    max-height: none;
  }
  .alarm-detail {
    grid-template-columns: 1fr;
  }
  .detail-item--wide {
    grid-column: auto;
  }
}
</style>
