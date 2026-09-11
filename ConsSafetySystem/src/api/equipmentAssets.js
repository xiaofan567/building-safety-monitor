import request from '@/utils/request'

/**
 * 设备分页列表
 * POST /equipmentAssets/list   body: { current, size }
 * 返回 BPage：data.records 为列表、data.total 为总数
 */
export function getEquipmentAssetsList(params = {}) {
  return request({
    url: '/equipmentAssets/list',
    method: 'post',
    data: {
      current: params.current ?? 1,
      size: params.size ?? 10
    },
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 新增或编辑设备（后端同一接口，按 assetId 是否存在区分）
 * POST /equipmentAssets/save
 */
export function saveEquipmentAssets(data) {
  return request({
    url: '/equipmentAssets/save',
    method: 'post',
    data,
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 删除设备
 * POST /equipmentAssets/delete   body: { assetId }
 */
export function deleteEquipmentAssets(assetId) {
  return request({
    url: '/equipmentAssets/delete',
    method: 'post',
    data: { assetId },
    headers: { 'Content-Type': 'application/json' }
  })
}