<script setup>
import { ref, reactive, computed, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listMonitorPoints,
  listEquipmentAssetsForSelect,
  saveOrUpdateMonitorPoint,
  deleteMonitorPoint
} from '@/api/deviceMntPoint'

// 监测类型-参数 联动数据（与后端约定一致）
const monitorTypeLinkageData = [
  {
    value: 1,
    label: '测试监测',
    children: [{ value: 2, label: '测试子类型', code: 'cszlx' }]
  },
  {
    value: 10,
    label: '环境监测',
    children: [
      { value: 1001, label: 'PM2.5', code: 'pm25' },
      { value: 1002, label: 'PM10', code: 'pm10' },
      { value: 1003, label: '噪音', code: 'noise' },
      { value: 1004, label: '温度', code: 'temp' },
      { value: 1005, label: '湿度', code: 'humi' },
      { value: 1006, label: '风向', code: 'windDir' },
      { value: 1007, label: '风速', code: 'windSpeed' },
      { value: 1008, label: 'TSP', code: 'tsp' },
      { value: 1009, label: '气压', code: 'airPres' }
    ]
  },
  {
    value: 11,
    label: '高支模监测',
    children: [
      { value: 1101, label: '压力', code: 'pressure' },
      { value: 1102, label: '设备电量', code: 'batteryLevel' },
      { value: 1103, label: '沉降', code: 'settlement' },
      { value: 1104, label: 'x 倾角', code: 'xAngle' },
      { value: 1105, label: 'y 倾角', code: 'yAngle' },
      { value: 1106, label: '传感器状态', code: 'sensorStatus' }
    ]
  },
  {
    value: 12,
    label: '深基坑监测',
    children: [
      { value: 1201, label: '土方压力', code: 'earthPressure' },
      { value: 1202, label: '土方应变力', code: 'strain' },
      { value: 1203, label: '沉降', code: 'settlement' },
      { value: 1204, label: 'x 倾角', code: 'xAngle' },
      { value: 1205, label: 'y 倾角', code: 'yAngle' },
      { value: 1206, label: '位移', code: 'horizontalDisplacement' },
      { value: 1207, label: '地下水位', code: 'waterLevel' },
      { value: 1208, label: '传感器状态', code: 'sensorStatus' }
    ]
  },
  {
    value: 13,
    label: '塔吊检测',
    children: [
      { value: 1301, label: '吊重', code: 'weight' },
      { value: 1302, label: '幅度', code: 'amplitude' },
      { value: 1303, label: '倾角', code: 'obliguity' },
      { value: 1304, label: '风速', code: 'windSpeed' },
      { value: 1305, label: '力矩', code: 'moment' },
      { value: 1306, label: '高度', code: 'height' },
      { value: 1307, label: '转角', code: 'rotation_angle' }
    ]
  },
  {
    value: 14,
    label: '升降机检测',
    children: [
      { value: 1401, label: '前门锁', code: 'beforeLock' },
      { value: 1402, label: '后门锁', code: 'afterLock' },
      { value: 1403, label: 'x 倾角', code: 'xAngle' },
      { value: 1404, label: 'y 倾角', code: 'yAngle' },
      { value: 1405, label: '高度', code: 'height' },
      { value: 1406, label: '速度', code: 'speed' },
      { value: 1407, label: '吊重', code: 'weight' },
      { value: 1408, label: '人数', code: 'numberOfPeople' },
      { value: 1409, label: '风速', code: 'wind' }
    ]
  }
]

const monitorTypeOptions = computed(() =>
  monitorTypeLinkageData.map(item => ({ value: item.value, label: item.label }))
)

const list = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 检索条件
const searchForm = reactive({
  monitorType: '',
  pointName: ''
})

// 模态框
const dialogVisible = ref(false)
const dialogMode = ref('add')
const formLoading = ref(false)
const formRef = ref(null)
const allDevices = ref([])
const isEdit = computed(() => !!form.id)

const form = reactive({
  id: null,
  pointCode: '',
  pointName: '',
  deviceCode: '',
  monitorType: '',
  monitorSonType: '',
  equipmentId: null,
  equipmentName: '',
  statisticalType: '',
  unit: '',
  sort: 0,
  alarmUpper: null,
  alarmLower: null,
  warnUpper: null,
  warnLower: null,
  rangeTime: null,
  pointLocation: '',
  pointLocation_x: '',
  pointLocation_y: '',
  pointLocation_z: '',
  isSpraySystem: '0',
  startSprayValue: '',
  closeSprayValue: ''
})

const rules = {
  pointCode: [{ required: true, message: '请输入监测点编号', trigger: 'blur' }],
  pointName: [{ required: true, message: '请输入监测点名称', trigger: 'blur' }],
  monitorType: [{ required: true, message: '请选择监测点类型', trigger: 'change' }],
  equipmentId: [{ required: true, message: '请选择所属设备', trigger: 'change' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }],
  sort: [{ required: true, message: '请输入排序', trigger: 'blur' }]
}

// 根据 monitorType 计算可选子类型
const availableSubTypes = computed(() => {
  if (form.monitorType === '' || form.monitorType === null) return []
  const parent = monitorTypeLinkageData.find(item => item.value === Number(form.monitorType))
  return parent?.children || []
})

function getMonitorTypeLabel(val) {
  if (val === null || val === undefined || val === '') return ''
  const num = Number(val)
  const parent = monitorTypeLinkageData.find(item => {
    if (item.value === num) return true
    return item.children?.some(child => child.value === num)
  })
  if (!parent) return val
  const child = parent.children?.find(c => c.value === num)
  return child ? `${parent.label} / ${child.label}` : parent.label
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listMonitorPoints({
      current: currentPage.value,
      size: pageSize.value,
      monitorType: searchForm.monitorType || undefined,
      pointName: searchForm.pointName || undefined
    })
    const d = res?.data || {}
    list.value = d.records || []
    total.value = d.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function fetchDevices() {
  try {
    const res = await listEquipmentAssetsForSelect()
    allDevices.value = res?.data?.records || []
  } catch (e) {
    allDevices.value = []
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchList()
}

function handleResetSearch() {
  searchForm.monitorType = ''
  searchForm.pointName = ''
  currentPage.value = 1
  fetchList()
}

function onPageChange(page) {
  currentPage.value = page
  fetchList()
}

function onSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  fetchList()
}

function resetForm() {
  Object.assign(form, {
    id: null,
    pointCode: '',
    pointName: '',
    deviceCode: '',
    monitorType: '',
    monitorSonType: '',
    equipmentId: null,
    equipmentName: '',
    statisticalType: '',
    unit: '',
    sort: 0,
    alarmUpper: null,
    alarmLower: null,
    warnUpper: null,
    warnLower: null,
    rangeTime: null,
    pointLocation: '',
    pointLocation_x: '',
    pointLocation_y: '',
    pointLocation_z: '',
    isSpraySystem: '0',
    startSprayValue: '',
    closeSprayValue: ''
  })
}

function openAddDialog() {
  dialogMode.value = 'add'
  resetForm()
  dialogVisible.value = true
  if (allDevices.value.length === 0) {
    fetchDevices()
  }
  nextTick(() => formRef.value?.clearValidate())
}

function openEditDialog(row) {
  dialogMode.value = 'edit'
  resetForm()
  Object.assign(form, row)
  if (row.equipmentId && allDevices.value.length === 0) {
    fetchDevices()
  } else if (row.equipmentId) {
    const exist = allDevices.value.find(d => d.assetId === row.equipmentId)
    if (!exist) {
      allDevices.value = [
        { assetId: row.equipmentId, assetName: row.equipmentName },
        ...allDevices.value
      ]
    }
  }
  if (row.pointLocation && typeof row.pointLocation === 'string') {
    const parts = row.pointLocation.split(',')
    form.pointLocation_x = parts[0] ?? ''
    form.pointLocation_y = parts[1] ?? ''
    form.pointLocation_z = parts[2] ?? ''
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function handleEquipmentChange() {
  const sel = allDevices.value.find(d => d.assetId === form.equipmentId)
  form.equipmentName = sel ? sel.assetName : ''
}

function handleMonitorTypeChange() {
  form.monitorSonType = ''
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (!form.pointLocation_x || !form.pointLocation_y || !form.pointLocation_z) {
    ElMessage.warning('位置信息（X/Y/Z）不能为空')
    return
  }

  form.pointLocation = `${form.pointLocation_x},${form.pointLocation_y},${form.pointLocation_z}`

  formLoading.value = true
  try {
    const payload = { ...form }
    if (payload.monitorSonType === '' || payload.monitorSonType === null) {
      payload.monitorSonType = ''
    }
    await saveOrUpdateMonitorPoint(payload)
    ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    formLoading.value = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(
    `确定要删除监测点「${row.pointName}」吗？删除后不可恢复。`,
    '删除确认',
    { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      await deleteMonitorPoint(row.id)
      ElMessage.success('删除成功')
      if (list.value.length === 1 && currentPage.value > 1) {
        currentPage.value -= 1
      }
      await fetchList()
    } catch (e) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchList()
  fetchDevices()
})
</script>
<template>
  <div class="dmp-page">
    <div class="breadcrumb">
      <span class="crumb-tag">监测配置</span>
      <span class="crumb-sep">/</span>
      <span class="crumb-current">设备监测点管理</span>
    </div>

    <div class="page-header">
      <h2>设备监测点管理</h2>
      <div class="header-actions">
        <button class="btn btn-primary" @click="openAddDialog">
          <svg viewBox="0 0 24 24" fill="none" width="14" height="14"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
          新增监测点
        </button>
      </div>
    </div>

    <div class="search-card">
      <div class="search-row">
        <div class="search-item">
          <label class="search-label">监测点类型</label>
          <el-select v-model="searchForm.monitorType" placeholder="请选择监测点类型" clearable style="width: 200px">
            <el-option v-for="opt in monitorTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </div>
        <div class="search-item">
          <label class="search-label">监测点名称</label>
          <el-input
            v-model="searchForm.pointName"
            placeholder="请输入监测点名称"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="search-actions">
          <button class="btn btn-primary" @click="handleSearch">
            <svg viewBox="0 0 24 24" fill="none" width="14" height="14"><circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="2"/><path d="M20 20l-3.5-3.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
            搜索
          </button>
          <button class="btn btn-ghost" @click="handleResetSearch">重置</button>
        </div>
      </div>
    </div>

    <div class="table-panel">
      <el-table :data="list" v-loading="loading" stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="64" align="center" />
        <el-table-column label="监测点类型" min-width="130">
          <template #default="{ row }">{{ getMonitorTypeLabel(row.monitorType) }}</template>
        </el-table-column>
        <el-table-column prop="pointCode" label="监测点编号" min-width="130" />
        <el-table-column prop="pointName" label="监测点名称" min-width="160" />
        <el-table-column prop="equipmentName" label="设备名称" min-width="140" />
        <el-table-column prop="unit" label="单位" min-width="80" align="center" />
        <el-table-column prop="warnUpper" label="预警上限阈值" min-width="110" align="center">
          <template #default="{ row }">{{ row.warnUpper ?? '--' }}</template>
        </el-table-column>
        <el-table-column prop="warnLower" label="预警下限阈值" min-width="110" align="center">
          <template #default="{ row }">{{ row.warnLower ?? '--' }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditDialog(row)">修改</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增监测点' : '编辑监测点'"
      width="780px"
      top="3vh"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="140px" label-position="right" size="default">
        <div class="form-section-title">基本信息</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="监测点编号" prop="pointCode">
              <el-input v-model="form.pointCode" placeholder="请输入监测点编号" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="监测点名称" prop="pointName">
              <el-input v-model="form.pointName" placeholder="请输入监测点名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="监测点类型" prop="monitorType">
              <el-select v-model="form.monitorType" placeholder="请选择监测点类型" style="width: 100%" clearable @change="handleMonitorTypeChange">
                <el-option v-for="opt in monitorTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="监测点子类型">
              <el-select v-model="form.monitorSonType" placeholder="请选择监测点子类型" style="width: 100%" clearable :disabled="!form.monitorType">
                <el-option v-for="opt in availableSubTypes" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属设备" prop="equipmentId">
              <el-select v-model="form.equipmentId" placeholder="请选择所属设备" style="width: 100%" clearable @change="handleEquipmentChange">
                <el-option v-for="d in allDevices" :key="d.assetId" :label="d.assetName" :value="d.assetId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备数据参数">
              <el-input v-model="form.deviceCode" placeholder="如 batteryLevel / pm25" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="统计类型">
              <el-select v-model="form.statisticalType" placeholder="请选择统计类型" style="width: 100%" clearable>
                <el-option label="平均" value="1" />
                <el-option label="最大值" value="2" />
                <el-option label="最小值" value="3" />
                <el-option label="总和" value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="form.unit" placeholder="如 μg/m³ / kN / °" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="form-section-title">位置信息</div>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="X 轴 (px)">
              <el-input v-model="form.pointLocation_x" placeholder="相对位置 X" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Y 轴 (px)">
              <el-input v-model="form.pointLocation_y" placeholder="相对位置 Y" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Z 轴 (px)">
              <el-input v-model="form.pointLocation_z" placeholder="相对位置 Z" maxlength="20" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="form-section-title">预警信息</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="预警上限阈值">
              <el-input-number v-model="form.alarmUpper" :min="0" controls-position="right" style="width: 100%" placeholder="预警上限阈值" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预警下限阈值">
              <el-input-number v-model="form.alarmLower" :min="0" controls-position="right" style="width: 100%" placeholder="预警下限阈值" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报警上限阈值">
              <el-input-number v-model="form.warnUpper" :min="0" controls-position="right" style="width: 100%" placeholder="报警上限阈值" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报警下限阈值">
              <el-input-number v-model="form.warnLower" :min="0" controls-position="right" style="width: 100%" placeholder="报警下限阈值" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="接警持续时间（秒）">
              <el-input-number v-model="form.rangeTime" :min="0" controls-position="right" style="width: 100%" placeholder="为 0 时立即报警" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="form-section-title">喷淋信息</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="是否关联喷淋系统">
              <el-radio-group v-model="form.isSpraySystem">
                <el-radio label="0">否</el-radio>
                <el-radio label="1">是</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启动喷淋阈值">
              <el-input-number v-model="form.startSprayValue" :min="0" controls-position="right" style="width: 100%" placeholder="启动喷淋阈值" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关闭喷淋阈值">
              <el-input-number v-model="form.closeSprayValue" :min="0" controls-position="right" style="width: 100%" placeholder="关闭喷淋阈值" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dmp-page {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  overflow: hidden;
  background: var(--bg-page);
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
.header-actions { display: flex; gap: 10px; }

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 16px;
  border-radius: 9px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
}
.btn-primary {
  background: var(--primary-grad);
  color: #fff;
  box-shadow: 0 4px 12px rgba(176, 123, 62, 0.22);
}
.btn-primary:hover { transform: translateY(-1px); box-shadow: 0 6px 16px rgba(176, 123, 62, 0.3); }
.btn-ghost {
  background: var(--bg-soft);
  color: var(--text-secondary);
  border: 1px solid var(--border);
  padding: 8px 14px;
}
.btn-ghost:hover { color: var(--primary); border-color: var(--primary); }

.search-card {
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 14px;
  box-shadow: var(--shadow-sm);
  padding: 16px 18px;
  margin-bottom: 14px;
  flex-shrink: 0;
}
.search-row {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
}
.search-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.search-label {
  font-size: 13px;
  color: var(--text-secondary);
  white-space: nowrap;
}
.search-actions {
  display: flex;
  gap: 10px;
  margin-left: auto;
}

.table-panel {
  flex: 1;
  min-height: 0;
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 14px;
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: 14px 18px;
  border-top: 1px solid var(--border);
  flex-shrink: 0;
}

.form-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--primary-text);
  border-left: 3px solid var(--primary);
  padding-left: 10px;
  margin: 4px 0 14px;
}
:deep(.el-dialog__body) { max-height: 70vh; overflow-y: auto; }

:deep(.el-table) {
  --el-table-header-bg-color: var(--bg-soft);
  --el-table-border-color: var(--border);
}
:deep(.el-table th.el-table__cell) {
  background: var(--bg-soft) !important;
  color: var(--text-secondary);
  font-weight: 600;
}
:deep(.el-table td.el-table__cell) {
  color: var(--text-main);
}
:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: var(--bg-soft);
}
:deep(.el-table .el-table__row:hover > td) {
  background: var(--bg-hover) !important;
}
:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  background: var(--bg-soft);
  border-color: var(--border);
  box-shadow: none;
  border-radius: 8px;
}
:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover) {
  border-color: var(--border-strong);
}
:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused) {
  border-color: var(--primary);
  box-shadow: 0 0 0 1px var(--primary) inset, 0 0 0 3px rgba(176, 123, 62, 0.12);
}
:deep(.el-input-number) {
  width: 100%;
}
:deep(.el-pager li.is-active) {
  background: var(--primary) !important;
  color: #fff;
}
</style>
