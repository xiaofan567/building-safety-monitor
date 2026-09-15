<template>
  <div class="mar-page">
    <h2 class="mar-crumb">施工设备 > 监测报警记录</h2>

    <!-- 报警处理弹框 -->
    <div v-if="showHandleModal" class="mar-modal-mask" @click.self="closeHandleModal">
      <div class="mar-modal">
        <div class="mar-modal-head">
          <span>报警处理</span>
          <button class="mar-modal-close" @click="closeHandleModal">×</button>
        </div>
        <div class="mar-modal-body">
          <div class="mar-modal-info">
            <div>{{ currentRecord.assetName || '-' }}</div>
            <div class="mar-modal-content">{{ currentRecord.content || '-' }}</div>
          </div>
          <div class="mar-form-row">
            <label class="mar-label">
              <span class="mar-req">*</span>处理人
            </label>
            <input
              v-model.trim="handleForm.handleBy"
              type="text"
              placeholder="请输入处理人"
              class="mar-input"
            />
          </div>
          <div class="mar-form-row">
            <label class="mar-label">
              <span class="mar-req">*</span>处理措施
            </label>
            <textarea
              v-model.trim="handleForm.handleMeasures"
              rows="3"
              placeholder="请输入处理措施"
              class="mar-input mar-textarea"
            ></textarea>
          </div>
          <div class="mar-form-row">
            <label class="mar-label">
              <span class="mar-req">*</span>处理结论
            </label>
            <textarea
              v-model.trim="handleForm.handleConclu"
              rows="3"
              placeholder="请输入处理结论"
              class="mar-input mar-textarea"
            ></textarea>
          </div>
          <div class="mar-error" v-if="handleError">{{ handleError }}</div>
        </div>
        <div class="mar-modal-foot">
          <button class="mar-btn mar-btn-plain" @click="closeHandleModal">取消</button>
          <button class="mar-btn mar-btn-primary" @click="submitHandle" :disabled="submitting">
            {{ submitting ? '提交中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 筛选条件区域 -->
    <div class="mar-panel">
      <div class="mar-filter-grid">
        <div>
          <label class="mar-label">报警类型</label>
          <select v-model="filters.alarmType" class="mar-input mar-select">
            <option v-for="opt in alarmTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>
        <div>
          <label class="mar-label">开始日期</label>
          <input v-model="filters.startDate" type="date" class="mar-input" />
        </div>
        <div>
          <label class="mar-label">结束日期</label>
          <input v-model="filters.endDate" type="date" class="mar-input" />
        </div>
        <div>
          <label class="mar-label">设备名称</label>
          <input v-model.trim="filters.deviceName" type="text" placeholder="请输入设备名称" class="mar-input" />
        </div>
        <div>
          <label class="mar-label">处理状态</label>
          <select v-model="filters.status" class="mar-input mar-select">
            <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>
        <div class="mar-ops">
          <button class="mar-btn mar-btn-primary" @click="searchAlarms">搜索</button>
          <button class="mar-btn mar-btn-plain" @click="resetFilters">重置</button>
        </div>
      </div>
    </div>

    <!-- 数据表格区域 -->
    <div class="mar-panel mar-table-panel">
      <div class="mar-table-wrap">
        <table class="mar-table">
          <thead>
            <tr>
              <th>序号</th>
              <th>报警内容</th>
              <th>设备名称</th>
              <th>设备编号</th>
              <th>监测点</th>
              <th>报警值</th>
              <th>报警类型</th>
              <th>报警分类</th>
              <th>监测时间</th>
              <th>处理状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in alarmData" :key="item.id">
              <td>{{ (pagination.currentPage - 1) * pagination.pageSize + index + 1 }}</td>
              <td class="mar-cell-content" :title="item.content">{{ item.content || '-' }}</td>
              <td>{{ item.assetName || '-' }}</td>
              <td>{{ item.assetCode || '-' }}</td>
              <td>{{ item.pointName || '-' }}</td>
              <td>{{ item.monitorData ? item.monitorData + (item.unit || '') : '-' }}</td>
              <td>{{ alertTypeText(item.monitorAlertType) }}</td>
              <td>{{ alertClassText(item.alertType) }}</td>
              <td>{{ item.createTime }}</td>
              <td>
                <span class="mar-status-tag" :class="item.handleState === '1' ? 'mar-status-done' : 'mar-status-pending'">
                  {{ item.handleState === '1' ? '已处理' : '未处理' }}
                </span>
              </td>
              <td>
                <button
                  v-if="item.handleState !== '1'"
                  class="mar-btn mar-btn-primary mar-btn-sm"
                  @click="openHandleModal(item)"
                >处理</button>
                <span v-else class="mar-dash">—</span>
              </td>
            </tr>
            <tr v-if="alarmData.length === 0">
              <td colspan="11" class="mar-empty">暂无数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页组件 -->
      <div class="mar-pagination">
        <div class="mar-page-info">
          <span>共 {{ pagination.total }} 条</span>
          <span>{{ pagination.pageSize }} 条/页</span>
        </div>
        <div class="mar-page-btns">
          <button class="mar-page-btn mar-page-first" @click="goToPage(1)" :disabled="pagination.currentPage <= 1">«</button>
          <button class="mar-page-btn" @click="prevPage" :disabled="pagination.currentPage <= 1">‹</button>
          <button
            v-for="page in visiblePages"
            :key="page"
            class="mar-page-num"
            :class="{ 'mar-page-on': page === pagination.currentPage }"
            @click="goToPage(page)"
          >{{ page }}</button>
          <button class="mar-page-btn" @click="nextPage" :disabled="pagination.currentPage >= pagination.totalPages">›</button>
          <button class="mar-page-btn mar-page-last" @click="goToPage(pagination.totalPages)" :disabled="pagination.currentPage >= pagination.totalPages">»</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { selectMonitorAlertData, handleMonitorAlert } from '@/api/equipment'

// 报警类型：值对应后端 monitor_alert_type
const alarmTypeOptions = [
  { value: '', label: '请选择报警类型' },
  { value: '1', label: '基坑监测报警' },
  { value: '2', label: '塔吊报警' },
  { value: '3', label: '升降机报警' },
  { value: '9', label: '环境监测报警' }
]

const statusOptions = [
  { value: '', label: '请选择处理状态' },
  { value: '0', label: '未处理' },
  { value: '1', label: '已处理' }
]

const filters = ref({
  alarmType: '',      // 报警类型
  startDate: '',      // 开始日期
  endDate: '',        // 结束日期
  deviceName: '',     // 设备名称
  status: ''          // 处理状态
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
  totalPages: 0
})

const alarmData = ref([])

// 弹框
const showHandleModal = ref(false)
const currentRecord = ref({})
const handleForm = reactive({ handleBy: '', handleMeasures: '', handleConclu: '' })
const handleError = ref('')
const submitting = ref(false)

const visiblePages = computed(() => {
  const pages = []
  const start = Math.max(1, pagination.currentPage - 2)
  const end = Math.min(pagination.totalPages, pagination.currentPage + 2)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

// 格式化后端 LocalDateTime
function formatTime(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 19)
}

function alertTypeText(t) {
  const map = { '1': '基坑监测', '2': '塔吊监测', '3': '升降机监测', '9': '环境监测' }
  return map[t] || '-'
}

function alertClassText(t) {
  const map = { '0': '预警', '1': '报警', '2': '控制' }
  return map[t] || '-'
}

function buildPayload() {
  const payload = {
    page: pagination.currentPage,
    limit: pagination.pageSize
  }
  if (filters.value.alarmType !== '') payload.monitorAlertType = filters.value.alarmType
  if (filters.value.startDate) payload.startTime = filters.value.startDate + 'T00:00:00'
  if (filters.value.endDate) payload.endTime = filters.value.endDate + 'T23:59:59'
  if (filters.value.deviceName) payload.assetName = filters.value.deviceName
  if (filters.value.status !== '') payload.handleState = filters.value.status
  return payload
}

const loadAlarmData = async () => {
  try {
    const resp = await selectMonitorAlertData(buildPayload())
    const bp = resp && resp.data ? resp.data : {}
    alarmData.value = (bp.records || []).map((item) => ({
      id: item.id,
      content: item.content || '',
      assetName: item.assetName || '',
      assetCode: item.assetCode || '',
      pointName: item.pointName || '',
      monitorData: item.monitorData || '',
      unit: item.unit || '',
      monitorAlertType: item.monitorAlertType,
      alertType: item.alertType,
      createTime: formatTime(item.createTime),
      handleState: item.handleState,
      handleBy: item.handleBy || '',
      handleMeasures: item.handleMeasures || '',
      handleConclu: item.handleConclu || ''
    }))
    pagination.total = bp.total || 0
    pagination.totalPages = Math.max(1, bp.pages || Math.ceil(pagination.total / pagination.pageSize))
  } catch (error) {
    console.error('加载监测报警记录失败:', error)
    alarmData.value = []
    pagination.total = 0
    pagination.totalPages = 0
  }
}

const searchAlarms = () => { pagination.currentPage = 1; loadAlarmData() }

const resetFilters = () => {
  filters.value = {
    alarmType: '',
    startDate: '',
    endDate: '',
    deviceName: '',
    status: ''
  }
  pagination.currentPage = 1
  loadAlarmData()
}

const goToPage = (page) => {
  if (page < 1 || page > pagination.totalPages) return
  pagination.currentPage = page
  loadAlarmData()
}

const prevPage = () => { if (pagination.currentPage > 1) { pagination.currentPage--; loadAlarmData() } }

const nextPage = () => { if (pagination.currentPage < pagination.totalPages) { pagination.currentPage++; loadAlarmData() } }

// 打开处理弹框
function openHandleModal(item) {
  currentRecord.value = item
  handleForm.handleBy = ''
  handleForm.handleMeasures = ''
  handleForm.handleConclu = ''
  handleError.value = ''
  showHandleModal.value = true
}

function closeHandleModal() {
  showHandleModal.value = false
  currentRecord.value = {}
}

// 提交处理
async function submitHandle() {
  handleError.value = ''
  if (!handleForm.handleBy) { handleError.value = '请输入处理人'; return }
  if (!handleForm.handleMeasures) { handleError.value = '请输入处理措施'; return }
  if (!handleForm.handleConclu) { handleError.value = '请输入处理结论'; return }

  submitting.value = true
  try {
    await handleMonitorAlert({
      id: currentRecord.value.id,
      handleBy: handleForm.handleBy,
      handleMeasures: handleForm.handleMeasures,
      handleConclu: handleForm.handleConclu
    })
    ElMessage.success('报警处理成功')
    showHandleModal.value = false
    // 刷新当前页数据，保持筛选条件
    loadAlarmData()
  } catch (error) {
    console.error('处理报警失败:', error)
  } finally {
    submitting.value = false
  }
}

onMounted(loadAlarmData)
</script>

<style scoped>
.mar-page {
  min-height: 100vh;
  background: var(--bg-page);
  padding: 16px;
}

.mar-crumb {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-muted);
  margin: 8px 0 12px;
}

.mar-panel {
  background: var(--bg-white, #fff);
  border: 1px solid var(--border);
  border-radius: 10px;
  box-shadow: var(--shadow-sm);
  margin-bottom: 16px;
}

.mar-filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  padding: 20px 24px;
  align-items: end;
}

.mar-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.mar-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 13px;
  color: var(--text-strong);
  background: var(--bg-white, #fff);
  outline: none;
  box-sizing: border-box;
}
.mar-input:focus { border-color: var(--primary-text); box-shadow: 0 0 0 2px var(--primary-grad); }
.mar-select { cursor: pointer; }

.mar-ops { display: flex; gap: 12px; }

.mar-btn {
  padding: 8px 18px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all .2s;
}
.mar-btn-primary {
  background: var(--primary-grad);
  color: #fff;
  border: none;
}
.mar-btn-primary:hover { box-shadow: 0 4px 12px rgba(0,0,0,.12); }
.mar-btn-plain {
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid var(--border);
}
.mar-btn-plain:hover { background: var(--bg-muted); }
.mar-btn-sm { padding: 4px 14px; font-size: 12px; }

.mar-table-panel { padding: 0; overflow: hidden; }
.mar-table-wrap { overflow-x: auto; }

.mar-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 1100px;
}
.mar-table th {
  text-align: left;
  padding: 12px 18px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: .03em;
  background: var(--bg-muted);
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
}
.mar-table td {
  padding: 12px 18px;
  font-size: 13px;
  color: var(--text-strong);
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
}
.mar-table tbody tr:hover { background: var(--bg-muted); }
.mar-cell-content { max-width: 260px; overflow: hidden; text-overflow: ellipsis; }

.mar-empty { text-align: center; color: var(--text-muted); padding: 32px 0 !important; }
.mar-dash { color: var(--text-faint, #bbb); }

.mar-status-tag {
  display: inline-flex;
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 600;
  border-radius: 999px;
}
.mar-status-done { background: rgba(46,125,50,.12); color: #2e7d50; }
.mar-status-pending { background: rgba(255,179,0,.16); color: #b07b3e; }

.mar-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  border-top: 1px solid var(--border);
  background: var(--bg-muted);
}
.mar-page-info { display: flex; gap: 16px; font-size: 13px; color: var(--text-secondary); }
.mar-page-btns { display: flex; align-items: center; gap: 4px; }
.mar-page-btn, .mar-page-num {
  min-width: 30px;
  height: 30px;
  padding: 0 8px;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 13px;
  color: var(--text-secondary);
  background: var(--bg-white, #fff);
  cursor: pointer;
}
.mar-page-btn:disabled { opacity: .5; cursor: not-allowed; }
.mar-page-num:hover { border-color: var(--primary-text); }
.mar-page-on { background: var(--primary-grad); border-color: transparent; color: #fff; }

/* 处理弹框 */
.mar-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.mar-modal {
  width: 480px;
  max-width: 92vw;
  background: var(--bg-white, #fff);
  border-radius: 12px;
  box-shadow: var(--shadow, 0 12px 32px rgba(0,0,0,.18));
  overflow: hidden;
}
.mar-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-strong);
  border-bottom: 1px solid var(--border);
}
.mar-modal-close {
  border: none;
  background: transparent;
  font-size: 20px;
  line-height: 1;
  color: var(--text-muted);
  cursor: pointer;
}
.mar-modal-body { padding: 20px; }
.mar-modal-info {
  background: var(--bg-soft, #f5f5f5);
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 16px;
  font-size: 13px;
  color: var(--text-strong);
}
.mar-modal-content {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 12px;
}
.mar-form-row { margin-bottom: 16px; }
.mar-req { color: #d93026; margin-right: 2px; }
.mar-textarea { resize: none; }
.mar-error { color: #d93026; font-size: 12px; margin-top: -6px; margin-bottom: 4px; }
.mar-modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 14px 20px;
  border-top: 1px solid var(--border);
  background: var(--bg-muted);
}
</style>