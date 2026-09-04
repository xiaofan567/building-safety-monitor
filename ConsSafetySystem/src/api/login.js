import request from '@/utils/request'

/**
 * 登录
 * POST /login
 * 请求体：{ username, password }
 * 响应：{ code:0, msg:'success', data:{ adminToken:'xxx' } }
 */
export function login(data) {
  return request({
    url: '/login',
    method: 'post',
    data,
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 获取当前登录用户信息
 * POST /queryLoginUser （需携带 Admin-Token 请求头）
 */
export function getUserInfo() {
  return request({
    url: '/queryLoginUser',
    method: 'post',
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 退出登录
 * POST /logout
 */
export function logout() {
  return request({
    url: '/logout',
    method: 'post',
    headers: { 'Content-Type': 'application/json' }
  })
}