<template>
  <div class="template-page">
    <header class="action-bar">
      <div class="action-info">
        <span class="info-tag">日报模板</span>
        <span class="info-desc">使用 Markdown 编写日报模板，支持变量占位符，保存后将写入 Coze 知识库</span>
      </div>
      <div class="action-buttons">
        <button class="ghost-btn" type="button" @click="reload">
          <svg viewBox="0 0 24 24" fill="none" width="15" height="15" aria-hidden="true">
            <path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          重新加载
        </button>
        <button class="primary-btn" type="button" :disabled="saving" @click="saveForm">
          <svg viewBox="0 0 24 24" fill="none" width="15" height="15" aria-hidden="true">
            <path d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          {{ saving ? '保存中...' : '保存模板' }}
        </button>
      </div>
    </header>

    <div class="editor-board">
      <section class="editor-pane">
        <div class="pane-head">
          <div class="head-title">
            <span class="dot dot-yellow"></span>
            <span class="head-label">Markdown 编辑</span>
          </div>
          <span class="head-meta">{{ form.mdContent.length }} 字符</span>
        </div>
        <textarea
          v-model="form.mdContent"
          class="editor-textarea"
          spellcheck="false"
          placeholder="在此输入日报模板的 Markdown 内容..."
        ></textarea>
      </section>

      <section class="editor-pane">
        <div class="pane-head">
          <div class="head-title">
            <span class="dot dot-green"></span>
            <span class="head-label">实时预览</span>
          </div>
          <span class="head-meta">HTML 渲染</span>
        </div>
        <div ref="previewRef" class="preview-body prose"></div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import { addTemplate, selectTemplateList } from '@/api/dailyReport'

const previewRef = ref(null)
const saving = ref(false)

const form = reactive({
  mdContent: ''
})

let renderTimer = null

const renderPreview = () => {
  if (!previewRef.value) return
  try {
    previewRef.value.innerHTML = marked.parse(form.mdContent || '')
  } catch (e) {
    previewRef.value.innerHTML = '<p class="render-error">Markdown 解析错误</p>'
  }
}

watch(() => form.mdContent, () => {
  if (renderTimer) clearTimeout(renderTimer)
  renderTimer = setTimeout(renderPreview, 200)
})

const loadTemplate = async () => {
  try {
    const res = await selectTemplateList()
    if (res && res.promptText) {
      form.mdContent = res.promptText
      renderPreview()
    }
  } catch (e) {
    console.warn('加载日报模板失败:', e)
    ElMessage.warning('加载日报模板失败，请稍后重试')
  }
}

const reload = async () => {
  form.mdContent = ''
  await loadTemplate()
  ElMessage.success('已重新加载最新模板')
}

const saveForm = async () => {
  if (!form.mdContent.trim()) {
    ElMessage.warning('模板内容不能为空')
    return
  }
  saving.value = true
  try {
    await addTemplate({ promptText: form.mdContent })
    ElMessage.success('模板保存成功')
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadTemplate()
  renderPreview()
})
</script>

<style scoped>
.template-page {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 14px;
  padding: 16px 18px 18px;
  background: var(--bg-page);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

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

.ghost-btn:hover {
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

.primary-btn:disabled,
.ghost-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.editor-board {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  min-height: 0;
  flex: 1;
}

.editor-pane {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-white);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.pane-head {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  padding: 11px 16px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-soft);
}

.head-title {
  display: inline-flex;
  align-items: center;
  gap: 7px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot-yellow {
  background: #d8a93a;
  box-shadow: 0 0 8px rgba(216, 169, 58, 0.45);
}

.dot-green {
  background: #4d8547;
  box-shadow: 0 0 8px rgba(77, 133, 71, 0.45);
}

.head-label {
  color: var(--text-strong);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.4px;
}

.head-meta {
  color: var(--text-faint);
  font-size: 11px;
}

.editor-textarea {
  width: 100%;
  flex: 1;
  resize: none;
  border: 0;
  outline: 0;
  padding: 18px 22px;
  background: var(--bg-white);
  color: var(--text-main);
  font-family: 'Consolas', 'Source Code Pro', monospace;
  font-size: 13px;
  line-height: 1.85;
  tab-size: 2;
}

.editor-textarea::placeholder {
  color: var(--text-faint);
}

.preview-body {
  flex: 1;
  min-height: 0;
  padding: 22px 26px;
  overflow-y: auto;
}

.preview-body::-webkit-scrollbar {
  width: 5px;
}

.preview-body::-webkit-scrollbar-thumb {
  border-radius: 4px;
  background: rgba(176, 123, 62, 0.2);
}

.render-error {
  margin: 0;
  color: var(--danger);
  font-size: 13px;
}

/* ---------- Markdown 渲染样式（与知识库页保持一致） ---------- */
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
  font-weight: 700;
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

.prose :deep(hr) {
  margin: 1rem 0;
  border: 0;
  border-top: 1px solid var(--border);
}

@media (max-width: 1100px) {
  .editor-board {
    grid-template-columns: 1fr;
    min-height: auto;
  }
  .editor-pane {
    min-height: 360px;
  }
  .info-desc {
    display: none;
  }
}
</style>