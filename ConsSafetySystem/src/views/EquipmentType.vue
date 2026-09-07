<script setup>
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEquipmentTypeTree, saveEquipmentType, updateEquipmentType, deleteEquipmentType } from '@/api/equipmentType'

const treeData = ref([])
const selectedNode = ref(null)
const formRef = ref(null)
const formLoading = ref(false)

const isEdit = computed(() => !!form.typeId)

const form = reactive({
  typeId: null,
  typeCode: '',
  typeName: '',
  typePid: 0,
  sort: 0
})

const rules = {
  typeCode: [{ required: true, message: '请输入类型编号', trigger: 'blur' }],
  typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }]
}

function buildTree(list, pid = 0) {
  const result = []
  list.forEach(item => {
    if (item.typePid === pid) {
      const children = buildTree(list, item.typeId)
      const node = {
        _expanded: true,
        ...item,
        typePid: item.typePid,
        id: item.typeId
      }
      if (children.length > 0) {
        node.children = children
      }
      result.push(node)
    }
  })
  return result
}

const flatTreeList = computed(() => {
  const result = []
  function flatten(nodes, depth = 0) {
    if (!nodes) return
    nodes.forEach(node => {
      result.push({ ...node, _depth: depth })
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
    const res = await getEquipmentTypeTree()
    const list = res?.data || []
    if (Array.isArray(list) && list.length > 0) {
      treeData.value = buildTree(list)
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
  form.typeId = data.typeId || data.id
  form.typeCode = data.typeCode || ''
  form.typeName = data.typeName || data.label || ''
  form.typePid = data.typePid !== undefined ? data.typePid : 0
  form.sort = data.sort !== undefined ? data.sort : 0
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

function toggleExpand(node) {
  node._expanded = !node._expanded
}

function isSelected(node) {
  return selectedNode.value && (selectedNode.value.typeId === node.typeId || selectedNode.value.id === node.id)
}

function hasChildren(node) {
  return node.children && node.children.length > 0
}

function getParentName() {
  if (!form.typePid || form.typePid === 0) return '根节点'
  const list = flatTreeList.value
  const parent = list.find(n => (n.typeId || n.id) === form.typePid)
  return parent ? (parent.typeName || parent.label) : '未知'
}

function handleAdd(parentNode) {
  form.typeId = null
  form.typeCode = ''
  form.typeName = ''
  form.typePid = parentNode ? (parentNode.typeId || parentNode.id) : 0
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
      typeCode: form.typeCode,
      typeName: form.typeName,
      typePid: form.typePid,
      sort: form.sort
    }
    if (isEdit.value) {
      params.typeId = form.typeId
      await updateEquipmentType(params)
      ElMessage.success('编辑成功')
    } else {
      await saveEquipmentType(params)
      ElMessage.success('新增成功')
    }
    form.typeId = null
    form.typeCode = ''
    form.typeName = ''
    form.typePid = 0
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
    ElMessage.warning('请先选择要删除的类型')
    return
  }
  if (hasChildren(selectedNode.value)) {
    ElMessage.warning('该类型存在子节点，无法删除，请先删除子节点')
    return
  }
  ElMessageBox.confirm(
    `确定要删除类型「${selectedNode.value.typeName || selectedNode.value.label}」吗？删除后不可恢复。`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteEquipmentType(selectedNode.value.typeId || selectedNode.value.id)
      ElMessage.success('删除成功')
      form.typeId = null
      form.typeCode = ''
      form.typeName = ''
      form.typePid = 0
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
    form.typeId = null
    form.typeCode = ''
    form.typeName = ''
    form.typePid = 0
    form.sort = 0
    formRef.value?.clearValidate()
  }
}

onMounted(() => {
  fetchTree()
})
</script>

<template>
  <div class="equipment-type-page">
    <div class="page-header">
      <h2>设备类型管理</h2>
      <div class="header-actions">
        <button class="btn btn-primary" @click="handleAdd(null)">
          <svg viewBox="0 0 24 24" fill="none" width="14" height="14"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
          新增根类型
        </button>
      </div>
    </div>

    <div class="page-body">
      <div class="left-panel">
        <div class="panel-header">
          <span class="panel-title">类型树形结构</span>
          <span class="panel-count">{{ treeData.length }} 个根节点</span>
        </div>
        <div class="tree-container">
          <div v-if="treeData.length === 0" class="tree-empty">
            <svg viewBox="0 0 24 24" fill="none" width="40" height="40"><path d="M13 2L3 7v10l10 5 10-5V7L13 2z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>
            <p>暂无设备类型数据</p>
            <p class="sub">点击上方按钮新增根类型</p>
          </div>
          <div v-else class="tree-list">
            <div
              v-for="node in flatTreeList"
              :key="node.typeId || node.id"
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
                    <path d="M4 5h16M4 12h16M4 19h12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                  </svg>
                </span>
                <span class="tree-node-label">{{ node.typeName || node.label }}</span>
                <span class="tree-node-code">{{ node.typeCode }}</span>
                <span class="tree-node-actions" @click.stop>
                  <button class="tree-action-btn" title="添加子节点" @click="handleAdd(node)">
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
          <span class="panel-title">{{ isEdit ? '编辑类型' : '新增类型' }}</span>
          <span v-if="selectedNode" class="panel-hint">当前选中：{{ selectedNode.typeName || selectedNode.label }}</span>
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
            <el-form-item label="上级类型">
              <div class="parent-display">{{ getParentName() }}</div>
            </el-form-item>

            <el-form-item label="类型编号" prop="typeCode">
              <el-input
                v-model="form.typeCode"
                placeholder="请输入类型编号"
                maxlength="50"
                clearable
              />
            </el-form-item>

            <el-form-item label="类型名称" prop="typeName">
              <el-input
                v-model="form.typeName"
                placeholder="请输入类型名称"
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
                  删除当前类型
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
.equipment-type-page {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  overflow: hidden;
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
  font-size: 18px;
  font-weight: 700;
  color: #e8edf2;
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
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.btn-primary {
  background: #1a73e8;
  color: #fff;
  border-color: #1a73e8;
}

.btn-primary:hover {
  background: #1557b0;
  border-color: #1557b0;
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
  background: #0f1d2e;
  border: 1px solid rgba(26, 50, 72, 0.5);
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.right-panel {
  flex: 1;
  background: #0f1d2e;
  border: 1px solid rgba(26, 50, 72, 0.5);
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid rgba(26, 50, 72, 0.5);
  flex-shrink: 0;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #e8edf2;
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
  background: linear-gradient(180deg, #1a73e8, #0d9e6c);
  border-radius: 2px;
}

.panel-count {
  font-size: 11px;
  color: #5a6a7a;
}

.panel-hint {
  font-size: 12px;
  color: #5a8fd4;
}

.tree-container {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.tree-container::-webkit-scrollbar {
  width: 3px;
}

.tree-container::-webkit-scrollbar-thumb {
  background: rgba(26, 115, 232, 0.15);
  border-radius: 2px;
}

.tree-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #3a5068;
}

.tree-empty p {
  margin: 8px 0 0;
  font-size: 13px;
}

.tree-empty .sub {
  font-size: 11px;
  color: #5a6a7a;
}

.tree-node-wrapper {
  user-select: none;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  position: relative;
}

.tree-node:hover {
  background: rgba(26, 115, 232, 0.08);
}

.tree-node-selected {
  background: rgba(26, 115, 232, 0.15) !important;
  border: 1px solid rgba(26, 115, 232, 0.3);
}

.tree-node-arrow {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.2s;
  color: #5a6a7a;
  border-radius: 3px;
}

.tree-node-arrow:hover {
  background: rgba(26, 115, 232, 0.1);
  color: #8899aa;
}

.tree-node-arrow.expanded {
  transform: rotate(90deg);
}

.tree-node-arrow.invisible {
  visibility: hidden;
}

.tree-node-icon {
  color: #5a6a7a;
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.tree-node-selected .tree-node-icon {
  color: #1a73e8;
}

.tree-node-label {
  flex: 1;
  font-size: 13px;
  color: #bcc8d4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-node-selected .tree-node-label {
  color: #e8edf2;
  font-weight: 500;
}

.tree-node-code {
  font-size: 10px;
  color: #3a5068;
  background: rgba(26, 50, 72, 0.4);
  padding: 1px 6px;
  border-radius: 3px;
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
  color: #5a6a7a;
  cursor: pointer;
  border-radius: 4px;
  padding: 0;
}

.tree-action-btn:hover {
  background: rgba(26, 115, 232, 0.15);
  color: #1a73e8;
}

.form-container {
  flex: 1;
  padding: 24px 30px;
  overflow-y: auto;
}

.form-container::-webkit-scrollbar {
  width: 3px;
}

.form-container::-webkit-scrollbar-thumb {
  background: rgba(26, 115, 232, 0.15);
  border-radius: 2px;
}

.parent-display {
  font-size: 13px;
  color: #bcc8d4;
  background: #0a1625;
  border: 1px solid rgba(26, 50, 72, 0.4);
  border-radius: 6px;
  padding: 8px 12px;
  line-height: 1.4;
}

.form-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

:deep(.el-form-item__label) {
  color: #8899aa;
  font-size: 13px;
}

:deep(.el-input__wrapper) {
  background: #0a1625;
  border-color: rgba(26, 50, 72, 0.4);
  box-shadow: none;
}

:deep(.el-input__wrapper:hover) {
  border-color: rgba(26, 115, 232, 0.3);
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #1a73e8;
  box-shadow: 0 0 0 1px rgba(26, 115, 232, 0.2);
}

:deep(.el-input__inner) {
  color: #bcc8d4;
}

:deep(.el-input-number) {
  background: #0a1625;
  border-color: rgba(26, 50, 72, 0.4);
}

:deep(.el-input-number .el-input__inner) {
  background: #0a1625;
  color: #bcc8d4;
}

:deep(.el-button--primary) {
  --el-button-bg-color: #1a73e8;
  --el-button-border-color: #1a73e8;
}

:deep(.el-button--primary:hover) {
  --el-button-bg-color: #1557b0;
  --el-button-border-color: #1557b0;
}

:deep(.el-button--danger.is-plain) {
  --el-button-bg-color: transparent;
  --el-button-border-color: rgba(229, 69, 69, 0.4);
  --el-button-text-color: #e54545;
}

:deep(.el-button) {
  --el-button-bg-color: #0a1625;
  --el-button-border-color: rgba(26, 50, 72, 0.4);
  --el-button-text-color: #8899aa;
}

:deep(.el-button:hover) {
  --el-button-border-color: rgba(26, 115, 232, 0.3);
  --el-button-text-color: #bcc8d4;
}
</style>