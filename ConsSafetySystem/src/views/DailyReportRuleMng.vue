<template>
  <div class="rule-page">
    <header class="action-bar">
      <div class="action-info">
        <span class="info-tag">报警规则</span>
        <span class="info-desc">用 Markdown 描述报警触发条件、严重程度与推送对象，保存后写入 Coze 知识库</span>
      </div>
      <div class="action-buttons">
        <button class="ghost-btn" type="button" @click="showRefModal = true">
          <svg viewBox="0 0 24 24" fill="none" width="15" height="15" aria-hidden="true">
            <path d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          配置参考
        </button>
        <button class="ghost-btn" type="button" :disabled="loading" @click="reload">
          <svg viewBox="0 0 24 24" fill="none" width="15" height="15" aria-hidden="true">
            <path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          重新加载
        </button>
        <button class="primary-btn" type="button" :disabled="saving" @click="saveForm">
          <svg viewBox="0 0 24 24" fill="none" width="15" height="15" aria-hidden="true">
            <path d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          {{ saving ? '保存中...' : '保存规则' }}
        </button>
      </div>
    </header>

    <transition name="help">
      <div v-if="showHelp" class="help-card">
        <div class="help-head">
          <div class="help-title">
            <svg viewBox="0 0 24 24" fill="none" width="15" height="15" aria-hidden="true">
              <path d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>如何配置规则</span>
          </div>
          <button class="help-close" type="button" @click="toggleHelp">收起</button>
        </div>
        <ul class="help-list">
          <li><span class="step-no">1</span>使用 Markdown 格式编写规则，按章节划分硬性报警、关注性报警等</li>
          <li><span class="step-no">2</span>每个规则项写明触发条件和执行动作（发送报警 / 仅记录 / 忽略）</li>
          <li><span class="step-no">3</span>可点击右上角"配置参考"按钮查看《安全日报异常报警标准》</li>
          <li><span class="step-no">4</span>发送人信息建议使用 Markdown 表格格式，方便维护与预览</li>
        </ul>
      </div>
    </transition>

    <div class="editor-board">
      <section class="editor-pane">
        <div class="pane-head">
          <div class="head-title">
            <span class="dot dot-yellow"></span>
            <span class="head-label">规则编辑（Markdown）</span>
          </div>
          <button class="pane-toggle" type="button" @click="toggleHelp">
            {{ showHelp ? '收起帮助' : '如何配置？' }}
          </button>
        </div>
        <textarea
          v-model="form.ruleContent"
          class="editor-textarea"
          spellcheck="false"
          placeholder="输入报警规则内容（支持 Markdown 格式）"
        ></textarea>
      </section>

      <section class="editor-pane">
        <div class="pane-head">
          <div class="head-title">
            <span class="dot dot-green"></span>
            <span class="head-label">实时预览</span>
          </div>
          <span class="head-meta">{{ form.ruleContent.length }} 字符</span>
        </div>
        <div ref="previewRef" class="preview-body prose"></div>
      </section>
    </div>

    <!-- 配置参考弹窗 -->
    <transition name="modal">
      <div v-if="showRefModal" class="modal-mask" @click.self="showRefModal = false">
        <div class="modal-card">
          <header class="modal-head">
            <div class="modal-title">
              <span class="modal-mark">
                <svg viewBox="0 0 24 24" fill="none" width="18" height="18" aria-hidden="true">
                  <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </span>
              <div>
                <h3>安全日报异常报警标准</h3>
                <p>左侧编辑器按 Markdown 编写阈值与动作即可生效</p>
              </div>
            </div>
            <button class="modal-close" type="button" @click="showRefModal = false">
              <svg viewBox="0 0 24 24" fill="none" width="18" height="18" aria-hidden="true">
                <path d="M6 18L18 6M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </button>
          </header>

          <div class="modal-body">
            <div class="modal-tip">
              <strong>提示：</strong>此文档定义了日报报警的触发标准，可对照编写规则配置。
            </div>

            <section class="ref-section">
              <h4 class="ref-title ref-title-red">
                <span class="ref-no">一</span>
                硬性报警标准（必须发送）
              </h4>
              <div class="ref-list">
                <div v-for="(item, i) in hardAlerts" :key="i" class="ref-item ref-item-red">
                  <span class="ref-index">{{ i + 1 }}</span>
                  <div>
                    <p class="ref-item-title">{{ item.title }}</p>
                    <p class="ref-item-desc">{{ item.desc }}</p>
                  </div>
                </div>
              </div>
            </section>

            <section class="ref-section">
              <h4 class="ref-title ref-title-amber">
                <span class="ref-no">二</span>
                关注性报警标准（建议发送，需综合判断）
              </h4>
              <div class="ref-list">
                <div v-for="(item, i) in watchAlerts" :key="i" class="ref-item ref-item-amber">
                  <span class="ref-index">{{ i + 1 }}</span>
                  <div>
                    <p class="ref-item-title">{{ item.title }}</p>
                    <p class="ref-item-desc">{{ item.desc }}</p>
                  </div>
                </div>
              </div>
            </section>

            <section class="ref-section">
              <h4 class="ref-title ref-title-blue">
                <span class="ref-no">三</span>
                判断流程
              </h4>
              <div class="flow-block">
                <pre>遍历日报各模块数据
        │
        ├─ 硬性报警条件检查 ── 满足任一 ──▶ 立即触发报警
        │
        └─ 关注性报警条件检查 ── 满足任一 ──▶ 综合判断 ──▶ 决定是否触发</pre>
              </div>
            </section>

            <section class="ref-section">
              <h4 class="ref-title ref-title-green">
                <span class="ref-no">四</span>
                发送规则示例
              </h4>
              <div class="ref-table-wrap">
                <table class="ref-table">
                  <thead>
                    <tr>
                      <th>触发条件</th>
                      <th>发送给</th>
                      <th>说明</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, i) in sendRules" :key="i">
                      <td>{{ row.condition }}</td>
                      <td>{{ row.sendTo }}</td>
                      <td class="ref-note">{{ row.note }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
          </div>

          <footer class="modal-foot">
            <button class="primary-btn" type="button" @click="showRefModal = false">关闭</button>
          </footer>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import { addRule, selectRuleList } from '@/api/dailyReport'

const previewRef = ref(null)
const saving = ref(false)
const loading = ref(false)
const showHelp = ref(false)
const showRefModal = ref(false)

const hardAlerts = [
  { title: '橙色/红色设备报警', desc: 'alertType="1"（警报）或 "2"（控制报警），无论是否已处置' },
  { title: '连续报警比例过高', desc: '报警总数 > 10 起，或环比昨日增长 > 200%（昨日<3起时阈值降为100%）' },
  { title: '设备在线率严重下降', desc: 'overallOnlineRate < 70%' },
  { title: '重大环境指标超标', desc: '风速 ≥ 10.0m/s（6级）、PM2.5 ≥ 115μg/m³（重度）、TSP ≥ 500μg/m³（严重），取实时值' },
  { title: '关键设备长时间离线', desc: '塔吊/升降机/高支模/深基坑 > 30分钟；摄像头 > 60分钟' },
  { title: 'AI 识别明火报警', desc: '检测类型 = 6（工作现场明火）' }
]

const watchAlerts = [
  { title: '设备预警批量出现', desc: '同设备类型单日预警（alertType="0"）≥ 3次' },
  { title: '环境指标连续趋高', desc: 'PM2.5 连续3小时 > 75μg/m³；噪声连续3小时 > 75dB' },
  { title: 'AI违规趋势上升', desc: 'AI违规总数环比增长 > 50%；或同类违规连续3日增长 > 20%' },
  { title: '设备离线频发', desc: '同设备离线/上线切换 ≥ 3次/日；或单日离线总次数 ≥ 5次' },
  { title: '传感器数据异常', desc: '塔吊载荷=0但力矩≠0；升降机倾斜角>1.0°无对应报警；多监测点同时无数据>2小时' }
]

const sendRules = [
  { condition: '橙色/红色设备报警', sendTo: '赵强、刘志强', note: '设备管理员排查，安全总监掌握情况' },
  { condition: 'AI识别明火报警', sendTo: '王刚、张浩、刘志强', note: '明火隐患为最高风险，须全员立即响应' },
  { condition: '重大环境指标超标', sendTo: '王刚、张浩', note: '安排降尘或停工措施' },
  { condition: '设备在线率严重下降', sendTo: '赵强、刘志强', note: '多设备排查，安全总监决策' }
]

const form = reactive({
  ruleContent: ''
})

let renderTimer = null

const renderPreview = () => {
  if (!previewRef.value) return
  try {
    previewRef.value.innerHTML = marked.parse(form.ruleContent || '')
  } catch (e) {
    previewRef.value.innerHTML = '<p class="render-error">规则解析错误</p>'
  }
}

watch(() => form.ruleContent, () => {
  if (renderTimer) clearTimeout(renderTimer)
  renderTimer = setTimeout(renderPreview, 200)
})

const toggleHelp = () => {
  showHelp.value = !showHelp.value
}

const loadRule = async () => {
  loading.value = true
  try {
    const res = await selectRuleList()
    if (res && res.promptText) {
      form.ruleContent = res.promptText
    }
  } catch (e) {
    console.warn('加载报警规则失败:', e)
    ElMessage.warning('加载报警规则失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const reload = async () => {
  form.ruleContent = ''
  await loadRule()
  renderPreview()
  ElMessage.success('已重新加载最新规则')
}

const saveForm = async () => {
  if (!form.ruleContent.trim()) {
    ElMessage.warning('规则内容不能为空')
    return
  }
  saving.value = true
  try {
    await addRule({ promptText: form.ruleContent })
    ElMessage.success('规则保存成功')
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadRule()
  renderPreview()
})
</script>

<style scoped>
.rule-page {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  gap: 14px;
  padding: 16px 18px 18px;
  background: var(--bg-page);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ---------- 顶部操作栏 ---------- */
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
  background: var(--warning-tint);
  color: var(--warning);
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

.ghost-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
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

.primary-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

/* ---------- 帮助卡片 ---------- */
.help-card {
  padding: 14px 18px;
  border: 1px solid var(--info-tint);
  border-radius: 12px;
  background: var(--bg-white);
  box-shadow: var(--shadow-sm);
}

.help-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.help-title {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--info);
  font-size: 13px;
  font-weight: 700;
}

.help-close {
  border: 0;
  background: transparent;
  color: var(--text-muted);
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
}

.help-close:hover {
  color: var(--primary-text);
}

.help-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 18px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.help-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.7;
}

.step-no {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--info-tint);
  color: var(--info);
  font-size: 11px;
  font-weight: 700;
}

/* ---------- 编辑器双栏 ---------- */
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

.pane-toggle {
  border: 0;
  background: transparent;
  color: var(--primary-text);
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
}

.pane-toggle:hover {
  text-decoration: underline;
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

/* ---------- Markdown 渲染样式 ---------- */
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

.prose :deep(hr) {
  margin: 1rem 0;
  border: 0;
  border-top: 1px solid var(--border);
}

/* ---------- 配置参考弹窗 ---------- */
.modal-mask {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.32);
  backdrop-filter: blur(3px);
  z-index: 2000;
}

.modal-card {
  width: min(960px, 92vw);
  max-height: 86vh;
  display: flex;
  flex-direction: column;
  border-radius: 16px;
  background: var(--bg-white);
  box-shadow: 0 18px 50px rgba(40, 30, 15, 0.25);
  overflow: hidden;
}

.modal-head {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 22px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-soft);
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.modal-mark {
  display: inline-flex;
  width: 36px;
  height: 36px;
  align-items: center;
  justify-content: center;
  border-radius: 11px;
  background: var(--primary-tint);
  color: var(--primary-text);
}

.modal-title h3 {
  margin: 0;
  color: var(--text-strong);
  font-size: 15px;
  font-weight: 800;
}

.modal-title p {
  margin: 2px 0 0;
  color: var(--text-muted);
  font-size: 12px;
}

.modal-close {
  display: inline-flex;
  width: 30px;
  height: 30px;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
}

.modal-close:hover {
  background: var(--bg-muted);
  color: var(--text-strong);
}

.modal-body {
  flex: 1;
  padding: 22px 26px;
  overflow-y: auto;
}

.modal-body::-webkit-scrollbar {
  width: 6px;
}

.modal-body::-webkit-scrollbar-thumb {
  border-radius: 4px;
  background: rgba(176, 123, 62, 0.22);
}

.modal-tip {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  padding: 12px 16px;
  border: 1px solid var(--warning-tint);
  border-radius: 10px;
  background: rgba(201, 141, 46, 0.08);
  color: var(--text-secondary);
  font-size: 12.5px;
  line-height: 1.75;
  margin-bottom: 18px;
}

.modal-tip strong {
  color: var(--warning);
}

.ref-section {
  margin-bottom: 22px;
}

.ref-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
  font-size: 14px;
  font-weight: 700;
}

.ref-title-red { color: var(--danger); border-bottom-color: rgba(194, 91, 91, 0.2); }
.ref-title-amber { color: var(--warning); border-bottom-color: rgba(201, 141, 46, 0.22); }
.ref-title-blue { color: var(--info); border-bottom-color: rgba(90, 143, 212, 0.22); }
.ref-title-green { color: var(--success); border-bottom-color: rgba(77, 133, 71, 0.22); }

.ref-no {
  display: inline-flex;
  width: 22px;
  height: 22px;
  align-items: center;
  justify-content: center;
  border-radius: 7px;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
}

.ref-title-red .ref-no { background: var(--danger); }
.ref-title-amber .ref-no { background: var(--warning); }
.ref-title-blue .ref-no { background: var(--info); }
.ref-title-green .ref-no { background: var(--success); }

.ref-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ref-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
}

.ref-item-red { background: rgba(194, 91, 91, 0.05); border-color: rgba(194, 91, 91, 0.16); }
.ref-item-amber { background: rgba(201, 141, 46, 0.05); border-color: rgba(201, 141, 46, 0.18); }

.ref-index {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  font-size: 11px;
  font-weight: 700;
}

.ref-item-red .ref-index { background: rgba(194, 91, 91, 0.16); color: var(--danger); }
.ref-item-amber .ref-index { background: rgba(201, 141, 46, 0.16); color: var(--warning); }

.ref-item-title {
  margin: 0;
  color: var(--text-strong);
  font-size: 13px;
  font-weight: 700;
}

.ref-item-desc {
  margin: 4px 0 0;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.65;
}

.flow-block {
  padding: 16px 18px;
  border: 1px solid var(--info-tint);
  border-radius: 10px;
  background: rgba(90, 143, 212, 0.05);
}

.flow-block pre {
  margin: 0;
  color: var(--info);
  font-family: 'Consolas', 'Source Code Pro', monospace;
  font-size: 12.5px;
  line-height: 1.85;
  white-space: pre-wrap;
}

.ref-table-wrap {
  overflow-x: auto;
  border: 1px solid var(--border);
  border-radius: 10px;
}

.ref-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12.5px;
}

.ref-table thead {
  background: var(--bg-muted);
}

.ref-table th,
.ref-table td {
  padding: 10px 14px;
  border-bottom: 1px solid var(--border);
  text-align: left;
}

.ref-table th {
  color: var(--text-strong);
  font-weight: 700;
}

.ref-table tbody tr:last-child td {
  border-bottom: 0;
}

.ref-table tbody tr:nth-child(even) {
  background: var(--bg-soft);
}

.ref-note {
  color: var(--text-muted);
}

.modal-foot {
  display: flex;
  justify-content: flex-end;
  padding: 12px 22px;
  border-top: 1px solid var(--border);
  background: var(--bg-soft);
}

.modal-foot .primary-btn {
  padding: 8px 18px;
}

/* ---------- 过渡动画 ---------- */
.modal-enter-active,
.modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-active .modal-card,
.modal-leave-active .modal-card { transition: transform 0.22s ease; }
.modal-enter-from,
.modal-leave-to { opacity: 0; }
.modal-enter-from .modal-card { transform: translateY(8px) scale(0.97); }
.modal-leave-to .modal-card { transform: translateY(8px) scale(0.97); }

.help-enter-active,
.help-leave-active { transition: opacity 0.22s ease, transform 0.22s ease; }
.help-enter-from,
.help-leave-to { opacity: 0; transform: translateY(-6px); }

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
  .help-list {
    grid-template-columns: 1fr;
  }
}
</style>