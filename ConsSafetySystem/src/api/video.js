import request from '@/utils/request'

/**
 * 查询摄像头列表（POST /camera/list）
 * 返回 Camera 分页记录：
 *   id / assetId / assetName / assetCode / status(1在线 0离线)
 *   isEnable / wvpOpenUrl(完整HLS拉流地址) / aiMonitorType / locationName
 */
export function selectVideoData(data) {
  return request({
    url: '/camera/list',
    method: 'post',
    data,
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 修改摄像头信息（POST /camera/update）
 * 必须携带 id，可更新 status / isEnable / aiMonitorType / sort / wvpOpenUrl
 */
export function updateVideoData(data) {
  return request({
    url: '/camera/update',
    method: 'post',
    data,
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 查询AI摄像头报警信息列表（POST /aiAlert/selectPage）
 * 参数：assetCode / alertType / alertStatus / startTime / endTime + page/limit
 */
export function selectAiAlertData(data) {
  return request({
    url: '/aiAlert/selectPage',
    method: 'post',
    data,
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 查询AI报警详情（POST /aiAlert/get/{id}，无请求体）
 */
export function getAiAlertDetail(id) {
  return request({
    url: `/aiAlert/get/${id}`,
    method: 'post'
  })
}

/**
 * 处理AI报警（POST /aiAlert/update，@RequestBody）
 * 必须携带 id + handleBy + handleContent，后端自动置处理时间/处理人ID
 */
export function updateAiAlert(data) {
  return request({
    url: '/aiAlert/update',
    method: 'post',
    data,
    headers: { 'Content-Type': 'application/json' }
  })
}