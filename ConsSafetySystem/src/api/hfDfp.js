import request from '@/utils/request'

// 后端统一返回 Result{ code, data, msg }，业务数据在 .data 中

/**
 * 查询高支模列表
 * POST /hfAndDfp/hfList
 * 返回 EquipmentAssets[]：assetId、assetName、assetCode、typeId、planeCoordinatePc 等
 */
export function hfList() {
  return request({
    url: '/hfAndDfp/hfList',
    method: 'post',
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 查询深基坑列表
 * POST /hfAndDfp/dfpList
 * 返回 EquipmentAssets[]：assetId、assetName、assetCode、typeId 等
 */
export function dfpList() {
  return request({
    url: '/hfAndDfp/dfpList',
    method: 'post',
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 查询高支模监测数据
 * POST /hfAndDfp/monitor/{assetId}
 * 返回 Map<String, List<MonitorValue>>，键为监测点名称
 * MonitorValue：monitorPointId、monitorData、monitorTime、monitorPointName、unit、location
 */
export function hfMonitor(assetId) {
  return request({
    url: `/hfAndDfp/monitor/${assetId}`,
    method: 'post',
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 查询高支模报警数据
 * POST /hfAndDfp/alarm/{assetId}
 * 返回 HfDfpAlarmInfoVO：alarmTypeList[{count,alarmType}]、alarmCountList[{alarmCount,alarmDate}]、warnCountList[]
 */
export function hfAlarm(assetId) {
  return request({
    url: `/hfAndDfp/alarm/${assetId}`,
    method: 'post',
    headers: { 'Content-Type': 'application/json' }
  })
}