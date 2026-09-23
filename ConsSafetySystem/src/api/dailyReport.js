import request from '@/utils/request'

const jsonHeaders = { 'Content-Type': 'application/json;charset=UTF-8' }

/** 创建 Coze 会话 */
export function createConversation(name = '新对话') {
  return request({
    url: '/coze/conversation/create',
    method: 'post',
    data: { name },
    headers: jsonHeaders
  })
}

/** 查看会话详情 */
export function retrieveConversation(conversationId) {
  return request({
    url: '/coze/conversation/retrieve',
    method: 'post',
    data: { conversationId },
    headers: jsonHeaders
  })
}

/** 查询当前用户的会话列表 */
export function listConversations() {
  return request({
    url: '/coze/conversation/list',
    method: 'post',
    data: {},
    headers: jsonHeaders
  })
}

/** 删除会话 */
export function deleteConversation(id) {
  return request({
    url: '/coze/conversation/delete',
    method: 'post',
    data: { id },
    headers: jsonHeaders
  })
}

/** 在会话中发起对话 */
export function chatInConversation(conversationId, message) {
  return request({
    url: '/coze/conversation/chat',
    method: 'post',
    data: { conversationId, message },
    headers: jsonHeaders
  })
}

// ==================== 知识库文档 API ====================

/**
 * 查询知识库文档列表
 * @param {Object} params - { page, size }
 * @returns {Promise<Object>} { code, msg, data: { documents, total } }
 */
export function selectKnowledgeDocuments(params) {
  return request({
    url: '/coze/knowledge/document/list',
    method: 'post',
    data: params || {},
    headers: jsonHeaders
  })
}

/**
 * 上传文件到知识库
 * @param {Object} params - { name, fileBase64, fileType, formatType, chunkType }
 * @returns {Promise<Object>} { code, msg, data: { documentInfos } }
 */
export function createKnowledgeDocument(params) {
  return request({
    url: '/coze/knowledge/document/create',
    method: 'post',
    data: params,
    headers: jsonHeaders
  })
}

/**
 * 删除知识库文档
 * @param {Object} params - { documentIds: string[] }
 * @returns {Promise<Object>} { code, msg, data: { deletedCount } }
 */
export function deleteKnowledgeDocuments(params) {
  return request({
    url: '/coze/knowledge/document/delete',
    method: 'post',
    data: params,
    headers: jsonHeaders
  })
}

// ==================== 日报模板 API ====================

/**
 * 获取日��模板（prompt_key = doc_template）
 * @returns {Promise<Object|null>} 后端返回的 { code, msg, data: { promptText } }，失败返回 null
 */
export async function selectTemplateList() {
  try {
    const response = await request({
      url: '/coze/database/getTemplate',
      method: 'post',
      data: {},
      headers: jsonHeaders
    })
    return response && response.data ? response.data : null
  } catch (error) {
    console.error('获取模板失败:', error)
    return null
  }
}

/**
 * 更新日报模板
 * @param {Object} requestData - { promptText: string }
 * @returns {Promise<Object>} 后端返回结果
 */
export async function addTemplate(requestData) {
  try {
    const response = await request({
      url: '/coze/database/updateTemplate',
      method: 'post',
      data: requestData,
      headers: jsonHeaders
    })
    return response && response.data ? response.data : null
  } catch (error) {
    console.error('更新模板失败:', error)
    throw error
  }
}

// ==================== 日报规则 API ====================

/**
 * 获取报警规则（prompt_key = rule_template）
 * @returns {Promise<Object|null>} 后端返回的 { code, msg, data: { promptText } }，失败返回 null
 */
export async function selectRuleList() {
  try {
    const response = await request({
      url: '/coze/database/getAlertConfig',
      method: 'post',
      data: {},
      headers: jsonHeaders
    })
    return response && response.data ? response.data : null
  } catch (error) {
    console.error('获取报警规则失败:', error)
    return null
  }
}

/**
 * 更新报警规则
 * @param {Object} requestData - { promptText: string }
 * @returns {Promise<Object>} 后端返回结果
 */
export async function addRule(requestData) {
  try {
    const response = await request({
      url: '/coze/database/updateAlertConfig',
      method: 'post',
      data: requestData,
      headers: jsonHeaders
    })
    return response && response.data ? response.data : null
  } catch (error) {
    console.error('更新报警规则失败:', error)
    throw error
  }
}

// ==================== 日报文档 API ====================

/**
 * 获取日报文档列表（分页）
 * @param {Object} params - { page, limit, docName?, startDate?, endDate? }
 * @returns {Promise<Object>} 后端返回 { code, msg, data: { records, total, page, limit } }
 */
export async function getDocumentList(params) {
  try {
    const response = await request({
      url: '/document/list',
      method: 'post',
      data: params || {},
      headers: {
        ...jsonHeaders,
        'Admin-Token': window.localStorage.token
      }
    })
    return response && response.data ? response.data : null
  } catch (error) {
    console.error('获取日报文档列表失败:', error)
    throw error
  }
}

/**
 * 获取日报文档文件（用于查看预览）
 * @param {string} docUrl - 文档相对路径，如 "docIdHash/original.docx"
 * @returns {Promise<Object>} axios 响应（含 Blob data）
 */
export async function getDocumentFile(docUrl) {
  try {
    const response = await request({
      url: `/doc-files/${docUrl}`,
      method: 'get',
      responseType: 'blob',
      headers: {
        'Admin-Token': window.localStorage.token
      }
    })
    return response
  } catch (error) {
    console.error('获取日报文档文件失败:', error)
    throw error
  }
}

/**
 * 获取日报文档下载完整 URL。
 * @param {string} docUrl - 文档相对路径
 * @returns {string} 完整的下载 URL
 */
export function getDocumentDownloadUrl(docUrl) {
  return `http://${import.meta.env.VITE_APP_Address}/doc-files/${docUrl}`
}

/**
 * 下载日报文档（通过 Blob 创建下载链接）。
 * @param {string} docUrl - 文档相对路径
 * @param {string} fileName - 下载的文件名
 */
export async function downloadDocumentFile(docUrl, fileName) {
  try {
    const response = await request({
      url: `/doc-files/${docUrl}`,
      method: 'get',
      responseType: 'blob',
      headers: {
        'Admin-Token': window.localStorage.token
      }
    })

    // 推断 MIME 类型
    const lower = (fileName || '').toLowerCase()
    let mime = 'application/octet-stream'
    if (lower.endsWith('.docx')) {
      mime = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    } else if (lower.endsWith('.doc')) {
      mime = 'application/msword'
    } else if (lower.endsWith('.pdf')) {
      mime = 'application/pdf'
    } else if (lower.endsWith('.md') || lower.endsWith('.txt')) {
      mime = 'text/plain'
    }

    const blob = new Blob([response.data], { type: mime })
    const downloadUrl = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = downloadUrl
    a.download = fileName || '文档.docx'
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(downloadUrl)
    document.body.removeChild(a)
  } catch (error) {
    console.error('下载日报文档失败:', error)
    throw error
  }
}

/**
 * 手动触发一次 Coze 文档同步。
 * @returns {Promise<string>} 同步结果描述
 */
export async function triggerDocumentSync() {
  try {
    const response = await request({
      url: '/document/sync',
      method: 'post',
      data: {},
      headers: {
        ...jsonHeaders,
        'Admin-Token': window.localStorage.token
      }
    })
    return response && response.data ? response.data : (response && response.msg) || '同步完成'
  } catch (error) {
    console.error('手动同步文档失败:', error)
    throw error
  }
}