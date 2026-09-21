<template>
  <div class="knowledge-shell">
    <!-- 左侧：知识库文件列表 -->
    <aside class="knowledge-sidebar">
      <div class="sidebar-head">
        <div class="head-title">
          <svg viewBox="0 0 24 24" fill="none" width="16" height="16" aria-hidden="true">
            <path d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span>知识库文档</span>
        </div>
        <span class="head-count">{{ filteredFiles.length }} / {{ knowledgeFiles.length }}</span>
      </div>

      <div class="sidebar-actions">
        <input
          ref="uploadInput"
          type="file"
          accept=".md,.txt,.pdf,.docx,.doc"
          class="hidden-input"
          @change="handleUpload"
        />
        <button class="upload-btn" type="button" :disabled="uploading" @click="uploadInput && uploadInput.click()">
          <svg v-if="!uploading" viewBox="0 0 24 24" fill="none" width="15" height="15" aria-hidden="true">
            <path d="M12 4v16m8-8H4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" width="15" height="15" class="spin" aria-hidden="true">
            <path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          {{ uploading ? '上传中...' : '上传文件' }}
        </button>
        <button class="refresh-btn" type="button" :disabled="loadingList" @click="loadDocuments">
          <svg :class="['icon', loadingList && 'spin']" viewBox="0 0 24 24" fill="none" width="14" height="14" aria-hidden="true">
            <path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          刷新
        </button>
      </div>

      <div class="search-wrap">
        <svg viewBox="0 0 24 24" fill="none" width="14" height="14" class="search-icon" aria-hidden="true">
          <path d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="搜索文件..."
          class="search-input"
        >
      </div>

      <div class="file-list">
        <div v-if="loadingList" class="list-state">
          <div class="spin-ring"></div>
          <span>加载中...</span>
        </div>
        <div v-else-if="filteredFiles.length === 0" class="list-state empty-state">
          <svg viewBox="0 0 24 24" fill="none" width="32" height="32" class="empty-icon" aria-hidden="true">
            <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <p>{{ knowledgeFiles.length === 0 ? '暂无知识库文档' : '未找到匹配的文件' }}</p>
          <small>{{ knowledgeFiles.length === 0 ? '点击上方按钮上传文档' : '尝试其他关键词' }}</small>
        </div>
        <button
          v-for="file in filteredFiles"
          v-else
          :key="file.id"
          type="button"
          class="file-item"
          :class="{ active: selectedFile && selectedFile.id === file.id }"
          @click="selectFile(file)"
        >
          <span class="file-icon" :class="file.type">
            <svg v-if="file.type === 'word'" viewBox="0 0 24 24" fill="none" width="14" height="14" aria-hidden="true">
              <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <svg v-else-if="file.type === 'pdf'" viewBox="0 0 24 24" fill="none" width="14" height="14" aria-hidden="true">
              <path d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <svg v-else-if="file.type === 'md'" viewBox="0 0 24 24" fill="none" width="14" height="14" aria-hidden="true">
              <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" width="14" height="14" aria-hidden="true">
              <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </span>
          <span class="file-info">
            <span class="file-name">{{ file.name }}</span>
            <span class="file-meta">{{ file.typeLabel }}<span v-if="file.size"> · {{ file.size }}</span></span>
          </span>
        </button>
      </div>
    </aside>

    <!-- 右侧：内容预览区 -->
    <section class="preview-panel">
      <template v-if="selectedFile">
        <header class="preview-head">
          <div class="head-info">
            <span class="file-icon" :class="selectedFile.type">
              <svg v-if="selectedFile.type === 'pdf'" viewBox="0 0 24 24" fill="none" width="14" height="14" aria-hidden="true">
                <path d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <svg v-else-if="selectedFile.type === 'word'" viewBox="0 0 24 24" fill="none" width="14" height="14" aria-hidden="true">
                <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" width="14" height="14" aria-hidden="true">
                <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </span>
            <span class="title-text">{{ selectedFile.name }}</span>
          </div>
          <div class="head-actions">
            <div class="head-meta">
              <span class="meta-tag">{{ previewTypeLabel }}</span>
              <span v-if="selectedFile.updatedAt" class="meta-divider">|</span>
              <span v-if="selectedFile.updatedAt" class="meta-tag">{{ selectedFile.updatedAt }}</span>
            </div>
            <a
              v-if="selectedFile.webUrl"
              :href="selectedFile.webUrl"
              target="_blank"
              class="action-btn download"
            >
              <svg viewBox="0 0 24 24" fill="none" width="13" height="13" aria-hidden="true">
                <path d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              下载
            </a>
            <button
              type="button"
              class="action-btn delete"
              :disabled="deleting"
              @click="handleDelete"
            >
              <svg viewBox="0 0 24 24" fill="none" width="13" height="13" aria-hidden="true">
                <path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              {{ deleting ? '删除中...' : '删除' }}
            </button>
          </div>
        </header>

        <div class="preview-body">
          <div v-if="loadingPreview" class="preview-loading">
            <div class="spin-ring"></div>
            <span>加载文件内容...</span>
          </div>

          <iframe
            v-else-if="selectedFile.type === 'pdf' && selectedFile.previewUrl"
            :src="selectedFile.previewUrl"
            class="preview-iframe"
          ></iframe>

          <div v-else-if="selectedFile.type === 'pdf'" class="preview-empty">
            <svg viewBox="0 0 24 24" fill="none" width="56" height="56" class="empty-icon" aria-hidden="true">
              <path d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <p class="empty-text">PDF 文件来自知识库</p>
            <a v-if="selectedFile.webUrl" :href="selectedFile.webUrl" target="_blank" class="empty-link">点击下载查看</a>
          </div>

          <pre v-else-if="selectedFile.type === 'txt' && selectedFile.content" class="preview-text">{{ selectedFile.content }}</pre>

          <div v-else-if="renderedContent" class="preview-markdown prose" v-html="renderedContent"></div>

          <div v-else class="preview-empty">
            <svg viewBox="0 0 24 24" fill="none" width="56" height="56" class="empty-icon" aria-hidden="true">
              <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <p class="empty-text">知识库文件无法直接预览</p>
            <a v-if="selectedFile.webUrl" :href="selectedFile.webUrl" target="_blank" class="empty-link">点击下载查看</a>
          </div>
        </div>
      </template>

      <div v-else class="preview-placeholder">
        <div class="placeholder-icon">
          <svg viewBox="0 0 24 24" fill="none" width="42" height="42" aria-hidden="true">
            <path d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <h4 class="placeholder-title">项目知识库</h4>
        <p class="placeholder-desc">请从左侧列表选择一个文件进行预览</p>
      </div>
    </section>

    <!-- 上传加载遮罩 -->
    <transition name="modal">
      <div v-if="uploading" class="upload-mask">
        <div class="mask-card">
          <div class="spin-ring"></div>
          <span>正在上传文件到知识库...</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'
import mammoth from 'mammoth'
import {
  createKnowledgeDocument,
  deleteKnowledgeDocuments,
  selectKnowledgeDocuments
} from '@/api/dailyReport'

const uploadInput = ref(null)
const searchKeyword = ref('')
const selectedFile = ref(null)
const uploading = ref(false)
const deleting = ref(false)
const loadingList = ref(false)
const loadingPreview = ref(false)
const knowledgeFiles = ref([])

const TYPE_LABELS = { word: 'Word 文档', pdf: 'PDF 文档', txt: '纯文本', md: 'Markdown' }

const previewTypeLabel = computed(() => {
  if (!selectedFile.value) return ''
  return TYPE_LABELS[selectedFile.value.type] || '文档'
})

const filteredFiles = computed(() => {
  if (!searchKeyword.value.trim()) return knowledgeFiles.value
  const kw = searchKeyword.value.trim().toLowerCase()
  return knowledgeFiles.value.filter((f) => f.name.toLowerCase().includes(kw))
})

const renderedContent = computed(() => {
  if (!selectedFile.value || !selectedFile.value.content) return ''
  try {
    if (selectedFile.value.type === 'word' || selectedFile.value.type === 'md') {
      return marked.parse(selectedFile.value.content)
    }
  } catch (e) {
    return '<p class="render-error">内容渲染失败</p>'
  }
  return ''
})

const formatSize = (bytes) => {
  if (!bytes || bytes <= 0) return ''
  if (bytes > 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / 1024).toFixed(1) + ' KB'
}

const formatTimestamp = (ts) => {
  if (!ts) return ''
  const value = String(ts).length > 10 ? Math.floor(Number(ts) / 1000) : Number(ts)
  const date = new Date(value * 1000)
  if (Number.isNaN(date.getTime())) return ''
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const mapFileType = (type) => {
  const map = { docx: 'word', doc: 'word', pdf: 'pdf', txt: 'txt', md: 'md' }
  return map[(type || '').toLowerCase()] || 'other'
}

const buildFileObject = (doc) => {
  const type = mapFileType(doc.type)
  return {
    id: doc.documentId,
    documentId: doc.documentId,
    name: doc.name,
    type,
    typeLabel: TYPE_LABELS[type] || '文档',
    size: doc.size > 0 ? formatSize(doc.size) : '',
    updatedAt: formatTimestamp(doc.updateTime || doc.createTime),
    content: '',
    webUrl: doc.webUrl || '',
    previewUrl: '',
    status: doc.status
  }
}

const loadDocuments = async () => {
  if (loadingList.value) return
  loadingList.value = true
  try {
    const res = await selectKnowledgeDocuments({ page: 1, size: 100 })
    if (res && res.code === 0) {
      const docs = (res.data && res.data.documents) || []
      knowledgeFiles.value = docs.map(buildFileObject)
    } else {
      ElMessage.warning('加载知识库文档失败: ' + (res?.msg || '未知错误'))
    }
  } catch (error) {
    console.error('加载知识库文档失败:', error)
  } finally {
    loadingList.value = false
  }
}

const readFileAsBase64 = (file) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const result = String(e.target.result || '')
      const base64 = result.includes(',') ? result.split(',')[1] : result
      resolve(base64)
    }
    reader.onerror = () => reject(new Error('读取文件失败'))
    reader.readAsDataURL(file)
  })

const readFileAsText = (file) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => resolve(String(e.target.result || ''))
    reader.onerror = () => reject(new Error('读取文件失败'))
    reader.readAsText(file)
  })

const readFileAsArrayBuffer = (file) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => resolve(e.target.result)
    reader.onerror = () => reject(new Error('读取文件失败'))
    reader.readAsArrayBuffer(file)
  })

const handleUpload = async (event) => {
  const file = event.target.files && event.target.files[0]
  if (!file) return

  const ext = file.name.split('.').pop().toLowerCase()
  const typeMap = { md: 'md', txt: 'txt', pdf: 'pdf', docx: 'word', doc: 'word' }
  const fileType = typeMap[ext]
  if (!fileType) {
    ElMessage.warning('不支持的文件格式，请上传 Word / PDF / TXT / MD 文件')
    if (uploadInput.value) uploadInput.value.value = ''
    return
  }

  uploading.value = true
  try {
    const newFile = {
      id: `tmp-${Date.now()}`,
      documentId: '',
      name: file.name,
      type: fileType,
      typeLabel: TYPE_LABELS[fileType],
      size: formatSize(file.size),
      updatedAt: new Date().toISOString().split('T')[0],
      content: '',
      webUrl: '',
      previewUrl: '',
      status: 1
    }

    if (fileType === 'pdf') {
      newFile.previewUrl = URL.createObjectURL(file)
    } else if (fileType === 'word') {
      const arrayBuffer = await readFileAsArrayBuffer(file)
      const result = await mammoth.convertToHtml({ arrayBuffer })
      newFile.content = result.value || ''
    } else if (fileType === 'txt' || fileType === 'md') {
      newFile.content = await readFileAsText(file)
    }

    const fileBase64 = await readFileAsBase64(file)
    const res = await createKnowledgeDocument({
      name: file.name,
      fileBase64,
      fileType: ext === 'docx' ? 'docx' : ext === 'doc' ? 'doc' : ext,
      formatType: 0,
      chunkType: 0
    })

    if (res && res.code === 0) {
      const docInfos = (res.data && res.data.documentInfos) || []
      if (docInfos.length > 0) {
        newFile.id = docInfos[0].documentId
        newFile.documentId = docInfos[0].documentId
        if (docInfos[0].webUrl) newFile.webUrl = docInfos[0].webUrl
      }
      knowledgeFiles.value.unshift(newFile)
      selectedFile.value = newFile
      ElMessage.success('文件上传成功')
    } else {
      ElMessage.error('上传失败: ' + (res?.msg || '未知错误'))
    }
  } catch (error) {
    console.error('文件上传失败:', error)
    ElMessage.error('文件上传失败: ' + (error.message || '未知错误'))
  } finally {
    uploading.value = false
    if (uploadInput.value) uploadInput.value.value = ''
  }
}

const handleDelete = async () => {
  if (!selectedFile.value) return
  try {
    await ElMessageBox.confirm(
      `确定要删除"${selectedFile.value.name}"吗？此操作不可恢复。`,
      '确认删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )

    deleting.value = true
    const documentId = selectedFile.value.documentId || selectedFile.value.id

    const res = await deleteKnowledgeDocuments({ documentIds: [documentId] })
    if (res && res.code === 0) {
      if (selectedFile.value.previewUrl && selectedFile.value.previewUrl.startsWith('blob:')) {
        URL.revokeObjectURL(selectedFile.value.previewUrl)
      }
      knowledgeFiles.value = knowledgeFiles.value.filter((f) => f.id !== selectedFile.value.id)
      selectedFile.value = knowledgeFiles.value.length > 0 ? knowledgeFiles.value[0] : null
      ElMessage.success('文件已删除')
    } else {
      ElMessage.error('删除失败: ' + (res?.msg || '未知错误'))
    }
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      console.error('删除文件失败:', e)
      ElMessage.error('删除失败: ' + (e.message || '未知错误'))
    }
  } finally {
    deleting.value = false
  }
}

const selectFile = async (file) => {
  selectedFile.value = file

  if (file.type === 'pdf' && file.webUrl && !file.previewUrl) {
    file.previewUrl = file.webUrl
    return
  }

  if ((file.type === 'txt' || file.type === 'md') && file.webUrl && !file.content) {
    loadingPreview.value = true
    try {
      const res = await fetch(file.webUrl)
      if (!res.ok) throw new Error('HTTP ' + res.status)
      const text = await res.text()
      file.content = text
    } catch (err) {
      console.warn('加载文件内容失败:', err)
    } finally {
      loadingPreview.value = false
    }
    return
  }

  if (file.type === 'word' && file.webUrl && !file.content) {
    loadingPreview.value = true
    try {
      const res = await fetch(file.webUrl)
      if (!res.ok) throw new Error('HTTP ' + res.status)
      const arrayBuffer = await res.arrayBuffer()
      const result = await mammoth.convertToHtml({ arrayBuffer })
      if (result.value) {
        file.content = result.value
      }
    } catch (err) {
      console.warn('Word 文件转换失败:', err)
    } finally {
      loadingPreview.value = false
    }
  }
}

onMounted(() => {
  loadDocuments()
})
</script>

<style scoped>
.knowledge-shell {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 0;
  gap: 14px;
  padding: 14px 18px 18px;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ---------- 左侧栏 ---------- */
.knowledge-sidebar {
  display: flex;
  width: 290px;
  flex-shrink: 0;
  flex-direction: column;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-white);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.sidebar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 12px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-soft);
}

.head-title {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--text-strong);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.4px;
}

.head-title svg {
  color: var(--primary-text);
}

.head-count {
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--primary-tint);
  color: var(--primary-text);
  font-size: 11px;
  font-weight: 600;
}

.sidebar-actions {
  display: flex;
  gap: 8px;
  padding: 10px 14px 8px;
}

.hidden-input {
  display: none;
}

.upload-btn,
.refresh-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 12px;
  border: 1px solid transparent;
  border-radius: 9px;
  font-family: inherit;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.upload-btn {
  flex: 1;
  border-color: transparent;
  background: var(--primary-grad);
  color: #fff;
  box-shadow: 0 5px 12px rgba(176, 123, 62, 0.18);
}

.upload-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 7px 16px rgba(176, 123, 62, 0.25);
}

.upload-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.refresh-btn {
  border-color: var(--border);
  background: var(--bg-white);
  color: var(--text-secondary);
}

.refresh-btn:hover:not(:disabled) {
  border-color: var(--primary-soft);
  background: var(--bg-soft);
  color: var(--primary-text);
}

.refresh-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.refresh-btn .icon {
  transition: transform 0.4s ease;
}

.refresh-btn:hover:not(:disabled) .icon {
  transform: rotate(180deg);
}

.search-wrap {
  position: relative;
  padding: 4px 14px 10px;
}

.search-icon {
  position: absolute;
  left: 24px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-faint);
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 8px 10px 8px 30px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-soft);
  color: var(--text-main);
  font-family: inherit;
  font-size: 12px;
  transition: all 0.2s ease;
}

.search-input::placeholder {
  color: var(--text-faint);
}

.search-input:focus {
  outline: none;
  border-color: var(--primary-soft);
  background: var(--bg-white);
  box-shadow: 0 0 0 3px rgba(176, 123, 62, 0.12);
}

.file-list {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 4px 10px 12px;
}

.file-list::-webkit-scrollbar {
  width: 5px;
}

.file-list::-webkit-scrollbar-thumb {
  border-radius: 4px;
  background: rgba(176, 123, 62, 0.2);
}

.list-state {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: center;
  justify-content: center;
  padding: 40px 14px;
  color: var(--text-faint);
  font-size: 12px;
  text-align: center;
}

.list-state.empty-state {
  color: var(--text-muted);
}

.empty-state .empty-icon {
  color: var(--text-faint);
}

.list-state p {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 12px;
}

.list-state small {
  color: var(--text-faint);
  font-size: 10px;
}

.file-item {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
  padding: 9px 10px;
  border: 1px solid transparent;
  border-radius: 9px;
  background: transparent;
  color: var(--text-secondary);
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: all 0.18s ease;
}

.file-item:hover {
  background: var(--bg-soft);
  color: var(--text-main);
}

.file-item.active {
  border-color: rgba(176, 123, 62, 0.2);
  background: var(--primary-tint);
  color: var(--primary-text);
}

.file-icon {
  display: inline-flex;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: var(--bg-muted);
  color: var(--text-muted);
}

.file-icon.word {
  background: rgba(90, 143, 212, 0.14);
  color: var(--info);
}

.file-icon.pdf {
  background: var(--danger-tint);
  color: var(--danger);
}

.file-icon.md {
  background: rgba(176, 123, 62, 0.14);
  color: var(--primary-text);
}

.file-icon.txt {
  background: var(--bg-muted);
  color: var(--text-muted);
}

.file-icon.other {
  background: var(--bg-muted);
  color: var(--text-faint);
}

.file-item.active .file-icon {
  background: rgba(255, 255, 255, 0.6);
}

.file-info {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.file-name {
  overflow: hidden;
  color: inherit;
  font-size: 12.5px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  color: var(--text-faint);
  font-size: 10px;
}

.file-item.active .file-meta {
  color: var(--primary-text);
  opacity: 0.7;
}

/* ---------- 右侧预览区 ---------- */
.preview-panel {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-white);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 18px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-soft);
}

.head-info {
  display: flex;
  min-width: 0;
  flex: 1;
  align-items: center;
  gap: 9px;
}

.title-text {
  overflow: hidden;
  color: var(--text-strong);
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.head-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-faint);
  font-size: 11px;
}

.meta-tag {
  font-size: 11px;
}

.meta-divider {
  color: var(--border-strong);
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 11px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-white);
  font-family: inherit;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.18s ease;
}

.action-btn.download {
  color: var(--info);
}

.action-btn.download:hover {
  border-color: var(--info);
  background: var(--info-tint);
}

.action-btn.delete {
  color: var(--danger);
}

.action-btn.delete:hover:not(:disabled) {
  border-color: var(--danger);
  background: var(--danger-tint);
}

.action-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.preview-body {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg-white);
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: 0;
  background: #fff;
}

.preview-text {
  flex: 1;
  margin: 0;
  padding: 22px 26px;
  border: 0;
  background: var(--bg-soft);
  color: var(--text-main);
  font-family: 'Consolas', 'Source Code Pro', monospace;
  font-size: 13px;
  line-height: 1.85;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-y: auto;
}

.preview-loading {
  display: flex;
  flex: 1;
  gap: 10px;
  align-items: center;
  justify-content: center;
  color: var(--text-faint);
  font-size: 13px;
}

.preview-empty {
  display: flex;
  flex: 1;
  gap: 10px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  color: var(--text-faint);
}

.preview-empty .empty-icon {
  color: var(--border-strong);
}

.empty-text {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
}

.empty-link {
  color: var(--primary-text);
  font-size: 12px;
  text-decoration: none;
}

.empty-link:hover {
  text-decoration: underline;
}

.preview-placeholder {
  display: flex;
  flex: 1;
  gap: 12px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
}

.placeholder-icon {
  display: flex;
  width: 80px;
  height: 80px;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(201, 141, 46, 0.18), rgba(201, 141, 46, 0.08));
  color: var(--warning);
}

.placeholder-title {
  margin: 0;
  color: var(--text-strong);
  font-size: 15px;
  font-weight: 700;
}

.placeholder-desc {
  margin: 0;
  color: var(--text-faint);
  font-size: 12px;
}

/* ---------- Markdown 渲染 ---------- */
.prose {
  flex: 1;
  max-width: none;
  padding: 22px 28px;
  overflow-y: auto;
  color: var(--text-main);
  font-size: 13px;
  line-height: 1.85;
}

.prose :deep(h1) {
  margin: 1.1rem 0 0.5rem;
  padding-bottom: 0.4rem;
  border-bottom: 2px solid var(--border);
  color: var(--text-strong);
  font-size: 1.4rem;
  font-weight: 700;
}

.prose :deep(h2) {
  margin: 1rem 0 0.4rem;
  color: var(--text-strong);
  font-size: 1.15rem;
  font-weight: 600;
}

.prose :deep(h3) {
  margin: 0.75rem 0 0.25rem;
  color: var(--text-strong);
  font-size: 1rem;
  font-weight: 600;
}

.prose :deep(p) {
  margin-bottom: 0.5rem;
  color: var(--text-main);
  line-height: 1.85;
}

.prose :deep(ul),
.prose :deep(ol) {
  margin-bottom: 0.5rem;
  padding-left: 1.6rem;
}

.prose :deep(li) {
  margin-bottom: 0.2rem;
  color: var(--text-main);
  line-height: 1.85;
}

.prose :deep(strong) {
  color: var(--text-strong);
  font-weight: 600;
}

.prose :deep(a) {
  color: var(--primary-text);
  text-decoration: underline;
}

.prose :deep(blockquote) {
  margin: 0.5rem 0;
  padding: 0.5rem 1rem;
  border-left: 3px solid var(--primary-soft);
  border-radius: 0 8px 8px 0;
  background: var(--bg-soft);
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.prose :deep(table) {
  width: 100%;
  margin: 0.5rem 0;
  border-collapse: collapse;
  font-size: 0.875rem;
}

.prose :deep(th),
.prose :deep(td) {
  padding: 8px 12px;
  border: 1px solid var(--border);
  text-align: left;
}

.prose :deep(th) {
  background: var(--bg-muted);
  color: var(--text-strong);
  font-weight: 600;
}

.prose :deep(code) {
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
  background: var(--bg-muted);
  color: var(--danger);
  font-size: 0.85em;
}

.prose :deep(pre) {
  margin: 0.6rem 0;
  padding: 14px 16px;
  border-radius: 8px;
  background: #1f2937;
  overflow-x: auto;
}

.prose :deep(pre code) {
  padding: 0;
  background: transparent;
  color: #e2e8f0;
  font-size: 0.85rem;
}

.prose :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
}

.prose :deep(.render-error) {
  color: var(--danger);
}

/* ---------- 旋转加载圈 ---------- */
.spin-ring {
  width: 22px;
  height: 22px;
  border: 2px solid var(--border);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.spin {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ---------- 上传遮罩 ---------- */
.upload-mask {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.32);
  backdrop-filter: blur(2px);
  z-index: 2000;
}

.mask-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 28px;
  border-radius: 12px;
  background: var(--bg-white);
  color: var(--text-main);
  font-size: 13px;
  box-shadow: var(--shadow);
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

@media (max-width: 1100px) {
  .knowledge-sidebar {
    width: 240px;
  }

  .head-meta {
    display: none;
  }
}
</style>