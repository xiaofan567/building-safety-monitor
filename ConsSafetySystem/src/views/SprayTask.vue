<template>
  <div class="st-page">
    <div class="breadcrumb">
      <span class="crumb-tag">施工环境</span>
      <span class="crumb-sep">/</span>
      <span class="crumb-current">喷淋任务</span>
    </div>

    <div class="page-header">
      <div>
        <h2>喷淋任务</h2>
        <div class="header-tip">维护施工现场自动喷淋的计划任务和参数</div>
      </div>
      <div class="record-summary">
        <span class="summary-dot"></span>
        共 {{ pagination.total }} 条任务
      </div>
    </div>

    <section class="search-card">
      <div class="search-row">
        <label class="search-item">
          <span class="search-label">���务名称</span>
          <el-input
            v-model="search.taskName"
            placeholder="请输入任务名称"
            clearable
            @keyup.enter="handleSearch"
          />
        </label>
        <label class="search-item">
          <span class="search-label">启停状态</span>
          <el-select
            v-model="search.isEnable"
            placeholder="全部"
            clearable
            style="width: 160px;"
          >
            <el-option label="启动" value="0" />
            <el-option label="停止" value="1" />
          </el-select>
        </label>
        <div class="search-actions">
          <button class="btn btn-primary" :disabled="loading" @click="handleSearch">
            <svg viewBox="0 0 24 24" fill="none" width="14" height="14">
              <circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="2"/>
              <path d="M20 20l-3.5-3.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
            搜索
          </button>
          <button class="btn btn-ghost" :disabled="loading" @click="handleReset">重置</button>
          <button class="btn btn-primary" @click="openCreate">
            <svg viewBox="0 0 24 24" fill="none" width="14" height="14">
              <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
            新增任务
          </button>
        </div>
      </div>
    </section>

    <section class="table-panel">
      <div class="table-panel-header">
        <h3>任务列表</h3>
        <div class="table-legend">启停状态：0 = 启动，1 = 停止</div>
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
          <el-table-column prop="taskName" label="任务名称" min-width="160">
            <template #default="{ row }">{{ row.taskName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="startTime" label="启动时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
          </el-table-column>
          <el-table-column prop="duration" label="喷淋时长（分钟）" min-width="150" align="center">
            <template #default="{ row }">{{ row.duration ?? '-' }}</template>
          </el-table-column>
          <el-table-column prop="cycle" label="执行周期" min-width="110" align="center">
            <template #default="{ row }">{{ row.cycle ?? '-' }}</template>
          </el-table-column>
          <el-table-column prop="cycleUnit" label="执行周期单位" min-width="140" align="center">
            <template #default="{ row }">{{ cycleUnitLabel(row.cycleUnit) }}</template>
          </el-table-column>
          <el-table-column prop="isEnable" label="启停状态" min-width="110" align="center">
            <template #default="{ row }">
              <span :class="['state-tag', row.isEnable === '0' ? 'state-on' : 'state-off']">
                {{ row.isEnable === '0' ? '启动' : '停止' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="nextTime" label="下次执行时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.nextTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <div class="row-actions">
                <button class="link-btn" @click="openEdit(row)">修改</button>
                <button class="link-btn link-danger" @click="handleDelete(row)">删除</button>
              </div>
            </template>
          </el-table-column>

          <template #empty>
            <div class="empty-state">
              <span class="empty-icon"></span>
              <span>暂无喷淋任务</span>
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
          @current-change="fetchList"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'edit' ? '编辑喷淋任务' : '新增喷淋任务'"
      width="640px"
      :close-on-click-modal="false"
      @closed="resetEditForm"
    >
      <el-form
        ref="formRef"
        :model="editForm"
        :rules="rules"
        label-width="110px"
        class="st-form"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="任务名称" prop="taskName">
              <el-input v-model="editForm.taskName" placeholder="请输入任务名称" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启停状态" prop="isEnable">
              <el-select v-model="editForm.isEnable" placeholder="请选择" style="width: 100%">
                <el-option label="启动" value="0" />
                <el-option label="停止" value="1" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启动时间" prop="startTime">
              <el-date-picker
                v-model="editForm.startTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择启动时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="下次执行时间" prop="nextTime">
              <el-date-picker
                v-model="editForm.nextTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择下次执行时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="喷淋时长(分钟)" prop="duration">
              <el-input-number
                v-model="editForm.duration"
                :min="1"
                :max="1440"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="执行周期" prop="cycle">
              <el-input-number
                v-model="editForm.cycle"
                :min="1"
                :max="999"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="执行周期单位" prop="cycleUnit">
              <el-select v-model="editForm.cycleUnit" placeholder="请选择" style="width: 100%">
                <el-option label="小时" value="HOURS" />
                <el-option label="天" value="DAYS" />
                <el-option label="周" value="WEEKS" />
                <el-option label="月" value="MONTHS" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, nextTick, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { selectSprayTaskData, saveSprayTask, deleteSprayTask } from '@/api/environmentMnt'

const loading = ref(false)
const saving = ref(false)
const tableData = ref([])

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const search = reactive({
  taskName: '',
  isEnable: ''
})

const dialogVisible = ref(false)
const dialogMode = ref('add')
const formRef = ref(null)

const blankForm = () => ({
  id: null,
  taskName: '',
  startTime: '',
  duration: null,
  cycle: null,
  cycleUnit: 'DAYS',
  isEnable: '0',
  nextTime: ''
})

const editForm = reactive(blankForm())

const requiredFields = [
  { field: 'taskName', label: '任务名称' },
  { field: 'startTime', label: '启动时间' },
  { field: 'duration', label: '喷淋时长（分钟）' },
  { field: 'cycle', label: '执行周期' },
  { field: 'cycleUnit', label: '执行周期单位' },
  { field: 'isEnable', label: '启停状态' },
  { field: 'nextTime', label: '下次执行时间' }
]

const rules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择启动时间', trigger: 'change' }],
  duration: [{ required: true, message: '请输入喷淋时长', trigger: 'blur' }],
  cycle: [{ required: true, message: '请输入执行周期', trigger: 'blur' }],
  cycleUnit: [{ required: true, message: '请选择执行周期单位', trigger: 'change' }],
  isEnable: [{ required: true, message: '请选择启停状态', trigger: 'change' }],
  nextTime: [{ required: true, message: '请选择下次执行时间', trigger: 'change' }]
}

const cycleUnitMap = {
  HOURS: '小时',
  DAYS: '天',
  WEEKS: '周',
  MONTHS: '月'
}
function cycleUnitLabel(value) {
  return cycleUnitMap[value] || value || '-'
}

function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(typeof value === 'string' ? value.replace(' ', 'T') : value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (n) => String(n).padStart(2, '0')
  return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) + ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes()) + ':' + pad(date.getSeconds())
}

function getRowIndex(index) {
  return (pagination.currentPage - 1) * pagination.pageSize + index + 1
}

async function fetchList() {
  loading.value = true
  try {
    const payload = {
      page: pagination.currentPage,
      limit: pagination.pageSize,
      pageType: 1,
      taskName: search.taskName || undefined,
      isEnable: search.isEnable || undefined
    }
    const data = await selectSprayTaskData(payload)
    tableData.value = data?.records || []
    pagination.total = data?.total || 0
  } catch (e) {
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.currentPage = 1
  fetchList()
}

function handleReset() {
  search.taskName = ''
  search.isEnable = ''
  pagination.currentPage = 1
  fetchList()
}

function handleSizeChange(size) {
  pagination.pageSize = size
  pagination.currentPage = 1
  fetchList()
}

function openCreate() {
  dialogMode.value = 'add'
  Object.assign(editForm, blankForm())
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row) {
  dialogMode.value = 'edit'
  Object.assign(editForm, blankForm(), {
    id: row.id,
    taskName: row.taskName,
    startTime: row.startTime ? formatDateTime(row.startTime) : '',
    duration: row.duration,
    cycle: row.cycle,
    cycleUnit: row.cycleUnit || 'DAYS',
    isEnable: row.isEnable || '0',
    nextTime: row.nextTime ? formatDateTime(row.nextTime) : ''
  })
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function resetEditForm() {
  Object.assign(editForm, blankForm())
  formRef.value?.clearValidate()
}

function validateRequired() {
  for (const { field, label } of requiredFields) {
    const value = editForm[field]
    if (value === null || value === undefined || String(value).trim() === '') {
      ElMessage.warning(label + '不能为空')
      return false
    }
  }
  return true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!validateRequired()) return
  saving.value = true
  try {
    const payload = {
      id: editForm.id,
      taskName: editForm.taskName,
      startTime: editForm.startTime,
      nextTime: editForm.nextTime,
      duration: Number(editForm.duration),
      cycle: Number(editForm.cycle),
      cycleUnit: editForm.cycleUnit,
      isEnable: editForm.isEnable
    }
    await saveSprayTask(payload)
    ElMessage.success(dialogMode.value === 'edit' ? '修改成功' : '新增成功')
    dialogVisible.value = false
    pagination.currentPage = 1
    fetchList()
  } catch (e) {
    ElMessage.error(e?.msg || '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除任务「' + row.taskName + '」吗？', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await deleteSprayTask(row.id)
    ElMessage.success('删除成功')
    if (tableData.value.length === 1 && pagination.currentPage > 1) {
      pagination.currentPage -= 1
    }
    fetchList()
  } catch (e) {
    ElMessage.error(e?.msg || '删除失败，请稍后重试')
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.st-page {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: var(--bg-page);
  overflow: hidden;
}

.breadcrumb {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.crumb-tag { color: var(--text-secondary); }
.crumb-sep { color: var(--text-faint); }
.crumb-current { color: var(--primary-text); font-weight: 500; }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-shrink: 0;
}
.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: var(--text-strong);
  letter-spacing: 1px;
}
.header-tip {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-muted);
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
.search-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 18px;
}
.search-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 260px;
}
.search-label {
  font-size: 13px;
  color: var(--text-secondary);
  white-space: nowrap;
}
.search-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
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
.table-legend {
  font-size: 11px;
  color: var(--text-muted);
}
.table-wrap {
  flex: 1;
  min-height: 0;
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

.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}
.link-btn {
  border: none;
  background: transparent;
  padding: 0;
  font-size: 12px;
  color: var(--primary);
  cursor: pointer;
}
.link-btn:hover {
  color: var(--primary-text);
}
.link-btn.link-danger {
  color: var(--danger);
}
.link-btn.link-danger:hover {
  color: #d65a4e;
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

.st-form :deep(.el-input__wrapper),
.st-form :deep(.el-select__wrapper),
.st-form :deep(.el-date-editor.el-input__wrapper) {
  background: var(--bg-soft);
  border-color: var(--border);
  box-shadow: 0 0 0 1px var(--border) inset;
  border-radius: 8px;
}
.st-form :deep(.el-input__wrapper:hover),
.st-form :deep(.el-select__wrapper:hover),
.st-form :deep(.el-date-editor.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--border-strong) inset;
}
.st-form :deep(.el-input__wrapper.is-focus),
.st-form :deep(.el-select__wrapper.is-focused),
.st-form :deep(.el-date-editor.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--primary) inset;
}
.st-form :deep(.el-input-number) {
  width: 100%;
}

:deep(.el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
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
</style>
