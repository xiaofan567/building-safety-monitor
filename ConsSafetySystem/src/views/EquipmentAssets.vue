<script setup>
import { ref, reactive, computed, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEquipmentAssetsList, saveEquipmentAssets, deleteEquipmentAssets } from '@/api/equipmentAssets'
import { getEquipmentTypeTree } from '@/api/equipmentType'
import { getEquipmentLocationTree } from '@/api/equipmentLocation'

const list = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 类型树与位置下拉
const typeTree = ref([])
const typeOptions = ref([])
const locationOptions = ref([])

// 类型筛选（后端 list 不支持 typeId 过滤，暂在当前页做客户端过滤）
const selectedTypeId = ref(null)

const dialogVisible = ref(false)
const dialogMode = ref('add') // add / edit
const formLoading = ref(false)
const formRef = ref(null)
// 记录编辑前状态，用于在线→离线时的二次确认
const originalOfflineStatus = ref(null)

const form = reactive({
  // 基本信息
  assetId: null,
  assetName: '',
  assetCode: '',
  typeId: null,
  locationId: null,
  brand: '',
  specification: '',
  model: '',
  status: '1',
  offlineStatus: '0',
  sort: 0,
  // 升降机扩展
  ratedWeight: null,
  basicHeight: null,
  ascendingSpeed: null,
  ratedLoad: null,
  cabinSize: '',
  maxAscendingHeight: null,
  // 塔吊扩展
  forwardArmLength: null,
  backwardArmLength: null,
  maxLoad: null,
  maxHeight: null,
  ratedMoment: null,
  // 其他信息
  factory: '',
  productionDate: null,
  supplier: '',
  supplyDate: null,
  acceptanceDate: null,
  installDate: null,
  startDate: null,
  scrapPre: null,
  original: null,
  originalPre: null,
  serviceTerm: null,
  serviceDate: null,
  remarks: ''
})

const rules = {
  assetCode: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  assetName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  typeId: [{ required: true, message: '请选择设备类型', trigger: 'change' }]
}

const displayList = computed(() => {
  if (selectedTypeId.value == null) return list.value
  return list.value.filter(r => r.typeId === selectedTypeId.value)
})

const isEdit = computed(() => !!form.assetId)

const selectedTypeName = computed(() => {
  if (!form.typeId) return ''
  const opt = typeOptions.value.find(o => o.value === form.typeId)
  return opt ? opt.label : ''
})

const selectedFilterTypeName = computed(() => {
  const opt = typeOptions.value.find(o => o.value === selectedTypeId.value)
  return opt ? opt.label : ''
})

// 是否展示扩展信息：塔吊按名称含“塔”，升降机按名称含“升降”
const showTowerExt = computed(() => selectedTypeName.value.includes('塔'))
const showLiftExt = computed(() => selectedTypeName.value.includes('升降'))

// 工具：类型/位置树递归展平
function collectOptions(nodes, result, idKey, nameKey) {
  if (!Array.isArray(nodes)) return
  nodes.forEach(node => {
    result.push({ value: node[idKey], label: node[nameKey] })
    if (node.children && node.children.length) collectOptions(node.children, result, idKey, nameKey)
  })
}

// 类型树补充前端展示字段
function normalizeTypeTree(nodes) {
  if (!Array.isArray(nodes)) return []
  return nodes.map(node => ({
    ...node,
    _expanded: true,
    children: node.children && node.children.length ? normalizeTypeTree(node.children) : undefined
  }))
}

const flatTypeTree = computed(() => {
  const result = []
  function flatten(nodes, depth = 0) {
    if (!nodes) return
    nodes.forEach(node => {
      node._depth = depth
      result.push(node)
      if (node.children && node.children.length > 0 && node._expanded) {
        flatten(node.children, depth + 1)
      }
    })
  }
  flatten(typeTree.value)
  return result
})

function toggleTypeExpand(node) {
  node._expanded = !node._expanded
}

function hasTypeChildren(node) {
  return node.children && node.children.length > 0
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getEquipmentAssetsList({ current: currentPage.value, size: pageSize.value })
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

async function fetchOptions() {
  try {
    const tRes = await getEquipmentTypeTree()
    const tree = tRes?.data || []
    typeTree.value = normalizeTypeTree(tree)
    const tOpts = []
    collectOptions(tree, tOpts, 'typeId', 'typeName')
    typeOptions.value = tOpts
  } catch (e) { /* ignore */ }

  try {
    const lRes = await getEquipmentLocationTree()
    const lOpts = []
    collectOptions(lRes?.data || [], lOpts, 'locationId', 'locationName')
    locationOptions.value = lOpts
  } catch (e) { /* ignore */ }
}

// ---- 类型筛选 ----
function handleTypeNodeClick(node) {
  selectedTypeId.value = node.typeId
  currentPage.value = 1
}
function selectAllType() {
  selectedTypeId.value = null
  currentPage.value = 1
}

// ---- 弹窗 ----
function resetForm() {
  Object.assign(form, {
    assetId: null, assetName: '', assetCode: '', typeId: null, locationId: null,
    brand: '', specification: '', model: '', status: '1', offlineStatus: '0', sort: 0,
    ratedWeight: null, basicHeight: null, ascendingSpeed: null, ratedLoad: null,
    cabinSize: '', maxAscendingHeight: null,
    forwardArmLength: null, backwardArmLength: null, maxLoad: null, maxHeight: null, ratedMoment: null,
    factory: '', productionDate: null, supplier: '', supplyDate: null,
    acceptanceDate: null, installDate: null, startDate: null, scrapPre: null,
    original: null, originalPre: null, serviceTerm: null, serviceDate: null, remarks: ''
  })
}

function openDialog(mode, row) {
  dialogMode.value = mode
  resetForm()
  form.assetId = null
  originalOfflineStatus.value = null
  if (mode === 'add') {
    // form 已重置
  } else {
    // 编辑/配置回填基本字段（扩展字段后端 get 未返回 extraParams，暂置空）
    Object.assign(form, {
      assetId: row.assetId, assetName: row.assetName || '', assetCode: row.assetCode || '',
      typeId: row.typeId ?? null, locationId: row.locationId ?? null,
      brand: row.brand || '', specification: row.specification || '', model: row.model || '',
      status: row.status || '1', offlineStatus: row.offlineStatus || '0', sort: row.sort ?? 0,
      factory: row.factory || '', supplier: row.supplier || '',
      remarks: row.remarks || ''
    })
    originalOfflineStatus.value = row.offlineStatus || '0'
  }
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function buildExtraParams() {
  const ext = {}
  const liftMap = {
    ratedWeight: form.ratedWeight, basicHeight: form.basicHeight,
    ascendingSpeed: form.ascendingSpeed, ratedLoad: form.ratedLoad,
    cabinSize: form.cabinSize, maxAscendingHeight: form.maxAscendingHeight
  }
  const towerMap = {
    forwardArmLength: form.forwardArmLength, backwardArmLength: form.backwardArmLength,
    ratedLoad: form.ratedLoad, maxLoad: form.maxLoad,
    maxHeight: form.maxHeight, ratedMoment: form.ratedMoment
  }
  Object.entries({ ...liftMap, ...towerMap }).forEach(([k, v]) => {
    if (v !== null && v !== undefined && v !== '') ext[k] = v
  })
  return Object.keys(ext).length ? JSON.stringify(ext) : undefined
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 编辑时由在线改为离线，需二次确认；新增或本就是离线则不弹
  if (form.offlineStatus === '0' && originalOfflineStatus.value === '1') {
    try {
      await ElMessageBox.confirm(
        '该设备将被标记为「离线」，确定保存吗？',
        '状态变更确认',
        { confirmButtonText: '确定保存', cancelButtonText: '取消', type: 'warning' }
      )
    } catch (e) {
      return
    }
  }

  formLoading.value = true
  try {
    const payload = {
      assetId: form.assetId,
      assetName: form.assetName,
      assetCode: form.assetCode,
      typeId: form.typeId,
      locationId: form.locationId,
      brand: form.brand,
      specification: form.specification,
      model: form.model,
      status: '1',
      offlineStatus: form.offlineStatus,
      sort: form.sort,
      factory: form.factory,
      supplier: form.supplier,
      remarks: form.remarks,
      extraParams: buildExtraParams()
    }
    await saveEquipmentAssets(payload)
    ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    formLoading.value = false
  }
}

// ---- 启用/禁用 ----
async function toggleEnable(row) {
  const next = row.openStatus === '1' ? '0' : '1'
  try {
    await saveEquipmentAssets({
      assetId: row.assetId,
      assetCode: row.assetCode,
      assetName: row.assetName,
      typeId: row.typeId,
      platformEqCode: row.platformEqCode,
      openStatus: next
    })
    row.openStatus = next
    ElMessage.success(next === '1' ? '已启用' : '已禁用')
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// ---- 删除 ----
function handleDelete(row) {
  ElMessageBox.confirm(
    `确定要删除设备「${row.assetName}」吗？删除后不可恢复。`,
    '删除确认',
    { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      await deleteEquipmentAssets(row.assetId)
      ElMessage.success('删除成功')
      await fetchList()
    } catch (e) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
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

onMounted(() => {
  fetchList()
  fetchOptions()
})
</script>

<template>
  <div class="equipment-assets-page">
    <div class="page-header">
      <h2>设备管理</h2>
      <div class="header-actions">
        <button class="btn btn-primary" @click="openDialog('add')">
          <svg viewBox="0 0 24 24" fill="none" width="14" height="14"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
          新增设备
        </button>
      </div>
    </div>

    <div class="body-row">
      <!-- 左侧：设备类型树筛选 -->
      <div class="type-panel">
        <div class="panel-header">
          <span class="panel-title">设备类型</span>
        </div>
        <div class="type-head-actions">
          <button class="btn btn-ghost" @click="selectAllType">全部设备</button>
        </div>
        <div class="type-tree">
          <div v-if="flatTypeTree.length === 0" class="tree-empty">暂无类型数据</div>
          <div
            v-for="node in flatTypeTree"
            :key="node.typeId || node.id"
            class="type-node"
            :class="{ 'type-node-selected': selectedTypeId === (node.typeId || node.id) }"
            :style="{ paddingLeft: (node._depth * 20 + 10) + 'px' }"
            @click.stop="handleTypeNodeClick(node)"
          >
            <span
              class="type-node-arrow"
              :class="{ expanded: node._expanded, invisible: !hasTypeChildren(node) }"
              @click.stop="toggleTypeExpand(node)"
            >
              <svg viewBox="0 0 24 24" fill="none" width="14" height="14">
                <path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </span>
            <span class="type-node-label">{{ node.typeName || node.label }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧：设备列表 -->
      <div class="table-panel">
        <div class="table-toolbar">
          <span v-if="selectedTypeId != null" class="filter-hint">
          已筛选：{{ selectedFilterTypeName || '该类型' }}
          <a @click="selectAllType">清除</a>
        </span>
          <span v-else class="filter-hint">全部设备</span>
        </div>
        <el-table :data="displayList" v-loading="loading" stripe style="width: 100%">
          <el-table-column type="index" label="序号" width="64" align="center" />
          <el-table-column prop="assetName" label="设备名称" min-width="120" />
          <el-table-column prop="assetCode" label="设备编号" min-width="110" />
          <el-table-column prop="locationName" label="设备位置" min-width="120" />
          <el-table-column prop="typeName" label="设备类型" min-width="100" />
          <el-table-column prop="sort" label="排序" width="70" align="center" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <span class="tag" :class="row.offlineStatus === '1' ? 'tag-green' : 'tag-red'">
                {{ row.offlineStatus === '1' ? '在线' : '离线' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="启用/禁用" width="100" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.openStatus === '1'"
                active-color="#0d9e6c"
                inactive-color="#c4c7cf"
                @change="toggleEnable(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="190" align="center">
            <template #default="{ row }">
              <el-button type="warning" link @click="openDialog('edit', row)">对接配置</el-button>
              <el-button type="primary" link @click="openDialog('edit', row)">编辑</el-button>
              <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="selectedTypeId != null ? displayList.length : total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @current-change="onPageChange"
            @size-change="onSizeChange"
          />
        </div>
      </div>
    </div>

    <!-- 新增/编辑模态框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增设备' : '编辑设备'"
      width="760px"
      destroy-on-close
      top="4vh"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" label-position="right" size="default">
        <div class="form-section-title">基本信息</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="设备名称" prop="assetName">
              <el-input v-model="form.assetName" placeholder="请输入设备名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备编号" prop="assetCode">
              <el-input v-model="form.assetCode" placeholder="请输入设备编号" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备类型" prop="typeId">
              <el-select v-model="form.typeId" placeholder="请选择设备类型" style="width: 100%" clearable>
                <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所在位置">
              <el-select v-model="form.locationId" placeholder="请选择所在位置" style="width: 100%" clearable>
                <el-option v-for="opt in locationOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌">
              <el-input v-model="form.brand" placeholder="请输入品牌" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规格">
              <el-input v-model="form.specification" placeholder="请输入规格" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="型号">
              <el-input v-model="form.model" placeholder="请输入型号" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.offlineStatus">
                <el-radio label="1">在线</el-radio>
                <el-radio label="0">离线</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <div v-if="showLiftExt" class="form-section-title">升降机扩展信息</div>
        <el-row v-if="showLiftExt" :gutter="16">
          <el-col :span="8">
            <el-form-item label="额定重量"><el-input-number v-model="form.ratedWeight" :min="0" controls-position="right" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="基本高度"><el-input-number v-model="form.basicHeight" :min="0" controls-position="right" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="起升速度"><el-input-number v-model="form.ascendingSpeed" :min="0" controls-position="right" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="额定载重(人)"><el-input-number v-model="form.ratedLoad" :min="0" controls-position="right" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="吊笼尺寸"><el-input v-model="form.cabinSize" placeholder="如 3.2×1.5×2.2m" maxlength="50" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大起升高度"><el-input-number v-model="form.maxAscendingHeight" :min="0" controls-position="right" style="width: 100%" /></el-form-item>
          </el-col>
        </el-row>

        <div v-if="showTowerExt" class="form-section-title">塔吊扩展信息</div>
        <el-row v-if="showTowerExt" :gutter="16">
          <el-col :span="8">
            <el-form-item label="前向臂长"><el-input-number v-model="form.forwardArmLength" :min="0" controls-position="right" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="后向臂长"><el-input-number v-model="form.backwardArmLength" :min="0" controls-position="right" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="额定吊重"><el-input-number v-model="form.ratedLoad" :min="0" controls-position="right" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大吊重"><el-input-number v-model="form.maxLoad" :min="0" controls-position="right" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大吊高"><el-input-number v-model="form.maxHeight" :min="0" controls-position="right" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="额定力矩"><el-input-number v-model="form.ratedMoment" :min="0" controls-position="right" style="width: 100%" /></el-form-item>
          </el-col>
        </el-row>

        <div class="form-section-title">生产与供应商信息</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="生产厂家"><el-input v-model="form.factory" placeholder="请输入生产厂家" maxlength="50" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出厂日期"><el-date-picker v-model="form.productionDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商"><el-input v-model="form.supplier" placeholder="请输入供应商" maxlength="50" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供货日期"><el-date-picker v-model="form.supplyDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
          </el-col>
        </el-row>

        <div class="form-section-title">日期信息</div>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="验收日期"><el-date-picker v-model="form.acceptanceDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="安装日期"><el-date-picker v-model="form.installDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="启动日期"><el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="报废预期"><el-date-picker v-model="form.scrapPre" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="保修期限(月)"><el-input-number v-model="form.serviceTerm" :min="0" controls-position="right" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="保修截止"><el-date-picker v-model="form.serviceDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
          </el-col>
        </el-row>

        <div class="form-section-title">价值信息</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="设备原值"><el-input-number v-model="form.original" :min="0" controls-position="right" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="评估价值"><el-input-number v-model="form.originalPre" :min="0" controls-position="right" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注"><el-input v-model="form.remarks" type="textarea" :rows="2" maxlength="255" /></el-form-item>
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
.equipment-assets-page {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  overflow: hidden;
  background: var(--bg-page);
}

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
  padding: 5px 12px;
  font-size: 12px;
}
.btn-ghost:hover { color: var(--primary); border-color: var(--primary); }

.body-row {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 16px;
}

.type-panel {
  width: 260px;
  flex-shrink: 0;
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 14px;
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.panel-header {
  padding: 14px 16px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-soft);
}
.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  position: relative;
  padding-left: 14px;
}
.panel-title::before {
  content: '';
  position: absolute;
  left: 0; top: 50%; transform: translateY(-50%);
  width: 3px; height: 16px;
  background: var(--primary-grad);
  border-radius: 2px;
}
.type-head-actions { padding: 10px 12px 4px; }
.type-tree { flex: 1; overflow-y: auto; padding: 6px 8px 10px; }
.type-tree::-webkit-scrollbar { width: 4px; }
.type-tree::-webkit-scrollbar-thumb { background: rgba(176,123,62,.2); border-radius: 2px; }
.tree-empty { color: var(--text-faint); font-size: 13px; text-align: center; padding: 24px 0; }

.type-node {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 10px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-main);
  border: 1px solid transparent;
  transition: all .15s;
}
.type-node:hover { background: var(--bg-hover); }
.type-node-selected { background: var(--primary-tint) !important; border-color: rgba(176,123,62,.3); color: var(--primary-text); font-weight: 600; }
.type-node-arrow {
  width: 18px; height: 18px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-muted); transition: transform .2s; border-radius: 4px;
}
.type-node-arrow.expanded { transform: rotate(90deg); }
.type-node-arrow.invisible { visibility: hidden; }
.type-node-label { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.table-panel {
  flex: 1;
  min-width: 0;
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 14px;
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.table-toolbar {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}
.filter-hint { font-size: 13px; color: var(--text-secondary); }
.filter-hint a { color: var(--primary); cursor: pointer; margin-left: 6px; }

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding: 14px 18px;
  border-top: 1px solid var(--border);
  flex-shrink: 0;
}

.tag { display: inline-block; padding: 2px 10px; border-radius: 20px; font-size: 12px; }
.tag-green { color: #0d9e6c; background: rgba(13,158,108,.1); }
.tag-red { color: #e54545; background: rgba(229,69,69,.1); }

.form-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--primary-text);
  border-left: 3px solid var(--primary);
  padding-left: 10px;
  margin: 4px 0 14px;
}
:deep(.el-dialog__body) { max-height: 68vh; overflow-y: auto; }
</style>