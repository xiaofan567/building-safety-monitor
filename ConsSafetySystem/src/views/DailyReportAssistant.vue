<template>
  <div class="assistant-layout">
    <aside class="conversation-panel">
      <div class="new-chat-wrap">
        <button class="new-chat-btn" type="button" :disabled="creating" @click="newChat">
          <svg viewBox="0 0 24 24" fill="none" width="17" height="17" aria-hidden="true">
            <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          {{ creating ? '创建中...' : '新对话' }}
        </button>
      </div>

      <div class="history-head">
        <span>历史对话</span>
        <button class="refresh-btn" type="button" title="刷新会话" :disabled="loadingConversations" @click="loadConversations">
          <svg viewBox="0 0 24 24" fill="none" width="15" height="15" aria-hidden="true">
            <path d="M20 11a8.1 8.1 0 00-15.5-2M4 4v5h5M4 13a8.1 8.1 0 0015.5 2M20 20v-5h-5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>

      <div class="conversation-list">
        <button
          v-for="conv in conversations"
          :key="conv.id"
          class="conversation-item"
          :class="{ active: sameConversationId(activeConvId, conv.id) }"
          type="button"
          @click="switchChat(conv.id)"
        >
          <span class="conversation-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" width="16" height="16">
              <path d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </span>
          <span class="conversation-copy">
            <span class="conversation-title">{{ conv.title || '新对话' }}</span>
            <span class="conversation-time">{{ conv.time || '刚刚' }}</span>
          </span>
          <span class="delete-btn" role="button" tabindex="0" title="删除对话" @click.stop="deleteConv(conv.id)" @keydown.enter.stop="deleteConv(conv.id)">
            <svg viewBox="0 0 24 24" fill="none" width="15" height="15" aria-hidden="true">
              <path d="M4 7h16M9 7V5h6v2M8 11v6M12 11v6M16 11v6M6 7l1 13h10l1-13" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </span>
        </button>

        <div v-if="loadingConversations" class="list-state">正在加载会话...</div>
        <div v-else-if="conversations.length === 0" class="list-state empty-state">
          <span>暂无历史对话</span>
          <small>点击上方按钮开始提问</small>
        </div>
      </div>

      <div class="conversation-footnote">
        <i></i>
        对话记录仅保存在当前设备
      </div>
    </aside>

    <section class="chat-panel">
      <div v-if="messages.length === 0 && !typing" class="welcome-area">
        <div class="welcome-card">
          <div class="assistant-mark">
            <svg viewBox="0 0 24 24" fill="none" width="30" height="30" aria-hidden="true">
              <path d="M7 8h10M7 12h6M9 18H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v10a2 2 0 01-2 2h-5l-5 5v-5z" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M17 5.5v3M15.5 7h3" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
            </svg>
          </div>
          <span class="welcome-kicker">AI SAFETY ASSISTANT</span>
          <h3>智能监测助手</h3>
          <p>我可以帮您分析日报数据、解读报警信息、查询施工进度和设备状态。</p>

          <div class="quick-grid">
            <button
              v-for="(item, index) in quickQuestions"
              :key="item"
              class="quick-card"
              type="button"
              @click="sendQuick(item)"
            >
              <span class="quick-index">{{ String(index + 1).padStart(2, '0') }}</span>
              <span class="quick-text">{{ item }}</span>
              <svg viewBox="0 0 24 24" fill="none" width="15" height="15" aria-hidden="true">
                <path d="M5 12h14M14 7l5 5-5 5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>
        </div>
      </div>

      <div v-else ref="chatContainer" class="message-list">
        <div class="message-inner">
          <div v-for="(msg, idx) in messages" :key="msg.id || idx" class="message-row" :class="msg.role">
            <div class="message-avatar" :class="msg.role">
              <svg v-if="msg.role === 'user'" viewBox="0 0 24 24" fill="none" width="17" height="17" aria-hidden="true">
                <circle cx="12" cy="8" r="3.2" stroke="currentColor" stroke-width="1.7"/>
                <path d="M5.5 20a6.5 6.5 0 0113 0" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" width="18" height="18" aria-hidden="true">
                <path d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div class="message-bubble" :class="msg.role">
              <div v-if="msg.role === 'assistant'" class="markdown-body" v-html="msg.html || renderMarkdown(msg.text)"></div>
              <div v-else class="user-text">{{ msg.text }}</div>
            </div>
          </div>

          <div v-if="typing" class="message-row assistant">
            <div class="message-avatar assistant">
              <svg viewBox="0 0 24 24" fill="none" width="18" height="18" aria-hidden="true">
                <path d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div class="message-bubble assistant typing-bubble" aria-label="助手正在输入">
              <span class="typing-dot"></span>
              <span class="typing-dot"></span>
              <span class="typing-dot"></span>
            </div>
          </div>
        </div>
      </div>

      <div class="composer-area">
        <div class="composer" :class="{ focused: composerFocused }">
          <textarea
            v-model="inputText"
            class="composer-input"
            rows="1"
            :disabled="typing"
            placeholder="输入您的问题，按 Enter 发送..."
            @focus="composerFocused = true"
            @blur="composerFocused = false"
            @keydown.enter.exact.prevent="sendMessage"
          ></textarea>
          <button class="send-btn" type="button" :disabled="!inputText.trim() || typing" @click="sendMessage">
            <span>{{ typing ? '生成中' : '发送' }}</span>
            <svg viewBox="0 0 24 24" fill="none" width="16" height="16" aria-hidden="true">
              <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
        <p class="composer-hint">Enter 发送，Shift + Enter 换行</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import {
  chatInConversation,
  createConversation,
  deleteConversation,
  listConversations
} from '@/api/dailyReport'

const MSG_STORAGE_KEY = 'daily_report_assistant_messages'

const chatContainer = ref(null)
const inputText = ref('')
const typing = ref(false)
const messages = ref([])
const activeConvId = ref(null)
const conversations = ref([])
const loadingConversations = ref(false)
const creating = ref(false)
const composerFocused = ref(false)

const quickQuestions = [
  '今天工地日报汇总',
  '最近一周的安全报警情况',
  '塔吊设备运行状况如何',
  'PM2.5 超标了怎么处理',
  '生成今天的日报',
  '工地人员出勤统计'
]

const renderMarkdown = (text) => {
  if (!text) return ''
  marked.setOptions({ gfm: true, breaks: true })
  return String(marked.parse(String(text)))
}

const sameConversationId = (left, right) => String(left ?? '') === String(right ?? '')

const loadMessagesCache = () => {
  try {
    const parsed = JSON.parse(localStorage.getItem(MSG_STORAGE_KEY) || '{}')
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch (e) {
    return {}
  }
}

const saveMessagesCache = (cache) => {
  try {
    localStorage.setItem(MSG_STORAGE_KEY, JSON.stringify(cache))
  } catch (e) {
    // 本地缓存不可用时不影响当前会话继续使用。
  }
}

const parseDate = (value) => {
  if (value instanceof Date) return value
  if (!value) return new Date()
  const date = new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? new Date() : date
}

const formatTime = (value) => {
  const date = parseDate(value)
  const diffMin = Math.floor((Date.now() - date.getTime()) / 60000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin}分钟前`
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return `${diffHour}小时前`
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${month}-${day}`
}

const normalizeMessage = (message) => {
  const role = message?.role === 'user' ? 'user' : 'assistant'
  const text = String(message?.text ?? '')
  return {
    ...message,
    role,
    text,
    html: role === 'assistant' ? message?.html || renderMarkdown(text) : undefined
  }
}

const syncCurrentConv = () => {
  const index = conversations.value.findIndex((item) => sameConversationId(item.id, activeConvId.value))
  if (index === -1) return

  conversations.value[index].messages = messages.value.map(normalizeMessage)
  const firstUser = conversations.value[index].messages.find((message) => message.role === 'user')
  if (firstUser?.text) {
    conversations.value[index].title = firstUser.text.length > 24
      ? `${firstUser.text.slice(0, 24)}...`
      : firstUser.text
  }

  const cache = loadMessagesCache()
  cache[String(activeConvId.value)] = conversations.value[index].messages
  saveMessagesCache(cache)
}

const scrollToBottom = async (smooth = false) => {
  await nextTick()
  const container = chatContainer.value
  if (!container) return
  container.scrollTo({
    top: container.scrollHeight,
    behavior: smooth ? 'smooth' : 'auto'
  })
}

const loadConversations = async () => {
  if (loadingConversations.value) return
  loadingConversations.value = true
  try {
    const res = await listConversations()
    if (res?.code === 0) {
      const messageCache = loadMessagesCache()
      conversations.value = (res.data || []).map((item) => ({
        id: item.id,
        conversationId: item.conversationId,
        title: item.title || '新对话',
        time: formatTime(item.updatedAt || item.createdAt),
        messages: (messageCache[String(item.id)] || []).map(normalizeMessage)
      }))
    }
  } catch (e) {
    // 请求拦截器已经展示具体错误。
  } finally {
    loadingConversations.value = false
  }
}

const newChat = async () => {
  if (creating.value) return false
  creating.value = true
  try {
    const res = await createConversation()
    if (res?.code !== 0) {
      ElMessage.error('创建会话失败: ' + (res?.msg || '未知错误'))
      return false
    }

    const conversation = {
      id: res.data?.localId,
      conversationId: res.data?.conversationId,
      title: '新对话',
      time: '刚刚',
      messages: []
    }
    conversations.value.unshift(conversation)
    activeConvId.value = conversation.id
    messages.value = []
    inputText.value = ''
    await nextTick()
    return true
  } catch (e) {
    return false
  } finally {
    creating.value = false
  }
}

const switchChat = async (id) => {
  if (sameConversationId(id, activeConvId.value)) return
  activeConvId.value = id
  const conversation = conversations.value.find((item) => sameConversationId(item.id, id))
  messages.value = (conversation?.messages || []).map(normalizeMessage)
  inputText.value = ''
  await scrollToBottom()
}

const deleteConv = async (id) => {
  try {
    await deleteConversation(id)
  } catch (e) {
    return
  }

  conversations.value = conversations.value.filter((item) => !sameConversationId(item.id, id))
  const cache = loadMessagesCache()
  delete cache[String(id)]
  saveMessagesCache(cache)

  if (sameConversationId(activeConvId.value, id)) {
    if (conversations.value.length > 0) {
      activeConvId.value = null
      await switchChat(conversations.value[0].id)
    } else {
      activeConvId.value = null
      messages.value = []
    }
  }
  ElMessage.success('对话已删除')
}

const parseAssistantPayload = (rawAnswer) => {
  if (rawAnswer && typeof rawAnswer === 'object') {
    return {
      answer: String(rawAnswer.answer || ''),
      followUp: String(rawAnswer.follow_up || '')
    }
  }

  const rawText = String(rawAnswer || '')
  try {
    const parsed = JSON.parse(rawText)
    if (parsed && typeof parsed === 'object') {
      return {
        answer: String(parsed.answer || rawText),
        followUp: String(parsed.follow_up || '')
      }
    }
  } catch (e) {
    // 普通文本回复无需二次解析。
  }
  return { answer: rawText, followUp: '' }
}

const sendQuick = async (question) => {
  if (typing.value) return
  if (!activeConvId.value) {
    const created = await newChat()
    if (!created) return
  }
  inputText.value = question
  await sendMessage()
}

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || typing.value) return

  if (!activeConvId.value) {
    const created = await newChat()
    if (!created) return
  }

  const conversation = conversations.value.find((item) => sameConversationId(item.id, activeConvId.value))
  if (!conversation?.conversationId) {
    ElMessage.error('会话ID丢失，请重新创建对话')
    return
  }

  messages.value.push({ role: 'user', text })
  inputText.value = ''
  syncCurrentConv()
  await scrollToBottom(true)

  typing.value = true
  try {
    const res = await chatInConversation(conversation.conversationId, text)
    if (res?.code === 0) {
      const parsed = parseAssistantPayload(res.data?.answer)
      const answer = parsed.answer || '暂时没有获取到有效回复，请稍后重试。'
      messages.value.push({
        role: 'assistant',
        text: answer,
        html: renderMarkdown(answer),
        followUp: parsed.followUp
      })
    } else {
      ElMessage.error('对话失败: ' + (res?.msg || '未知错误'))
    }
  } catch (e) {
    // 请求拦截器已经展示具体错误。
  } finally {
    typing.value = false
    syncCurrentConv()
    await scrollToBottom(true)
  }
}

onMounted(async () => {
  await loadConversations()
  if (conversations.value.length > 0) {
    await switchChat(conversations.value[0].id)
  }
})
</script>

<style scoped>
.assistant-layout {
  display: flex;
  height: 100%;
  min-height: 0;
  flex: 1;
  overflow: hidden;
  background: var(--bg-page);
  color: var(--text-main);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.conversation-panel {
  display: flex;
  width: 272px;
  min-height: 0;
  flex-shrink: 0;
  flex-direction: column;
  border-right: 1px solid var(--border);
  background: var(--bg-soft);
}

.new-chat-wrap {
  padding: 16px 14px 12px;
}

.new-chat-btn {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 11px 14px;
  border: 0;
  border-radius: 11px;
  background: var(--primary-grad);
  box-shadow: 0 7px 18px rgba(176, 123, 62, 0.2);
  color: #fff;
  font-family: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

.new-chat-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 9px 22px rgba(176, 123, 62, 0.27);
}

.new-chat-btn:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 20px 9px;
  color: var(--text-faint);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1px;
}

.refresh-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s ease;
}

.refresh-btn:hover:not(:disabled) {
  border-color: var(--border);
  background: var(--bg-white);
  color: var(--primary-text);
}

.refresh-btn:disabled {
  opacity: 0.5;
}

.conversation-list {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 0 10px 12px;
}

.conversation-list::-webkit-scrollbar,
.message-list::-webkit-scrollbar {
  width: 5px;
}

.conversation-list::-webkit-scrollbar-thumb,
.message-list::-webkit-scrollbar-thumb {
  border-radius: 4px;
  background: rgba(176, 123, 62, 0.2);
}

.conversation-item {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 9px;
  margin-bottom: 4px;
  padding: 10px 9px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: var(--text-secondary);
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.conversation-item:hover {
  background: var(--bg-white);
  color: var(--text-main);
}

.conversation-item.active {
  border-color: rgba(176, 123, 62, 0.2);
  background: var(--primary-tint);
  color: var(--primary-text);
}

.conversation-icon {
  display: inline-flex;
  width: 27px;
  height: 27px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: var(--bg-muted);
  color: var(--text-muted);
}

.conversation-item.active .conversation-icon {
  background: rgba(176, 123, 62, 0.16);
  color: var(--primary-text);
}

.conversation-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 3px;
}

.conversation-title {
  overflow: hidden;
  color: inherit;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-time {
  color: var(--text-faint);
  font-size: 10px;
}

.delete-btn {
  display: inline-flex;
  width: 25px;
  height: 25px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 7px;
  color: var(--text-faint);
  opacity: 0;
  transition: all 0.18s ease;
}

.conversation-item:hover .delete-btn,
.conversation-item.active .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: var(--danger-tint);
  color: var(--danger);
}

.list-state {
  padding: 24px 12px;
  color: var(--text-faint);
  font-size: 12px;
  text-align: center;
}

.empty-state {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.empty-state small {
  color: var(--text-faint);
  font-size: 10px;
}

.conversation-footnote {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 11px 12px 13px;
  border-top: 1px solid var(--border);
  color: var(--text-faint);
  font-size: 10px;
}

.conversation-footnote i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--success);
}

.chat-panel {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  overflow: hidden;
}

.welcome-area {
  display: flex;
  min-height: 0;
  flex: 1;
  align-items: center;
  justify-content: center;
  overflow-y: auto;
  padding: 28px 30px;
  background:
    radial-gradient(circle at 76% 22%, rgba(201, 162, 107, 0.14), transparent 34%),
    linear-gradient(180deg, var(--bg-page), #f8f5ef);
}

.welcome-card {
  width: min(680px, 100%);
  text-align: center;
}

.assistant-mark {
  display: inline-flex;
  width: 64px;
  height: 64px;
  align-items: center;
  justify-content: center;
  margin-bottom: 13px;
  border: 1px solid rgba(176, 123, 62, 0.18);
  border-radius: 20px;
  background: var(--primary-grad);
  box-shadow: 0 12px 28px rgba(176, 123, 62, 0.22);
  color: #fff;
}

.welcome-kicker {
  display: block;
  margin-bottom: 6px;
  color: var(--primary-text);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 2.2px;
}

.welcome-card h3 {
  margin: 0 0 8px;
  color: var(--text-strong);
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 1px;
}

.welcome-card > p {
  margin: 0 auto 25px;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.7;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.quick-card {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
  padding: 13px 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 4px 14px rgba(120, 95, 60, 0.04);
  color: var(--text-main);
  font-family: inherit;
  font-size: 12px;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.quick-card:hover {
  border-color: var(--primary-bright);
  background: var(--bg-white);
  box-shadow: var(--shadow-sm);
  color: var(--primary-text);
  transform: translateY(-1px);
}

.quick-index {
  color: var(--primary-soft);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.6px;
}

.quick-text {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quick-card svg {
  flex-shrink: 0;
  color: var(--text-faint);
}

.quick-card:hover svg {
  color: var(--primary);
}

.message-list {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 24px 26px 18px;
}

.message-inner {
  width: min(920px, 100%);
  margin: 0 auto;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 18px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.message-avatar {
  display: flex;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
}

.message-avatar.assistant {
  background: var(--primary-grad);
  box-shadow: 0 5px 13px rgba(176, 123, 62, 0.18);
  color: #fff;
}

.message-avatar.user {
  border: 1px solid var(--border-strong);
  background: var(--bg-muted);
  color: var(--text-secondary);
}

.message-bubble {
  max-width: min(76%, 720px);
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.75;
}

.message-bubble.user {
  padding: 10px 14px;
  border-bottom-right-radius: 4px;
  background: var(--primary-grad);
  box-shadow: 0 6px 16px rgba(176, 123, 62, 0.16);
  color: #fff;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-bubble.assistant {
  padding: 13px 16px;
  border: 1px solid var(--border);
  border-bottom-left-radius: 4px;
  background: var(--bg-white);
  box-shadow: var(--shadow-sm);
  color: var(--text-main);
}

.user-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.typing-bubble {
  display: flex;
  align-items: center;
  gap: 5px;
  min-height: 43px;
  padding: 13px 17px;
}

.typing-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--primary-soft);
  animation: assistant-dot 1.2s ease-in-out infinite;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.16s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.32s;
}

@keyframes assistant-dot {
  0%, 60%, 100% {
    opacity: 0.38;
    transform: translateY(0);
  }
  30% {
    opacity: 1;
    transform: translateY(-3px);
  }
}

.composer-area {
  flex-shrink: 0;
  padding: 10px 26px 16px;
  background: linear-gradient(180deg, rgba(245, 242, 236, 0), var(--bg-page) 30%);
}

.composer {
  display: flex;
  width: min(920px, 100%);
  align-items: flex-end;
  gap: 10px;
  margin: 0 auto;
  padding: 7px 8px 7px 15px;
  border: 1px solid var(--border-strong);
  border-radius: 15px;
  background: var(--bg-white);
  box-shadow: 0 8px 24px rgba(120, 95, 60, 0.09);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.composer.focused {
  border-color: var(--primary-bright);
  box-shadow: 0 0 0 3px rgba(176, 123, 62, 0.09), 0 8px 24px rgba(120, 95, 60, 0.09);
}

.composer-input {
  min-height: 38px;
  max-height: 120px;
  min-width: 0;
  flex: 1;
  resize: none;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--text-main);
  font-family: inherit;
  font-size: 13px;
  line-height: 1.7;
}

.composer-input::placeholder {
  color: var(--text-faint);
}

.composer-input:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.send-btn {
  display: inline-flex;
  height: 38px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 14px;
  border: 0;
  border-radius: 10px;
  background: var(--primary-grad);
  color: #fff;
  font-family: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.send-btn:hover:not(:disabled) {
  box-shadow: 0 6px 15px rgba(176, 123, 62, 0.23);
  transform: translateY(-1px);
}

.send-btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.composer-hint {
  width: min(920px, 100%);
  margin: 6px auto 0;
  color: var(--text-faint);
  font-size: 10px;
  text-align: right;
}

.markdown-body {
  overflow-wrap: anywhere;
}

.markdown-body :deep(p) {
  margin: 0 0 9px;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: 14px 0 7px;
  color: var(--text-strong);
  font-weight: 800;
  line-height: 1.45;
}

.markdown-body :deep(h1:first-child),
.markdown-body :deep(h2:first-child),
.markdown-body :deep(h3:first-child),
.markdown-body :deep(h4:first-child) {
  margin-top: 0;
}

.markdown-body :deep(h1) { font-size: 18px; }
.markdown-body :deep(h2) { font-size: 16px; }
.markdown-body :deep(h3),
.markdown-body :deep(h4) { font-size: 14px; }

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 6px 0 9px;
  padding-left: 20px;
}

.markdown-body :deep(li) {
  margin: 3px 0;
}

.markdown-body :deep(strong) {
  color: var(--text-strong);
  font-weight: 750;
}

.markdown-body :deep(a) {
  color: var(--primary-text);
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

.markdown-body :deep(blockquote) {
  margin: 10px 0;
  padding: 7px 11px;
  border-left: 3px solid var(--primary-soft);
  border-radius: 0 8px 8px 0;
  background: var(--primary-tint);
  color: var(--text-secondary);
}

.markdown-body :deep(code) {
  padding: 2px 5px;
  border-radius: 5px;
  background: var(--bg-muted);
  color: var(--primary-text);
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}

.markdown-body :deep(pre) {
  overflow-x: auto;
  margin: 10px 0;
  padding: 11px 12px;
  border: 1px solid var(--border);
  border-radius: 9px;
  background: #f7f3eb;
}

.markdown-body :deep(pre code) {
  padding: 0;
  background: transparent;
}

.markdown-body :deep(table) {
  width: 100%;
  margin: 10px 0;
  border-collapse: collapse;
  font-size: 12px;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 7px 9px;
  border: 1px solid var(--border);
  text-align: left;
}

.markdown-body :deep(th) {
  background: var(--bg-muted);
  color: var(--text-secondary);
  font-weight: 700;
}

.markdown-body :deep(hr) {
  margin: 14px 0;
  border: 0;
  border-top: 1px solid var(--border);
}

@media (max-width: 1100px) {
  .conversation-panel {
    width: 228px;
  }

  .message-list {
    padding-right: 18px;
    padding-left: 18px;
  }

  .composer-area {
    padding-right: 18px;
    padding-left: 18px;
  }

  .message-bubble {
    max-width: 82%;
  }
}
</style>
