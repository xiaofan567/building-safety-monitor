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