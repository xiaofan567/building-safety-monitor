import request from '@/utils/request'

/**
 * 查询所有环境监测点
 * POST /environmentMonitorPoint/list
 * 返回 Result<List<EnvironmentMonitorPoint>>，实际数据在 res.data
 */
export function selectEnvMntData() {
  return request({
    url: '/environmentMonitorPoint/list',
    method: 'post',
    data: {},
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 更新环境监测点（仅阈值/单位/设备编码可编辑）
 * POST /environmentMonitorPoint/update
 */
export function updateEnvMntPoint(payload) {
  return request({
    url: '/environmentMonitorPoint/update',
    method: 'post',
    data: payload,
    headers: { 'Content-Type': 'application/json' }
  })
}
