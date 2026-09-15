import request from '@/utils/request'

// 后端统一返回 Result{ code, data, msg }，业务数据在 .data 中

/**
 * 查询升降机列表
 * POST /elevator/selectAllElevator
 * 返回 ElevatorInfo[]：assetId、ratedWeight、basicHeight、ascendingSpeed、ratedLoad、cabinSize、maxAscendingHeight、assetName、assetCode
 */
export function selectAllElevator() {
  return request({
    url: '/elevator/selectAllElevator',
    method: 'post',
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 查询升降机实时数据
 * POST /elevator/getData/{assetId}
 * 返回 ElevatorData[]：monitorTime、speed、weight、weightPercent、height、xAngle、yAngle、numberOfPeople、liftingDirection、warningState、beforeLock、afterLock、wind
 */
export function getElevatorData(assetId) {
  return request({
    url: `/elevator/getData/${assetId}`,
    method: 'post',
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 查询升降机报警信息
 * POST /elevator/getElevatorAlarmInfo/{assetId}
 * 返回 ElevatorAlarmInfoVO：totalWarning、totalAlarm、todayWarning、todayAlarm、typeList[]、days30Warning[]、days30Alarm[]
 */
export function getElevatorAlarmInfo(assetId) {
  return request({
    url: `/elevator/getElevatorAlarmInfo/${assetId}`,
    method: 'post',
    headers: { 'Content-Type': 'application/json' }
  })
}