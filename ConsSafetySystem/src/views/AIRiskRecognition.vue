<template>
  <div class="ai-risk">
    <div class="breadcrumb">
      <span class="crumb-tag">施工场地</span>
      <span class="crumb-sep">/</span>
      <span class="crumb-current">AI风险识别与处理</span>
    </div>

    <!-- 筛选条件区 -->
    <div class="filter-card">
      <div class="filter-form">
        <div class="form-item">
          <label class="form-label">设备编号</label>
          <input v-model="filters.deviceNumber" class="form-input" type="text" placeholder="请输入设备编号" @keyup.enter="handleSearch" />
        </div>
        <div class="form-item">
          <label class="form-label">报警类型</label>
          <select v-model="filters.alarmType" class="form-select">
            <option value="">请选择报警类型</option>
            <option v-for="t in alarmTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
          </select>
        </div>
        <div class="form-item">
          <label class="form-label">开始日期</label>
          <input v-model="filters.startDate" class="form-input" type="date" />
        </div>
        <div class="form-item">
          <label class="form-label">结束日期</label>
          <input v-model="filters.endDate" class="form-input" type="date" />
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
              <th>设备编号</th>
              <th>报警时间</th>
              <th>报警类型</th>
              <th>报警位置</th>
              <th>报警内容</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in tableData" :key="row.id">
              <td>{{ (pagination.currentPage - 1) * pagination.pageSize + index + 1 }}</td>
              <td>{{ row.code || '--' }}</td>
              <td>{{ (row.alertTime || '').replace('T', ' ').slice(0, 19) || '--' }}</td>
              <td><span class="type-tag red">{{ alertTypeLabel(row.alertType) }}</span></td>
              <td>{{ row.alertPosition || '--' }}</td>
              <td class="content-cell" :title="row.alertContent">{{ row.alertContent || '--' }}</td>
              <td>
                <span class="tag" :class="row.alertStatus !== '1' ? 'tag-red' : 'tag-green'">
                  {{ row.alertStatus === '1' ? '已处理' : '未处理' }}
                </span>
              </td>
              <td>
                <div class="op-group">
                  <button class="btn-op" @click="openDetail(row)">查看</button>
                  <button v-if="row.alertStatus !== '1'" class="btn-op primary" @click="openHandle(row)">处理</button>
                </div>
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

    <!-- 详情弹框 -->
    <div v-if="showDetailModal" class="modal-mask" @click.self="showDetailModal = false">
      <div class="modal-box detail-box">
        <div class="modal-head">
          <h3>报警详情</h3>
          <button class="modal-close" @click="showDetailModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="detail-grid">
            <div class="detail-item"><span class="detail-key">设备编号</span><span class="detail-val">{{ detail.code || '--' }}</span></div>
            <div class="detail-item"><span class="detail-key">设备名称</span><span class="detail-val">{{ detail.assetName || '--' }}</span></div>
            <div class="detail-item"><span class="detail-key">报警时间</span><span class="detail-val">{{ (detail.alertTime || '').replace('T', ' ').slice(0, 19) || '--' }}</span></div>
            <div class="detail-item"><span class="detail-key">报警类型</span><span class="detail-val type-tag red">{{ alertTypeLabel(detail.alertType) }}</span></div>
            <div class="detail-item"><span class="detail-key">报警位置</span><span class="detail-val">{{ detail.alertPosition || '--' }}</span></div>
            <div class="detail-item"><span class="detail-key">报警内容</span><span class="detail-val">{{ detail.alertContent || '--' }}</span></div>
          </div>
          <div class="img-block" v-if="detail.alertImg">
            <img :src="detail.alertImg" alt="识别图像" class="alert-img" @error="imgError = true" />
          </div>
          <p v-if="imgError" class="text-dim">（暂无识别图像）</p>
        </div>
        <div class="modal-foot">
          <button class="btn btn-plain" @click="showDetailModal = false">关闭</button>
        </div>
      </div>
    </div>

    <!-- 处理弹框 -->
    <div v-if="showHandleModal" class="modal-mask" @click.self="showHandleModal = false">
      <div class="modal-box">
        <div class="modal-head">
          <h3>处理报警</h3>
          <button class="modal-close" @click="showHandleModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="handle-static">
            <span class="form-label">设备编号</span>
            <span class="modal-static">{{ handleForm.code || '--' }}</span>
          </div>
          <div class="modal-item">
            <label class="form-label"><span class="required">*</span>处理人</label>
            <input v-model="handleForm.handleBy" class="form-input wide" type="text" placeholder="请输入处理人" />
          </div>
          <div class="modal-item">
            <label class="form-label"><span class="required">*</span>处理内容</label>
            <textarea v-model="handleForm.handleContent" class="form-textarea" rows="4" placeholder="请输入处理内容"></textarea>
          </div>
          <p v-if="formError" class="form-error">{{ formError }}</p>
        </div>
        <div class="modal-foot">
          <button class="btn btn-plain" @click="showHandleModal = false">取消</button>
          <button class="btn btn-primary" @click="handleSubmit">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { selectAiAlertData, updateAiAlert } from '@/api/video'

// 报警类型（1-6）
const alarmTypes = [
  { value: '1', label: '未戴安全帽' },
  { value: '2', label: '未穿安全衣' },
  { value: '3', label: '工作现场抽烟' },
  { value: '4', label: '夜间施工' },
  { value: '5', label: '工作现场明烟' },
  { value: '6', label: '工作现场明火' }
]
const typeLabelMap = alarmTypes.reduce((acc, o) => { acc[o.value] = o.label; return acc }, {})
function alertTypeLabel(v) {
  return (v || '').split(',').filter(Boolean).map(x => typeLabelMap[x] || ('类型' + x)).join('/') || '--'
}

const filters = ref({ deviceNumber: '', alarmType: '', startDate: '', endDate: '' })
const tableData = ref([])
const pagination = reactive({ currentPage: 1, pageSize: 10, total: 0, totalPages: 1 })

const showDetailModal = ref(false)
const detail = ref({})
const imgError = ref(false)

const showHandleModal = ref(false)
const handleForm = reactive({ id: null, code: '', handleBy: '', handleContent: '' })
const formError = ref('')

async function loadData() {
  try {
    const params = {
      assetCode: filters.value.deviceNumber,
      alertType: filters.value.alarmType,
      startTime: filters.value.startDate ? `${filters.value.startDate} 00:00:00` : '',
      endTime: filters.value.endDate ? `${filters.value.endDate} 23:59:59` : '',
      page: pagination.currentPage,
      limit: pagination.pageSize
    }
    const res = await selectAiAlertData(params)
    const data = (res && res.data) || {}
    tableData.value = data.records || []
    pagination.total = data.total || 0
    pagination.totalPages = data.pages || Math.ceil(pagination.total / pagination.pageSize) || 1
  } catch (e) {
    console.error('AI报警数据加载失败', e)
    tableData.value = []
  }
}

function handleSearch() { pagination.currentPage = 1; loadData() }
function handleReset() {
  filters.value = { deviceNumber: '', alarmType: '', startDate: '', endDate: '' }
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

function openDetail(row) {
  detail.value = { ...row }
  imgError.value = false
  showDetailModal.value = true
}

function openHandle(row) {
  handleForm.id = row.id
  handleForm.code = row.code || ''
  handleForm.handleBy = ''
  handleForm.handleContent = ''
  formError.value = ''
  showHandleModal.value = true
}

async function handleSubmit() {
  if (!handleForm.handleBy.trim()) { formError.value = '请输入处理人'; return }
  if (!handleForm.handleContent.trim()) { formError.value = '请输入处理内容'; return }
  formError.value = ''
  try {
    await updateAiAlert({ id: handleForm.id, handleBy: handleForm.handleBy, handleContent: handleForm.handleContent })
    showHandleModal.value = false
    loadData()
  } catch (e) {
    formError.value = '处理失败，请重试'
    console.error('处理失败', e)
  }
}

onMounted(loadData)
</script>

<style scoped>
.ai-risk {
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

.filter-card { background: var(--bg-white); border: 1px solid var(--border); border-radius: 12px; box-shadow: var(--shadow-sm); padding: 16px 20px; margin-bottom: 16px; }
.filter-form { display: flex; align-items: flex-end; flex-wrap: wrap; gap: 16px; }
.form-item { display: flex; flex-direction: column; gap: 6px; }
.form-label { font-size: 12px; color: var(--text-secondary); }
.form-input, .form-select {
  width: 200px; height: 34px; padding: 0 12px; border: 1px solid var(--border); border-radius: 8px;
  background: var(--bg-field, #fff); font-size: 13px; color: var(--text-main); outline: none; box-sizing: border-box; transition: border .2s;
}
.form-input:focus { border-color: var(--primary); }
.form-input.wide { width: 100%; }
.form-textarea { width: 100%; border: 1px solid var(--border); border-radius: 8px; padding: 10px 12px; font-size: 13px; color: var(--text-main); resize: vertical; outline: none; background: var(--bg-field, #fff); box-sizing: border-box; }
.form-textarea:focus { border-color: var(--primary); }
.form-actions { display: flex; gap: 10px; }

.btn { height: 34px; padding: 0 20px; border-radius: 8px; font-size: 13px; cursor: pointer; border: none; transition: opacity .2s; }
.btn-primary { background: var(--primary-grad); color: #fff; }
.btn-primary:hover { opacity: .9; }
.btn-plain { background: var(--bg-soft); color: var(--text-secondary); border: 1px solid var(--border); }
.btn-plain:hover { color: var(--primary); border-color: var(--primary); }

.table-card { background: var(--bg-white); border: 1px solid var(--border); border-radius: 12px; box-shadow: var(--shadow-sm); overflow: hidden; }
.table-wrap { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th { padding: 12px 14px; text-align: left; background: var(--bg-soft); color: var(--text-secondary); font-weight: 600; border-bottom: 1px solid var(--border); white-space: nowrap; }
.data-table td { padding: 11px 14px; border-bottom: 1px solid var(--border); color: var(--text-main); }
.data-table tbody tr:nth-child(even) { background: var(--bg-stripe, rgba(176,123,62,.04)); }
.data-table tbody tr:hover { background: var(--bg-hover); }
.content-cell { max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.empty-cell { text-align: center; color: var(--text-muted); padding: 30px !important; }

.type-tag { display: inline-block; padding: 2px 10px; border-radius: 999px; font-size: 12px; font-weight: 500; }
.type-tag.red { background: var(--danger-tint); color: var(--danger); }
.tag { display: inline-block; padding: 2px 10px; border-radius: 999px; font-size: 12px; font-weight: 500; }
.tag-green { background: var(--success-tint); color: var(--success); }
.tag-red { background: var(--danger-tint); color: var(--danger); }

.op-group { display: flex; gap: 6px; }
.btn-op { height: 28px; padding: 0 14px; font-size: 12px; border-radius: 6px; cursor: pointer; border: 1px solid var(--border); background: var(--bg-soft); color: var(--text-secondary); }
.btn-op:hover { border-color: var(--primary); color: var(--primary); }
.btn-op.primary { background: var(--primary-tint); color: var(--primary-text); border-color: rgba(176,123,62,.3); }
.btn-op.primary:hover { background: var(--primary); color: #fff; }

.pagination { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; background: var(--bg-soft); border-top: 1px solid var(--border); }
.page-info { font-size: 12px; color: var(--text-muted); }
.page-actions { display: flex; align-items: center; gap: 4px; }
.page-btn, .page-num { min-width: 30px; height: 30px; padding: 0 6px; border: 1px solid var(--border); border-radius: 6px; background: #fff; color: var(--text-secondary); font-size: 13px; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; }
.page-btn:disabled { opacity: .5; cursor: not-allowed; }
.page-btn:hover:not(:disabled), .page-num:hover { border-color: var(--primary); color: var(--primary); }
.page-num.active { background: var(--primary-grad); color: #fff; border-color: transparent; }

.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,.5); display: flex; align-items: center; justify-content: center; z-index: 50; }
.modal-box { width: 460px; max-width: calc(100vw - 40px); max-height: 90vh; overflow-y: auto; background: var(--bg-white); border-radius: 12px; box-shadow: 0 20px 60px rgba(0,0,0,.25); }
.detail-box { width: 560px; }
.modal-head { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-bottom: 1px solid var(--border); }
.modal-head h3 { margin: 0; font-size: 15px; color: var(--text-main); }
.modal-close { border: none; background: none; font-size: 22px; line-height: 1; color: var(--text-muted); cursor: pointer; }
.modal-body { padding: 18px 20px; display: flex; flex-direction: column; gap: 14px; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.detail-item { display: flex; flex-direction: column; gap: 4px; }
.detail-key { font-size: 12px; color: var(--text-muted); }
.detail-val { font-size: 13px; color: var(--text-main); word-break: break-all; }
.img-block { display: flex; justify-content: center; background: #000; border-radius: 8px; overflow: hidden; }
.alert-img { max-width: 100%; max-height: 340px; object-fit: contain; }
.modal-item { display: flex; flex-direction: column; gap: 6px; }
.handle-static { display: flex; flex-direction: column; gap: 4px; }
.modal-static { font-size: 13px; color: var(--text-main); }
.required { color: var(--danger); margin-right: 2px; }
.form-error { margin: 0; font-size: 12px; color: var(--danger); }
.text-dim { font-size: 12px; color: var(--text-faint); text-align: center; }
.modal-foot { display: flex; justify-content: flex-end; gap: 10px; padding: 14px 20px; border-top: 1px solid var(--border); }
</style>