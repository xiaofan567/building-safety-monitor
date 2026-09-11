<template>
  <div class="emp-page">
    <div class="breadcrumb">
      <span class="crumb-tag">监测配置</span>
      <span class="crumb-sep">/</span>
      <span class="crumb-current">环境监测管理</span>
    </div>

    <div class="page-header">
      <h2>环境监测管理</h2>
      <div class="header-tip">用于环境监测点报��阈��的统一管理与维护</div>
    </div>

    <div class="emp-layout">
      <aside class="emp-sidebar">
        <div class="emp-sidebar-header">
          <span class="sidebar-title">环境信息</span>
          <span class="sidebar-count">{{ points.length }}</span>
        </div>
        <div class="emp-sidebar-body" v-loading="loading">
          <div v-if="!points.length && !loading" class="sidebar-empty">
            <div v-if="!points.length && !loading" class="sidebar-empty">{{ loadError || '暂无监测点数据' }}</div>
          </div>
          <div
            v-for="item in points"
            :key="item.id"
            class="emp-point"
            :class="{ active: selectedPoint && selectedPoint.id === item.id }"
            @click="selectPoint(item)"
          >
            <span class="point-icon"></span>
            <span class="point-name">{{ item.pointName || '未命名监测点' }}</span>
          </div>
        </div>
      </aside>

      <section class="emp-content">
        <div class="emp-card">
          <div class="emp-card-header">
            <h3>报警配置信息</h3>
            <span v-if="selectedPoint" class="header-sub">{{ selectedPoint.pointName }}</span>
          </div>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="120px"
            label-position="right"
            class="emp-form"
            :disabled="!selectedPoint"
          >
            <el-row :gutter="20">
              <el-col :xs="24" :md="12">
                <el-form-item label="预警上限阈值" prop="warnUpper">
                  <el-input-number
                    v-model="form.warnUpper"
                    :min="0"
                    :precision="2"
                    controls-position="right"
                    placeholder="请输入预警上限阈值"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="预警下限阈值" prop="warnLower">
                  <el-input-number
                    v-model="form.warnLower"
                    :min="0"
                    :precision="2"
                    controls-position="right"
                    placeholder="请输入预警下限阈值"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="报警上限阈值" prop="alarmUpper">
                  <el-input-number
                    v-model="form.alarmUpper"
                    :min="0"
                    :precision="2"
                    controls-position="right"
                    placeholder="请输入报警上限阈值"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="报警下限阈值" prop="alarmLower">
                  <el-input-number
                    v-model="form.alarmLower"
                    :min="0"
                    :precision="2"
                    controls-position="right"
                    placeholder="请输入报警下限阈值"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="设备数据编码" prop="deviceCode">
                  <el-input v-model="form.deviceCode" placeholder="如 pm25 / airPres" maxlength="50" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="单位" prop="unit">
                  <el-input v-model="form.unit" placeholder="如 μg/m³ / hPa" maxlength="20" />
                </el-form-item>
              </el-col>
            </el-row>

            <div class="emp-form-actions">
              <button class="btn btn-ghost" type="button" :disabled="!selectedPoint" @click="resetSelection">取消选择</button>
              <button class="btn btn-primary" type="button" :disabled="!selectedPoint || saving" @click="saveForm">保存配置</button>
            </div>
          </el-form>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { selectEnvMntData, updateEnvMntPoint } from '@/api/environmentMntPoint'

const points = ref([])
const loading = ref(false)
const loadError = ref('')
const saving = ref(false)
const selectedPoint = ref(null)
const formRef = ref(null)

const form = reactive({
  id: null,
  pointCode: '',
  pointName: '',
  deviceCode: '',
  unit: '',
  alarmUpper: null,
  alarmLower: null,
  warnUpper: null,
  warnLower: null
})

const rules = {
  warnUpper: [{ required: true, message: '请输入预警上限阈值', trigger: 'blur' }],
  warnLower: [{ required: true, message: '请输入预警下限阈值', trigger: 'blur' }],
  alarmUpper: [{ required: true, message: '请输入报警上限阈值', trigger: 'blur' }],
  alarmLower: [{ required: true, message: '请输入报警下限阈值', trigger: 'blur' }],
  deviceCode: [{ required: true, message: '请输入设备数据编码', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }]
}

function toNumber(v) {
  if (v === undefined || v === null || v === '') return null
  const n = Number(v)
  return Number.isNaN(n) ? null : n
}

async function fetchPoints() {
  loading.value = true
  loadError.value = ""
  try {
    const res = await selectEnvMntData()
    points.value = res?.data || []
    if (points.value.length) {
      selectPoint(points.value[0])
    } else {
      selectedPoint.value = null
      resetForm()
    }
  } catch (e) {
    points.value = []
    selectedPoint.value = null
    const status = e?.response?.status
    loadError.value = status ? ("加载失败 (HTTP " + status + ")") : (e?.message || "加载监测点失败，请稍后重试")
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}
function selectPoint(point) {
  selectedPoint.value = point
  Object.assign(form, {
    id: point.id ?? null,
    pointCode: point.pointCode ?? '',
    pointName: point.pointName ?? '',
    deviceCode: point.deviceCode ?? '',
    unit: point.unit ?? '',
    alarmUpper: toNumber(point.alarmUpper),
    alarmLower: toNumber(point.alarmLower),
    warnUpper: toNumber(point.warnUpper),
    warnLower: toNumber(point.warnLower)
  })
  formRef.value?.clearValidate()
}

function resetForm() {
  Object.assign(form, {
    id: null,
    pointCode: '',
    pointName: '',
    deviceCode: '',
    unit: '',
    alarmUpper: null,
    alarmLower: null,
    warnUpper: null,
    warnLower: null
  })
}

function resetSelection() {
  selectedPoint.value = null
  resetForm()
}

async function saveForm() {
  if (!selectedPoint.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (form.alarmLower !== null && form.alarmUpper !== null && Number(form.alarmLower) > Number(form.alarmUpper)) {
    ElMessage.warning('报警下限阈值不能大于报警上限阈值')
    return
  }
  if (form.warnLower !== null && form.warnUpper !== null && Number(form.warnLower) > Number(form.warnUpper)) {
    ElMessage.warning('预警下限阈值不能大于预警上限阈值')
    return
  }

  saving.value = true
  try {
    await updateEnvMntPoint({
      id: form.id,
      deviceCode: form.deviceCode,
      unit: form.unit,
      alarmUpper: form.alarmUpper,
      alarmLower: form.alarmLower,
      warnUpper: form.warnUpper,
      warnLower: form.warnLower
    })
    ElMessage.success('保存成功')
    await fetchPoints()
  } catch (e) {
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchPoints()
})
</script>

<style scoped>
.emp-page {
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
  font-size: 12px;
  color: var(--text-muted);
}

.emp-layout {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 16px;
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 14px;
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.emp-sidebar {
  width: 280px;
  flex-shrink: 0;
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  background: var(--bg-white);
}

.emp-sidebar-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.sidebar-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-strong);
}
.sidebar-count {
  font-size: 12px;
  color: var(--primary-text);
  background: var(--primary-tint);
  padding: 2px 10px;
  border-radius: 12px;
  font-weight: 600;
}

.emp-sidebar-body {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}
.sidebar-empty {
  padding: 32px 12px;
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
}

.emp-point {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-secondary);
  transition: all 0.2s;
  margin-bottom: 4px;
}
.emp-point:hover {
  background: var(--bg-hover);
  color: var(--text-main);
}
.emp-point.active {
  background: var(--primary-tint);
  color: var(--primary-text);
  font-weight: 600;
}
.point-icon {
  width: 14px;
  height: 14px;
  border-radius: 3px;
  background: var(--border-strong);
  flex-shrink: 0;
}
.emp-point.active .point-icon {
  background: var(--primary);
}
.point-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.emp-content {
  flex: 1;
  min-width: 0;
  padding: 24px;
  overflow: auto;
  display: flex;
  flex-direction: column;
}

.emp-card {
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
}
.emp-card-header {
  padding: 16px 24px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.emp-card-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-strong);
}
.header-sub {
  font-size: 12px;
  color: var(--primary-text);
  background: var(--primary-tint);
  padding: 2px 10px;
  border-radius: 12px;
  font-weight: 600;
}

.emp-form {
  padding: 24px;
}
.emp-form :deep(.el-form-item__label) {
  color: var(--text-secondary);
}
.emp-form :deep(.el-input__wrapper),
.emp-form :deep(.el-select__wrapper) {
  background: var(--bg-soft);
  border-color: var(--border);
  box-shadow: none;
  border-radius: 8px;
}
.emp-form :deep(.el-input__wrapper:hover),
.emp-form :deep(.el-select__wrapper:hover) {
  border-color: var(--border-strong);
}
.emp-form :deep(.el-input__wrapper.is-focus),
.emp-form :deep(.el-select__wrapper.is-focused) {
  border-color: var(--primary);
  box-shadow: 0 0 0 1px var(--primary) inset, 0 0 0 3px rgba(176, 123, 62, 0.12);
}
.emp-form :deep(.el-input-number) {
  width: 100%;
}

.emp-form-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
  padding-top: 20px;
  border-top: 1px dashed var(--border);
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
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
.btn-primary:hover:not([disabled]) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(176, 123, 62, 0.3);
}
.btn-ghost {
  background: var(--bg-soft);
  color: var(--text-secondary);
  border: 1px solid var(--border);
}
.btn-ghost:hover:not([disabled]) {
  color: var(--primary);
  border-color: var(--primary);
}
.btn[disabled] {
  cursor: not-allowed;
  opacity: 0.55;
  transform: none;
  box-shadow: none;
}
</style>
