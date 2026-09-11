import request from '@/utils/request'

/**
 * 监测点分页列表
 * POST /equipmentMonitorPoint/list   body: { current, size, monitorType, pointName }
 * 返回 BPage：data.records 为列表、data.total 为总数
 */
export function listMonitorPoints(params = {}) {
  return request({
    url: '/equipmentMonitorPoint/list',
    method: 'post',
    data: {
      current: params.current ?? 1,
      size: params.size ?? 10,
      monitorType: params.monitorType ?? undefined,
      pointName: params.pointName ?? undefined
    },
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 设备资产下拉（用于选择所属设备）
 * POST /equipmentAssets/list   返回 data.records
 */
export function listEquipmentAssetsForSelect() {
  return request({
    url: '/equipmentAssets/list',
    method: 'post',
    data: { current: 1, size: 9999 },
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 新增或编辑监测点（后端按 id 是否存在区分）
 * POST /equipmentMonitorPoint/saveOrUpdate
 */
export function saveOrUpdateMonitorPoint(data) {
  return request({
    url: '/equipmentMonitorPoint/saveOrUpdate',
    method: 'post',
    data,
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 删除监测点
 * POST /equipmentMonitorPoint/delete   body: { id }
 */
export function deleteMonitorPoint(id) {
  return request({
    url: '/equipmentMonitorPoint/delete',
    method: 'post',
    data: { id },
    headers: { 'Content-Type': 'application/json' }
  })
}
