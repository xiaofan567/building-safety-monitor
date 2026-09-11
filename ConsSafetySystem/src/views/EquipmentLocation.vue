<script setup>
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEquipmentLocationTree, saveEquipmentLocation, deleteEquipmentLocation } from '@/api/equipmentLocation'

const treeData = ref([])
const selectedNode = ref(null)
const formRef = ref(null)
const formLoading = ref(false)

const isEdit = computed(() => !!form.locationId)

const form = reactive({
  locationId: null,
  locationCode: '',
  locationName: '',
  locationPid: 0,
  sort: 0
})

const rules = {
  locationCode: [{ required: true, message: '请输入位置编码', trigger: 'blur' }],
  locationName: [{ required: true, message: '请输入位置名称', trigger: 'blur' }]
}

// 后端 queryTreeList 直接返回嵌套树，这里只需补充前端用到的默认展开状态
function normalize(nodes) {
  if (!Array.isArray(nodes)) return []
  return nodes.map(node => ({
    ...node,
    _expanded: true,
    children: node.children && node.children.length ? normalize(node.children) : undefined
  }))
}

const flatTreeList = computed(() => {
  const result = []
  function flatten(nodes, depth = 0) {
    if (!nodes) return
    nodes.forEach(node => {
      // 直接推入真实节点引用（非浅拷贝），让 toggleExpand 能修改原节点状态
      node._depth = depth
      result.push(node)
      if (node.children && node.children.length > 0 && node._expanded) {
        flatten(node.children, depth + 1)
      }
    })
  }
  flatten(treeData.value)
  return result
})

async function fetchTree() {
  try {
    const res = await getEquipmentLocationTree()
    const list = res?.data || []
    if (Array.isArray(list) && list.length > 0) {
      treeData.value = normalize(list)
      if (!selectedNode.value) {
        autoSelectFirst()
      }
    } else {
      treeData.value = []
    }
  } catch (e) {
    treeData.value = []
  }
}

function autoSelectFirst() {
  const list = flatTreeList.value
  if (list.length > 0) {
    handleNodeClick(list[0])
  }
}

function handleNodeClick(data) {
  selectedNode.value = data
  form.locationId = data.locationId || data.id
  form.locationCode = data.locationCode || ''
  form.locationName = data.locationName || data.label || ''
  form.locationPid = data.locationPid !== undefined ? data.locationPid : 0
  form.sort = data.sort !== undefined ? data.sort : 0
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

function toggleExpand(node) {
  node._expanded = !node._expanded
}

function isSelected(node) {
  return selectedNode.value && (selectedNode.value.locationId === node.locationId || selectedNode.value.id === node.id)
}

function hasChildren(node) {
  return node.children && node.children.length > 0
}

function getParentName() {
  if (!form.locationPid || form.locationPid === 0) return '根节点'
  const list = flatTreeList.value
  const parent = list.find(n => (n.locationId || n.id) === form.locationPid)
  return parent ? (parent.locationName || parent.label) : '未知'
}

function handleAdd(parentNode) {
  form.locationId = null
  form.locationCode = ''
  form.locationName = ''
  form.locationPid = parentNode ? (parentNode.locationId || parentNode.id) : 0
  form.sort = 0
  selectedNode.value = null
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  formLoading.value = true
  try {
    const params = {
      locationCode: form.locationCode,
      locationName: form.locationName,
      locationPid: form.locationPid,
      sort: form.sort
    }
    // 后端 save 接口按是否传入 locationId 自动区分新增/编辑
    if (isEdit.value) {
      params.locationId = form.locationId
    }
    await saveEquipmentLocation(params)
    ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
    form.locationId = null
    form.locationCode = ''
    form.locationName = ''
    form.locationPid = 0
    form.sort = 0
    selectedNode.value = null
    await fetchTree()
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    formLoading.value = false
  }
}

function handleDelete() {
  if (!selectedNode.value) {
    ElMessage.warning('请先选择要删除的位置')
    return
  }
  if (hasChildren(selectedNode.value)) {
    ElMessage.warning('该位置存在子节点，请先删除子节点')
    return
  }
  ElMessageBox.confirm(
    `确定要删除位置「${selectedNode.value.locationName || selectedNode.value.label}」吗？删除后不可恢复。`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteEquipmentLocation(selectedNode.value.locationId || selectedNode.value.id)
      ElMessage.success('删除成功')
      form.locationId = null
      form.locationCode = ''
      form.locationName = ''
      form.locationPid = 0
      form.sort = 0
      selectedNode.value = null
      await fetchTree()
    } catch (e) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

function handleReset() {
  if (selectedNode.value) {
    handleNodeClick(selectedNode.value)
  } else {
    form.locationId = null
    form.locationCode = ''
    form.locationName = ''
    form.locationPid = 0
    form.sort = 0
    formRef.value?.clearValidate()
  }
}

onMounted(() => {
  fetchTree()
})
</script>

<template>
  <div class="equipment-location-page">
    <div class="page-header">
      <h2>设备位置管理</h2>
      <div class="header-actions">
        <button class="btn btn-primary" @click="handleAdd(null)">
          <svg viewBox="0 0 24 24" fill="none" width="14" height="14"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
          新增根位置
        </button>
      </div>
    </div>

    <div class="page-body">
      <div class="left-panel">
        <div class="panel-header">
          <span class="panel-title">位置树形结构</span>
          <span class="panel-count">{{ treeData.length }} 个根节点</span>
        </div>
        <div class="tree-container">
          <div v-if="treeData.length === 0" class="tree-empty">
            <svg viewBox="0 0 24 24" fill="none" width="40" height="40"><path d="M12 2L2 7l10 5 10-5L12 2Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/><path d="M2 17l10 5 10-5" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/><path d="M2 12l10 5 10-5" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>
            <p>暂无设备位置数据</p>
            <p class="sub">点击上方按钮新增根位置</p>
          </div>
          <div v-else class="tree-list">
            <div
              v-for="node in flatTreeList"
              :key="node.locationId || node.id"
              class="tree-node-wrapper"
            >
              <div
                class="tree-node"
                :class="{ 'tree-node-selected': isSelected(node) }"
                :style="{ paddingLeft: (node._depth * 22 + 10) + 'px' }"
                @click.stop="handleNodeClick(node)"
              >
                <span
                  class="tree-node-arrow"
                  :class="{ expanded: node._expanded, invisible: !hasChildren(node) }"
                  @click.stop="toggleExpand(node)"
                >
                  <svg viewBox="0 0 24 24" fill="none" width="14" height="14">
                    <path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                  </svg>
                </span>
                <span class="tree-node-icon">
                  <svg v-if="hasChildren(node)" viewBox="0 0 24 24" fill="none" width="16" height="16">
                    <path d="M3 3h7v7H3V3zM14 3h7v7h-7V3zM3 14h7v7H3v-7zM14 14h7v7h-7v-7z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/>
                  </svg>
                  <svg v-else viewBox="0 0 24 24" fill="none" width="16" height="16">
                    <path d="M12 2C8.1 2 5 5.1 5 9c0 5.3 7 13 7 13s7-7.7 7-13c0-3.9-3.1-7-7-7Zm0 9.5A2.5 2.5 0 1 1 12 6.5a2.5 2.5 0 0 1 0 5Z" stroke="currentColor" stroke-width="1.5"/>
                  </svg>
                </span>
                <span class="tree-node-label">{{ node.locationName || node.label }}</span>
                <span class="tree-node-code">{{ node.locationCode }}</span>
                <span class="tree-node-actions" @click.stop>
                  <button class="tree-action-btn" title="添加子位置" @click="handleAdd(node)">
                    <svg viewBox="0 0 24 24" fill="none" width="14" height="14"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
                  </button>
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="right-panel">
        <div class="panel-header">
          <span class="panel-title">{{ isEdit ? '编辑位置' : '新增位置' }}</span>
          <span v-if="selectedNode" class="panel-hint">当前选中：{{ selectedNode.locationName || selectedNode.label }}</span>
        </div>
        <div class="form-container">
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="100px"
            label-position="right"
            size="default"
          >
            <el-form-item label="上级位置">
              <div class="parent-display">{{ getParentName() }}</div>
            </el-form-item>

            <el-form-item label="位置编码" prop="locationCode">
              <el-input
                v-model="form.locationCode"
                placeholder="请输入位置编码"
                maxlength="50"
                clearable
              />
            </el-form-item>

            <el-form-item label="位置名称" prop="locationName">
              <el-input
                v-model="form.locationName"
                placeholder="请输入位置名称"
                maxlength="100"
                clearable
              />
            </el-form-item>

            <el-form-item label="排序">
              <el-input-number
                v-model="form.sort"
                :min="0"
                :max="9999"
                placeholder="排序号"
                controls-position="right"
                style="width: 160px"
              />
            </el-form-item>

            <el-form-item>
              <div class="form-actions">
                <el-button type="primary" :loading="formLoading" @click="handleSave">
                  {{ isEdit ? '保存修改' : '确认新增' }}
                </el-button>
                <el-button v-if="isEdit" @click="handleReset">重置</el-button>
                <el-button
                  v-if="isEdit && selectedNode"
                  type="danger"
                  plain
                  @click="handleDelete"
                >
                  删除当前位置
                </el-button>
              </div>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.equipment-location-page {
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

.header-actions {
  display: flex;
  gap: 10px;
}

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
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(176, 123, 62, 0.22);
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(176, 123, 62, 0.3);
}

.page-body {
  flex: 1;
  display: flex;
  gap: 16px;
  overflow: hidden;
  min-height: 0;
}

.left-panel {
  width: 320px;
  flex-shrink: 0;
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.right-panel {
  flex: 1;
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-soft);
  flex-shrink: 0;
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
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  background: var(--primary-grad);
  border-radius: 2px;
}

.panel-count {
  font-size: 11px;
  color: var(--text-muted);
}

.panel-hint {
  font-size: 12px;
  color: var(--primary-text);
}

.tree-container {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.tree-container::-webkit-scrollbar {
  width: 4px;
}

.tree-container::-webkit-scrollbar-thumb {
  background: rgba(176, 123, 62, 0.2);
  border-radius: 2px;
}

.tree-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-faint);
}

.tree-empty p {
  margin: 8px 0 0;
  font-size: 13px;
}

.tree-empty .sub {
  font-size: 11px;
  color: var(--text-muted);
}

.tree-node-wrapper {
  user-select: none;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  border: 1px solid transparent;
}

.tree-node:hover {
  background: var(--bg-hover);
}

.tree-node-selected {
  background: var(--primary-tint) !important;
  border-color: rgba(176, 123, 62, 0.3);
}

.tree-node-arrow {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.2s;
  color: var(--text-muted);
  border-radius: 4px;
}

.tree-node-arrow:hover {
  background: rgba(176, 123, 62, 0.1);
  color: var(--text-secondary);
}

.tree-node-arrow.expanded {
  transform: rotate(90deg);
}

.tree-node-arrow.invisible {
  visibility: hidden;
}

.tree-node-icon {
  color: var(--text-muted);
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.tree-node-selected .tree-node-icon {
  color: var(--primary);
}

.tree-node-label {
  flex: 1;
  font-size: 13px;
  color: var(--text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-node-selected .tree-node-label {
  color: var(--primary-text);
  font-weight: 600;
}

.tree-node-code {
  font-size: 10px;
  color: var(--text-secondary);
  background: var(--bg-muted);
  padding: 1px 6px;
  border-radius: 4px;
  flex-shrink: 0;
}

.tree-node-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s;
}

.tree-node:hover .tree-node-actions {
  opacity: 1;
}

.tree-action-btn {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: 5px;
  padding: 0;
}

.tree-action-btn:hover {
  background: var(--primary-tint);
  color: var(--primary);
}

.form-container {
  flex: 1;
  padding: 24px 30px;
  overflow-y: auto;
}

.form-container::-webkit-scrollbar {
  width: 4px;
}

.form-container::-webkit-scrollbar-thumb {
  background: rgba(176, 123, 62, 0.2);
  border-radius: 2px;
}

.parent-display {
  font-size: 13px;
  color: var(--text-secondary);
  background: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 8px 12px;
  line-height: 1.4;
}

.form-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

:deep(.el-form-item__label) {
  color: var(--text-secondary);
  font-size: 13px;
}

:deep(.el-input__wrapper) {
  background: var(--bg-soft);
  border-color: var(--border);
  box-shadow: none;
  border-radius: 8px;
}

:deep(.el-input__wrapper:hover) {
  border-color: var(--border-strong);
}

:deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary);
  box-shadow: 0 0 0 1px var(--primary) inset, 0 0 0 3px rgba(176, 123, 62, 0.12);
}

:deep(.el-input__inner) {
  color: var(--text-main);
}

:deep(.el-input-number) {
  background: var(--bg-soft);
  border-color: var(--border);
  border-radius: 8px;
}

:deep(.el-input-number .el-input__inner) {
  background: var(--bg-soft);
  color: var(--text-main);
}

:deep(.el-button--primary) {
  --el-button-bg-color: var(--el-color-primary);
  --el-button-border-color: var(--el-color-primary);
}

:deep(.el-button--primary:hover) {
  --el-button-bg-color: var(--el-color-primary-dark-2);
  --el-button-border-color: var(--el-color-primary-dark-2);
}

:deep(.el-button--danger.is-plain) {
  --el-button-bg-color: transparent;
  --el-button-border-color: rgba(194, 91, 91, 0.4);
  --el-button-text-color: var(--danger);
}

:deep(.el-button) {
  --el-button-bg-color: var(--bg-white);
  --el-button-border-color: var(--border-strong);
  --el-button-text-color: var(--text-secondary);
}

:deep(.el-button:hover) {
  --el-button-border-color: var(--primary);
  --el-button-text-color: var(--primary);
}
</style>