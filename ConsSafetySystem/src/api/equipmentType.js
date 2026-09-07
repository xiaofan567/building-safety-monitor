import request from '@/utils/request'

/**
 * 获取设备类型树形结构
 * GET /equipmentType/tree
 */
export function getEquipmentTypeTree() {
  return request({
    url: '/equipmentType/tree',
    method: 'get'
  })
}

/**
 * 获取设备类型列表
 * GET /equipmentType/list
 */
export function getEquipmentTypeList() {
  return request({
    url: '/equipmentType/list',
    method: 'get'
  })
}

/**
 * 新增设备类型
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
 * 编辑设备类型
 * POST /equipmentType/update
 */
export function updateEquipmentType(data) {
  return request({
    url: '/equipmentType/update',
    method: 'post',
    data,
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 删除设备类型
 * POST /equipmentType/delete
 */
export function deleteEquipmentType(typeId) {
  return request({
    url: '/equipmentType/delete',
    method: 'post',
    data: { typeId },
    headers: { 'Content-Type': 'application/json' }
  })
}