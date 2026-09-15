<template>
  <div class="risk-settings">
    <div class="breadcrumb">
      <span class="crumb-tag">施工场地</span>
      <span class="crumb-sep">/</span>
      <span class="crumb-current">风险识别设置</span>
    </div>

    <!-- 筛选条件区 -->
    <div class="filter-card">
      <div class="filter-form">
        <div class="form-item">
          <label class="form-label">设备名称</label>
          <input
            v-model="filters.assetName"
            class="form-input"
            type="text"
            placeholder="请输入设备名称"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="form-item">
          <label class="form-label">设备编号</label>
          <input
            v-model="filters.assetCode"
            class="form-input"
            type="text"
            placeholder="请输入设备编号"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="form-actions">
          <button class="btn btn-primary" @click="handleSearch">搜索</button>
          <button class="btn btn-plain" @click="handleReset">重置</button>
        </div>
      </div>
    </div>

    <!-- 数据表格区 -->
    <div class="table-card">
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>序号</th>
              <th>设备名称</th>
              <th>设备编号</th>
              <th>所在位置</th>
              <th>设备状态</th>
              <th>启用状态</th>
              <th>AI识别类别</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in tableData" :key="row.id">
              <td>{{ (pagination.currentPage - 1) * pagination.pageSize + index + 1 }}</td>
              <td>{{ row.assetName || '--' }}</td>
              <td>{{ row.assetCode || '--' }}</td>
              <td>{{ row.locationName || '--' }}</td>
              <td>
                <span class="tag" :class="row.status === '1' ? 'tag-green' : 'tag-red'">
                  {{ row.status === '1' ? '在线' : '离线' }}
                </span>
              </td>
              <td>
                <span class="tag" :class="row.isEnable === '1' ? 'tag-blue' : 'tag-gray'">
                  {{ row.isEnable === '1' ? '启用' : '禁用' }}
                </span>
              </td>
              <td>
                <div class="type-tags">
                  <span
                    v-for="t in typeTags(row.aiMonitorType)"
                    :key="t"
                    class="type-tag"
                  >{{ t }}</span>
                  <span v-if="!row.aiMonitorType" class="text-dim">--</span>
                </div>
              </td>
              <td>
                <button class="btn btn-edit" @click="openEdit(row)">修改</button>
              </td>
            </tr>
            <tr v-if="!tableData.length">
              <td colspan="8" class="empty-cell">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页区 -->
      <div class="pagination">
        <span class="page-info">共 {{ pagination.total }} 条记录</span>
        <div class="page-actions">
          <button class="page-btn" :disabled="pagination.currentPage <= 1" @click="goToPage(1)">«</button>
          <button class="page-btn" :disabled="pagination.currentPage <= 1" @click="goToPage(pagination.currentPage - 1)">‹</button>
          <span v-for="p in pageNumbers" :key="p" class="page-num" :class="{ active: p === pagination.currentPage }" @click="goToPage(p)">{{ p }}</span>
          <button class="page-btn" :disabled="pagination.currentPage >= pagination.totalPages" @click="goToPage(pagination.currentPage + 1)">›</button>
          <button class="page-btn" :disabled="pagination.currentPage >= pagination.totalPages" @click="goToPage(pagination.totalPages)">»</button>
        </div>
      </div>
    </div>

    <!-- 编辑弹框 -->
    <div v-if="showEditModal" class="modal-mask" @click.self="showEditModal = false">
      <div class="modal-box">
        <div class="modal-head">
          <h3>修改设备</h3>
          <button class="modal-close" @click="showEditModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="modal-item">
            <label class="form-label">设备名称</label>
            <div class="modal-static">{{ editForm.assetName || '--' }}</div>
          </div>
          <div class="modal-item">
            <label class="form-label">设备编号</label>
            <div class="modal-static">{{ editForm.assetCode || '--' }}</div>
          </div>
          <div class="modal-item">
            <label class="form-label"><span class="required">*</span>AI识别类别</label>
            <div class="type-check-wrap">
              <label v-for="opt in aiTypeOptions" :key="opt.value" class="type-check">
                <input
                  type="checkbox"
                  :value="opt.value"
                  v-model="editForm.aiMonitorTypeArr"
                />
                <span>{{ opt.label }}</span>
              </label>
            </div>
          </div>
          <div class="modal-item">
            <label class="form-label"><span class="required">*</span>设备状态</label>
            <div class="radio-wrap">
              <label class="radio-item"><input type="radio" value="1" v-model="editForm.status" /><span>在线</span></label>
              <label class="radio-item"><input type="radio" value="0" v-model="editForm.status" /><span>离线</span></label>
            </div>
          </div>
          <div class="modal-item">
            <label class="form-label"><span class="required">*</span>启用状态</label>
            <div class="radio-wrap">
              <label class="radio-item"><input type="radio" value="1" v-model="editForm.isEnable" /><span>启用</span></label>
              <label class="radio-item"><input type="radio" value="0" v-model="editForm.isEnable" /><span>禁用</span></label>
            </div>
          </div>
          <p v-if="formError" class="form-error">{{ formError }}</p>
        </div>
        <div class="modal-foot">
          <button class="btn btn-plain" @click="showEditModal = false">取消</button>
          <button class="btn btn-primary" @click="handleSubmit">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { selectVideoData, updateVideoData } from '@/api/video'

// AI识别类别（aiMonitorType 字典）
const aiTypeOptions = [
  { value: '1', label: '未带安全帽' },
  { value: '2', label: '未穿安全衣' },
  { value: '3', label: '吸烟' },
  { value: '4', label: '夜间施工' },
  { value: '5', label: '明烟' },
  { value: '6', label: '明火' }
]
const typeLabelMap = aiTypeOptions.reduce((acc, o) => { acc[o.value] = o.label; return acc }, {})

const filters = ref({ assetName: '', assetCode: '' })
const tableData = ref([])
const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0, totalPages: 1 })

const showEditModal = ref(false)
const editForm = reactive({ id: null, assetName: '', assetCode: '', aiMonitorType: '', aiMonitorTypeArr: [], status: '0', isEnable: '1' })
const formError = ref('')

async function loadData() {
  try {
    const params = { ...filters.value, page: pagination.currentPage, limit: pagination.pageSize }
    const res = await selectVideoData(params)
    const data = (res && res.data) || {}
    tableData.value = data.records || []
    pagination.total = data.total || 0
    pagination.totalPages = data.pages || Math.ceil(pagination.total / pagination.pageSize) || 1
  } catch (e) {
    console.error('风险识别设备数据加载失败', e)
    tableData.value = []
  }
}

function handleSearch() {
  pagination.currentPage = 1
  loadData()
}

function handleReset() {
  filters.value = { assetName: '', assetCode: '' }
  pagination.currentPage = 1
  loadData()
}

function goToPage(page) {
  if (page < 1 || page > pagination.totalPages || page === pagination.currentPage) return
  pagination.currentPage = page
  loadData()
}

const pageNumbers = computed(() => {
  const pages = pagination.totalPages
  const current = pagination.currentPage
  if (pages <= 7) return Array.from({ length: pages }, (_, i) => i + 1)
  const start = Math.max(1, Math.min(current - 3, pages - 6))
  return Array.from({ length: 7 }, (_, i) => start + i)
})

function typeTags(str) {
  return (str || '').split(',').filter(Boolean).map(v => typeLabelMap[v] || ('类型' + v))
}

function openEdit(row) {
  editForm.id = row.id
  editForm.assetName = row.assetName || ''
  editForm.assetCode = row.assetCode || ''
  editForm.aiMonitorType = row.aiMonitorType || ''
  editForm.aiMonitorTypeArr = editForm.aiMonitorType ? editForm.aiMonitorType.split(',') : []
  editForm.status = row.status || '0'
  editForm.isEnable = row.isEnable || '1'
  formError.value = ''
  showEditModal.value = true
}

async function handleSubmit() {
  if (!editForm.aiMonitorTypeArr.length) {
    formError.value = '请至少选择一项 AI 识别类别'
    return
  }
  formError.value = ''
  try {
    await updateVideoData({
      id: editForm.id,
      status: editForm.status,
      isEnable: editForm.isEnable,
      aiMonitorType: editForm.aiMonitorTypeArr.join(',')
    })
    showEditModal.value = false
    loadData()
  } catch (e) {
    formError.value = '保存失败，请重试'
    console.error('保存失败', e)
  }
}

onMounted(loadData)
</script>

<style scoped>
.risk-settings {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 20px;
  box-sizing: border-box;
  background: var(--bg-page);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  overflow-y: auto;
}
.breadcrumb { display: flex; align-items: center; gap: 6px; margin-bottom: 16px; color: var(--text-muted); font-size: 12px; }
.crumb-tag { color: var(--text-secondary); }
.crumb-sep { color: var(--text-faint); }
.crumb-current { color: var(--primary-text); font-weight: 500; }

.filter-card {
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
  padding: 16px 20px;
  margin-bottom: 16px;
}
.filter-form { display: flex; align-items: flex-end; flex-wrap: wrap; gap: 16px; }
.form-item { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 12px; color: var(--text-secondary); }
.form-input {
  width: 220px;
  height: 34px;
  padding: 0 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-field, #fff);
  font-size: 13px;
  color: var(--text-main);
  outline: none;
  transition: border .2s;
}
.form-input:focus { border-color: var(--primary); }
.form-actions { display: flex; gap: 10px; }

.btn { height: 34px; padding: 0 20px; border-radius: 8px; font-size: 13px; cursor: pointer; border: none; transition: opacity .2s; }
.btn:disabled { opacity: .5; cursor: not-allowed; }
.btn-primary { background: var(--primary-grad); color: #fff; }
.btn-primary:hover:not(:disabled) { opacity: .9; }
.btn-plain { background: var(--bg-soft); color: var(--text-secondary); border: 1px solid var(--border); }
.btn-plain:hover { color: var(--primary); border-color: var(--primary); }
.btn-edit { height: 28px; padding: 0 14px; font-size: 12px; border-radius: 6px; background: var(--primary-tint); color: var(--primary-text); border: 1px solid rgba(176,123,62,.3); }
.btn-edit:hover { background: var(--primary); color: #fff; }

.table-card {
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}
.table-wrap { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th {
  padding: 12px 14px;
  text-align: left;
  background: var(--bg-soft);
  color: var(--text-secondary);
  font-weight: 600;
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
}
.data-table td { padding: 11px 14px; border-bottom: 1px solid var(--border); color: var(--text-main); }
.data-table tbody tr:nth-child(even) { background: var(--bg-stripe, rgba(176,123,62,.04)); }
.data-table tbody tr:hover { background: var(--bg-hover); }
.empty-cell { text-align: center; color: var(--text-muted); padding: 30px !important; }

.tag { display: inline-block; padding: 2px 10px; border-radius: 999px; font-size: 12px; font-weight: 500; }
.tag-green { background: var(--success-tint); color: var(--success); }
.tag-red { background: var(--danger-tint); color: var(--danger); }
.tag-blue { background: var(--info-tint); color: var(--info); }
.tag-gray { background: var(--bg-soft); color: var(--text-muted); }

.type-tags { display: flex; flex-wrap: wrap; gap: 4px; max-width: 300px; }
.type-tag { padding: 1px 8px; border-radius: 5px; background: var(--primary-tint); color: var(--primary-text); font-size: 11px; }
.text-dim { color: var(--text-faint); }

.pagination {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; background: var(--bg-soft); border-top: 1px solid var(--border);
}
.page-info { font-size: 12px; color: var(--text-muted); }
.page-actions { display: flex; align-items: center; gap: 4px; }
.page-btn, .page-num {
  min-width: 30px; height: 30px; padding: 0 6px; border: 1px solid var(--border); border-radius: 6px;
  background: #fff; color: var(--text-secondary); font-size: 13px; cursor: pointer; display: inline-flex; align-items: center; justify-content: center;
}
.page-btn:disabled { opacity: .5; cursor: not-allowed; }
.page-btn:hover:not(:disabled), .page-num:hover { border-color: var(--primary); color: var(--primary); }
.page-num.active { background: var(--primary-grad); color: #fff; border-color: transparent; }

.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,.5); display: flex; align-items: center; justify-content: center; z-index: 50; }
.modal-box {
  width: 460px; max-width: calc(100vw - 40px); max-height: 90vh; overflow-y: auto;
  background: var(--bg-white); border-radius: 12px; box-shadow: 0 20px 60px rgba(0,0,0,.25);
}
.modal-head { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-bottom: 1px solid var(--border); }
.modal-head h3 { margin: 0; font-size: 15px; color: var(--text-main); }
.modal-close { border: none; background: none; font-size: 22px; line-height: 1; color: var(--text-muted); cursor: pointer; }
.modal-body { padding: 18px 20px; display: flex; flex-direction: column; gap: 16px; }
.modal-item { display: flex; flex-direction: column; gap: 6px; }
.modal-static { font-size: 13px; color: var(--text-main); padding: 8px 0; }
.required { color: var(--danger); margin-right: 2px; }
.type-check-wrap { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; }
.type-check, .radio-item { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--text-main); cursor: pointer; }
.type-check input, .radio-item input { accent-color: var(--primary); width: 15px; height: 15px; }
.radio-wrap { display: flex; gap: 20px; }
.form-error { margin: 0; font-size: 12px; color: var(--danger); }
.modal-foot { display: flex; justify-content: flex-end; gap: 10px; padding: 14px 20px; border-top: 1px solid var(--border); }
</style>