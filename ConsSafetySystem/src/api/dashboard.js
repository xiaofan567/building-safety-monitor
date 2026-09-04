import request from '@/utils/request'

/**
 * 获取统计数据卡片
 * GET /dashboard/stats
 */
export function getDashboardStats() {
  return request({
    url: '/dashboard/stats',
    method: 'get'
  })
}

/**
 * 获取环境实时数据
 * GET /dashboard/environment/realtime
 */
export function getEnvironmentRealtime() {
  return request({
    url: '/dashboard/environment/realtime',
    method: 'get'
  })
}

/**
 * 获取安全隐患数据（用于图表）
 * GET /dashboard/safety/hazards
 */
export function getSafetyHazards() {
  return request({
    url: '/dashboard/safety/hazards',
    method: 'get'
  })
}

/**
 * 获取喷淋记录
 * GET /dashboard/spray/records
 */
export function getSprayRecords() {
  return request({
    url: '/dashboard/spray/records',
    method: 'get'
  })
}

/**
 * 获取报警列表
 * GET /dashboard/alarms
 */
export function getAlarmList() {
  return request({
    url: '/dashboard/alarms',
    method: 'get'
  })
}

/**
 * 获取设备状态总览
 * GET /dashboard/device/overview
 */
export function getDeviceOverview() {
  return request({
    url: '/dashboard/device/overview',
    method: 'get'
  })
}

/**
 * 获取设备运行统计
 * GET /dashboard/device/statistics
 */
export function getDeviceStatistics() {
  return request({
    url: '/dashboard/device/statistics',
    method: 'get'
  })
}