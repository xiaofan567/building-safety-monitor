<template>
  <div class="report-list-page">
    <header class="action-bar">
      <div class="action-info">
        <span class="info-tag">日报查看</span>
        <span class="info-desc">按时间范围与名称筛选 Coze 平台生成的安全报告，支持在线预览与下载</span>
      </div>
      <div class="action-buttons">
        <button class="ghost-btn" type="button" :disabled="loading" @click="loadDocs">
          <svg viewBox="0 0 24 24" fill="none" width="15" height="15" aria-hidden="true">
            <path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          刷新
        </button>
        <button class="primary-btn" type="button" :disabled="syncing" @click="triggerSync">
          <svg viewBox="0 0 24 24" fill="none" width="15" height="15" aria-hidden="true">
            <path d="M4 16v2a2 2 0 002 2h12a2 2 0 002-2v-2M7 10l5 5 5-5M12 15V3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          {{ syncing ? '同步中...' : '手动同步' }}
        </button>
      </div>
    </header>

    <!-- 筛选条件 -->
    <section class="filter-board">
      <div class="filter-row">
        <div class="filter-item filter-item-wide">
          <label class="filter-label">创建时间</label>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            class="filter-date"
            size="default"
          />
        </div>
        <div class="filter-item">
          <label class="filter-label">文档名称</label>
          <input
            v-model="filters.docName"
            type="text"
            placeholder="输入文档名称关键词"
            class="filter-input"
            @keyup.enter="searchDocs"
          />
        </div>
        <div class="filter-actions">
          <button class="primary-btn" type="button" @click="searchDocs">
            <svg viewBox="0 0 24 24" fill="none" width="14" height="14" aria-hidden="true">
              <path d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            搜索
          </button>
          <button class="ghost-btn" type="button" @click="resetFilters">重置</button>
        </div>
      </div>
    </section>

    <!-- 文档表格 -->
    <section class="table-board">
      <div v-if="loading" class="loading-block">
        <div class="spin-loader"></div>
        <span>加载中...</span>
      </div>

      <template v-else>
        <div class="table-wrap">
          <table class="doc-table">
            <thead>
              <tr>
                <th class="col-index">序号</th>
                <th class="col-name">文档名称</th>
                <th class="col-time">创建时间</th>
                <th class="col-action">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(doc, index) in docs" :key="doc.id">
                <td class="col-index">{{ (pagination.currentPage - 1) * pagination.pageSize + index + 1 }}</td>
                <td class="col-name">
                  <span class="doc-name" @click="viewDoc(doc)">{{ doc.docName || '--' }}</span>
                </td>
                <td class="col-time">{{ doc.createTime || '--' }}</td>
                <td class="col-action">
                  <div class="action-buttons-row">
                    <button class="link-btn link-btn-blue" type="button" @click="viewDoc(doc)">
                      <svg viewBox="0 0 24 24" fill="none" width="13" height="13" aria-hidden="true">
                        <path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                      </svg>
                      查看
                    </button>
                    <button class="link-btn link-btn-green" type="button" @click="downloadDoc(doc)">
                      <svg viewBox="0 0 24 24" fill="none" width="13" height="13" aria-hidden="true">
                        <path d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                      </svg>
                      下载
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="docs.length === 0">
                <td colspan="4" class="empty-cell">
                  <div class="empty-block">
                    <svg viewBox="0 0 24 24" fill="none" width="48" height="48">
                      <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    <p>暂无文档数据</p>
                    <p class="empty-tip">可通过点击右上角"手动同步"立即拉取最新报告</p>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="pagination.total > 0" class="pagination-bar">
          <div class="pagination-info">
            <span>共 <strong>{{ pagination.total }}</strong> 条</span>
            <span class="divider">|</span>
            <span>每页 {{ pagination.pageSize }} 条</span>
          </div>
          <div class="pagination-pages">
            <button class="page-btn" :disabled="pagination.currentPage <= 1" @click="prevPage">
              <svg viewBox="0 0 24 24" fill="none" width="14" height="14" aria-hidden="true">
                <path d="M15 19l-7-7 7-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
            <button
              v-for="page in visiblePages"
              :key="page"
              class="page-num"
              :class="{ active: page === pagination.currentPage, ellipsis: page === '...' }"
              :disabled="page === '...'"
              @click="goToPage(page)"
            >{{ page }}</button>
            <button class="page-btn" :disabled="pagination.currentPage >= pagination.totalPages" @click="nextPage">
              <svg viewBox="0 0 24 24" fill="none" width="14" height="14" aria-hidden="true">
                <path d="M9 5l7 7-7 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>
        </div>
      </template>
    </section>

    <!-- 预览弹窗 -->
    <transition name="modal">
      <div v-if="showViewModal" class="modal-mask" @click.self="closeModal">
        <div class="modal-card">
          <header class="modal-head">
            <div class="modal-title">
              <span class="modal-mark">
                <svg viewBox="0 0 24 24" fill="none" width="18" height="18" aria-hidden="true">
                  <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </span>
              <div>
                <h3>{{ viewDocTitle }}</h3>
                <p>{{ viewDocMeta }}</p>
              </div>
            </div>
            <button class="modal-close" type="button" @click="closeModal">
              <svg viewBox="0 0 24 24" fill="none" width="18" height="18" aria-hidden="true">
                <path d="M6 18L18 6M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </button>
          </header>
          <div class="modal-body" v-html="viewDocContent"></div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElDatePicker, ElMessage } from 'element-plus'
import mammoth from 'mammoth'
import {
  getDocumentList,
  getDocumentFile,
  downloadDocumentFile,
  triggerDocumentSync
} from '@/api/dailyReport'

const loading = ref(false)
const syncing = ref(false)
const docs = ref([])

// 默认日期范围：最近 30 天
const getDefaultDateRange = () => {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 30)
  const fmt = (d) => {
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }
  return [fmt(start), fmt(end)]
}

const dateRange = ref(getDefaultDateRange())
const filters = ref({
  startDate: dateRange.value[0],
  endDate: dateRange.value[1],
  docName: ''
})

watch(dateRange, (val) => {
  if (val && val.length === 2) {
    filters.value.startDate = val[0]
    filters.value.endDate = val[1]
  } else {
    filters.value.startDate = ''
    filters.value.endDate = ''
  }
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0,
  totalPages: 1
})

const visiblePages = computed(() => {
  const pages = []
  const total = pagination.totalPages
  if (total <= 5) {
    for (let i = 1; i <= total; i++) pages.push(i)
    return pages
  }
  const current = pagination.currentPage
  pages.push(1)
  if (current > 3) pages.push('...')
  const start = Math.max(2, current - 1)
  const end = Math.min(total - 1, current + 1)
  for (let i = start; i <= end; i++) pages.push(i)
  if (current < total - 2) pages.push('...')
  pages.push(total)
  return pages
})

const showViewModal = ref(false)
const viewDocTitle = ref('')
const viewDocMeta = ref('')
const viewDocContent = ref('')

const loadDocs = async () => {
  loading.value = true
  try {
    const requestData = { page: pagination.currentPage, limit: pagination.pageSize }
    if (filters.value.docName) requestData.docName = filters.value.docName
    if (filters.value.startDate) requestData.startDate = filters.value.startDate
    if (filters.value.endDate) requestData.endDate = filters.value.endDate

    const res = await getDocumentList(requestData)
    if (res && res.code === 0 && res.data) {
      docs.value = res.data.records || []
      pagination.total = res.data.total || 0
    } else if (res && res.records) {
      docs.value = res.records || []
      pagination.total = res.total || 0
    } else {
      docs.value = []
      pagination.total = 0
    }
    pagination.totalPages = Math.max(1, Math.ceil(pagination.total / pagination.pageSize))
  } catch (error) {
    console.error('加载文档列表失败:', error)
    docs.value = []
    pagination.total = 0
    pagination.totalPages = 1
    ElMessage.error('加载文档列表失败')
  } finally {
    loading.value = false
  }
}

const searchDocs = () => {
  pagination.currentPage = 1
  loadDocs()
}

const resetFilters = () => {
  dateRange.value = getDefaultDateRange()
  filters.value = { startDate: dateRange.value[0], endDate: dateRange.value[1], docName: '' }
  pagination.currentPage = 1
  loadDocs()
}

const goToPage = (page) => {
  if (page === '...') return
  pagination.currentPage = page
  loadDocs()
}

const prevPage = () => {
  if (pagination.currentPage > 1) {
    pagination.currentPage--
    loadDocs()
  }
}

const nextPage = () => {
  if (pagination.currentPage < pagination.totalPages) {
    pagination.currentPage++
    loadDocs()
  }
}

const triggerSync = async () => {
  syncing.value = true
  try {
    const msg = await triggerDocumentSync()
    ElMessage.success(typeof msg === 'string' ? msg : '同步任务已触发')
    await loadDocs()
  } catch (error) {
    console.error('同步失败:', error)
    ElMessage.error('同步失败，请稍后重试')
  } finally {
    syncing.value = false
  }
}

const viewDoc = async (doc) => {
  if (!doc || !doc.docUrl) {
    ElMessage.warning('该文档暂无可预览的文件')
    return
  }
  viewDocTitle.value = doc.docName || '文档预览'
  viewDocMeta.value = `创建时间：${doc.createTime || '--'}`
  showViewModal.value = true
  viewDocContent.value = '<div class="doc-loading"><div class="spin-loader"></div><span>加载中...</span></div>'

  try {
    const response = await getDocumentFile(doc.docUrl)
    const blob = response && response.data ? response.data : null
    if (!blob || blob.size === 0) {
      viewDocContent.value = '<p class="doc-empty">暂无可查看的文档内容</p>'
      return
    }
    const fileName = (doc.docName || '').toLowerCase()
    if (fileName.endsWith('.docx') || fileName.endsWith('.doc')) {
      const arrayBuffer = await blob.arrayBuffer()
      const result = await mammoth.convertToHtml({ arrayBuffer })
      viewDocContent.value = result.value || '<p class="doc-empty">暂无可查看的文档内容</p>'
    } else if (fileName.endsWith('.md') || fileName.endsWith('.txt')) {
      const text = await blob.text()
      const escaped = text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
      viewDocContent.value = `<pre class="plain-pre">${escaped}</pre>`
    } else {
      viewDocContent.value = '<p class="doc-empty">该文档类型暂不支持在线预览，请使用下载功能</p>'
    }
  } catch (error) {
    console.error('加载文档失败:', error)
    viewDocContent.value = '<p class="doc-empty">加载文档失败，请稍后重试</p>'
  }
}

const closeModal = () => {
  showViewModal.value = false
}

const downloadDoc = (doc) => {
  if (!doc || !doc.docUrl) {
    ElMessage.warning('该文档暂无可下载的文件')
    return
  }
  downloadDocumentFile(doc.docUrl, doc.docName || '文档.docx')
}

onMounted(() => {
  loadDocs()
})
</script>

<style scoped>
.report-list-page {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 14px;
  padding: 16px 18px 18px;
  background: var(--bg-page);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ========== 顶部操作条 ========== */
.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 18px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-white);
  box-shadow: var(--shadow-sm);
}

.action-info {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
}

.info-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--primary-tint);
  color: var(--primary-text);
  font-size: 12px;
  font-weight: 700;
}

.info-desc {
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.6;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.ghost-btn,
.primary-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 9px 14px;
  border: 1px solid transparent;
  border-radius: 9px;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.ghost-btn {
  border-color: var(--border);
  background: var(--bg-white);
  color: var(--text-secondary);
}

.ghost-btn:hover:not(:disabled) {
  border-color: var(--primary-soft);
  background: var(--bg-soft);
  color: var(--primary-text);
}

.primary-btn {
  border-color: transparent;
  background: var(--primary-grad);
  box-shadow: 0 6px 14px rgba(176, 123, 62, 0.22);
  color: #fff;
}

.primary-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(176, 123, 62, 0.28);
}

.ghost-btn:disabled,
.primary-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

/* ========== 筛选区 ========== */
.filter-board {
  padding: 16px 18px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-white);
  box-shadow: var(--shadow-sm);
}

.filter-row {
  display: flex;
  align-items: flex-end;
  gap: 14px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  min-width: 180px;
  flex-direction: column;
}

.filter-item-wide {
  min-width: 320px;
}

.filter-label {
  margin-bottom: 6px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.filter-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-white);
  color: var(--text-main);
  font-family: inherit;
  font-size: 13px;
  transition: all 0.2s ease;
}

.filter-input:focus {
  outline: none;
  border-color: var(--primary-soft);
  box-shadow: 0 0 0 3px rgba(176, 123, 62, 0.12);
}

.filter-input::placeholder {
  color: var(--text-faint);
}

.filter-date {
  width: 100%;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

/* ========== 表格 ========== */
.table-board {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-white);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.loading-block {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 60px 0;
  color: var(--text-muted);
  font-size: 13px;
}

.spin-loader {
  width: 18px;
  height: 18px;
  border: 2px solid var(--bg-muted);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.table-wrap {
  flex: 1;
  overflow-x: auto;
}

.doc-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.doc-table thead th {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-soft);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 700;
  text-align: left;
  letter-spacing: 0.4px;
}

.col-index { width: 70px; text-align: center !important; }
.col-name { }
.col-action { width: 180px; text-align: center !important; }

.doc-table tbody td {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  color: var(--text-main);
  vertical-align: middle;
}

.doc-table tbody tr:hover {
  background: var(--bg-hover);
}

.doc-table tbody tr:last-child td {
  border-bottom: none;
}

.doc-name {
  color: var(--text-strong);
  font-weight: 600;
  cursor: pointer;
  transition: color 0.15s ease;
}

.doc-name:hover {
  color: var(--primary-text);
  text-decoration: underline;
}

.action-buttons-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.link-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border: 1px solid transparent;
  border-radius: 7px;
  font-family: inherit;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.link-btn-blue {
  color: var(--info);
  background: var(--info-tint);
}

.link-btn-blue:hover {
  background: rgba(90, 143, 212, 0.22);
}

.link-btn-green {
  color: var(--success);
  background: var(--success-tint);
}

.link-btn-green:hover {
  background: rgba(77, 133, 71, 0.22);
}

.empty-cell {
  padding: 50px 0 !important;
  text-align: center !important;
}

.empty-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: var(--text-faint);
}

.empty-block p {
  margin: 8px 0 0;
  font-size: 13px;
}

.empty-tip {
  color: var(--text-muted) !important;
  font-size: 12px !important;
}

/* ========== 分页 ========== */
.pagination-bar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  border-top: 1px solid var(--border);
  background: var(--bg-soft);
}

.pagination-info {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-muted);
  font-size: 13px;
}

.pagination-info strong {
  color: var(--text-strong);
}

.divider {
  color: var(--border-strong);
}

.pagination-pages {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-btn,
.page-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 7px;
  background: transparent;
  color: var(--text-secondary);
  font-family: inherit;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-btn:hover:not(:disabled),
.page-num:hover:not(:disabled):not(.ellipsis) {
  background: var(--bg-white);
  color: var(--text-strong);
  border-color: var(--border);
}

.page-btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.page-num.active {
  background: var(--primary-grad);
  color: #fff;
  border-color: transparent;
  box-shadow: 0 4px 10px rgba(176, 123, 62, 0.25);
}

.page-num.ellipsis {
  cursor: default;
  background: transparent;
  color: var(--text-faint);
}

/* ========== 预览弹窗 ========== */
.modal-mask {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  background: rgba(40, 30, 16, 0.4);
  backdrop-filter: blur(4px);
}

.modal-card {
  display: flex;
  width: 90%;
  max-width: 880px;
  max-height: 85vh;
  flex-direction: column;
  border-radius: 14px;
  background: var(--bg-white);
  box-shadow: 0 24px 60px rgba(60, 45, 20, 0.28);
  overflow: hidden;
}

.modal-head {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.modal-mark {
  display: flex;
  width: 38px;
  height: 38px;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: var(--primary-tint);
  color: var(--primary-text);
}

.modal-title h3 {
  margin: 0;
  color: var(--text-strong);
  font-size: 15px;
  font-weight: 700;
}

.modal-title p {
  margin: 3px 0 0;
  color: var(--text-muted);
  font-size: 12px;
}

.modal-close {
  display: flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: var(--bg-soft);
  color: var(--text-strong);
}

.modal-body {
  flex: 1;
  padding: 22px 26px;
  overflow-y: auto;
  line-height: 1.75;
  color: var(--text-main);
  font-size: 14px;
}

.modal-body::-webkit-scrollbar {
  width: 6px;
}

.modal-body::-webkit-scrollbar-thumb {
  border-radius: 4px;
  background: rgba(176, 123, 62, 0.22);
}

.modal-body :deep(h1) {
  margin: 1.2rem 0 0.5rem;
  padding-bottom: 0.4rem;
  border-bottom: 2px solid var(--border);
  color: var(--text-strong);
  font-size: 1.4rem;
  font-weight: 700;
}

.modal-body :deep(h2) {
  margin: 1rem 0 0.4rem;
  color: var(--text-strong);
  font-size: 1.15rem;
  font-weight: 600;
}

.modal-body :deep(h3) {
  margin: 0.75rem 0 0.25rem;
  color: var(--text-strong);
  font-size: 1rem;
  font-weight: 600;
}

.modal-body :deep(p) {
  margin-bottom: 0.6rem;
  color: var(--text-main);
  line-height: 1.85;
}

.modal-body :deep(ul),
.modal-body :deep(ol) {
  margin-bottom: 0.6rem;
  padding-left: 1.6rem;
}

.modal-body :deep(li) {
  margin-bottom: 0.2rem;
  line-height: 1.85;
}

.modal-body :deep(table) {
  width: 100%;
  margin: 0.6rem 0;
  border-collapse: collapse;
  font-size: 0.875rem;
}

.modal-body :deep(th),
.modal-body :deep(td) {
  padding: 8px 12px;
  border: 1px solid var(--border);
  text-align: left;
}

.modal-body :deep(th) {
  background: var(--bg-muted);
  color: var(--text-strong);
  font-weight: 600;
}

.modal-body :deep(blockquote) {
  margin: 0.6rem 0;
  padding: 0.5rem 1rem;
  border-left: 3px solid var(--primary-soft);
  border-radius: 0 8px 8px 0;
  background: var(--bg-soft);
  color: var(--text-secondary);
}

.modal-body :deep(code) {
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
  background: var(--bg-muted);
  color: var(--danger);
  font-size: 0.85em;
}

.modal-body :deep(pre) {
  margin: 0.6rem 0;
  padding: 14px 16px;
  border-radius: 8px;
  background: #1f2937;
  overflow-x: auto;
}

.modal-body :deep(pre code) {
  padding: 0;
  background: transparent;
  color: #e2e8f0;
  font-size: 0.85rem;
}

.modal-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
}

.doc-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 50px 0;
  color: var(--text-muted);
  font-size: 13px;
}

.doc-empty {
  margin: 0;
  color: var(--text-faint);
  font-size: 13px;
  text-align: center;
  padding: 50px 0;
}

.plain-pre {
  margin: 0;
  padding: 16px;
  border-radius: 8px;
  background: var(--bg-soft);
  color: var(--text-main);
  font-family: 'Consolas', 'Source Code Pro', monospace;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-all;
}

/* 弹窗过渡 */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-active .modal-card,
.modal-leave-active .modal-card {
  transition: transform 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-card {
  transform: scale(0.96) translateY(8px);
}

.modal-leave-to .modal-card {
  transform: scale(0.96) translateY(8px);
}

@media (max-width: 1100px) {
  .filter-item-wide {
    min-width: 0;
    width: 100%;
  }
  .filter-actions {
    margin-left: 0;
  }
  .info-desc {
    display: none;
  }
}
</style>