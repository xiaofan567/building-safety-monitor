import request from '@/utils/request'

/**
 * 获取设备类型树形结构
 * 后端 `POST /equipmentType/queryTreeList` 直接返回嵌套好的树（含 children）
 */
export function getEquipmentTypeTree() {
  return request({
    url: '/equipmentType/queryTreeList',
    method: 'post'
  })
}

/**
 * 新增或编辑设备类型（后端同一接口，按 typeId 是否存在区分）
 * POST /equipmentType/save
 */
export function saveEquipmentType(data) {
  return request({
    url: '/equipmentType/save',
    method: 'post',
    data,
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 删除设备类型
 * POST /equipmentType/delete
 * @param {number} typeId 类型ID
 */
export function deleteEquipmentType(typeId) {
  return request({
    url: '/equipmentType/delete',
    method: 'post',
    data: { typeId },
    headers: { 'Content-Type': 'application/json' }
  })
}