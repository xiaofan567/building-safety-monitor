import request from '@/utils/request'

/**
 * 获取设备位置树形结构
 * 后端 `POST /equipmentLocation/queryTreeList` 直接返回嵌套好的树（含 children）
 */
export function getEquipmentLocationTree() {
  return request({
    url: '/equipmentLocation/queryTreeList',
    method: 'post'
  })
}

/**
 * 新增或编辑设备位置（后端同一接口，按 locationId 是否存在区分）
 * POST /equipmentLocation/save
 */
export function saveEquipmentLocation(data) {
  return request({
    url: '/equipmentLocation/save',
    method: 'post',
    data,
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 删除设备位置
 * POST /equipmentLocation/delete
 * @param {number} locationId 位置ID
 */
export function deleteEquipmentLocation(locationId) {
  return request({
    url: '/equipmentLocation/delete',
    method: 'post',
    data: { locationId },
    headers: { 'Content-Type': 'application/json' }
  })
}