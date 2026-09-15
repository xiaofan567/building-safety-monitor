import request from '@/utils/request'

// 后端统一返回 Result{ code, data, msg }，业务数据在 .data 中

/**
 * 查询塔吊列表
 * POST /tower/selectAllTower
 * 返回 TowerInfo[]：id、assetId、forwardArmLength、backwardArmLength、maxHeight、ratedLoad、maxLoad、ratedMoment、assetName
 */
export function selectAllTower() {
  return request({
    url: '/tower/selectAllTower',
    method: 'post',
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 查询塔吊实时数据（最近十条）
 * POST /tower/getData/{assetId}
 * 返回 TowerData[]：monitorTime、weight、amplitude、obliguity、windSpeed、moment、height、rotation
 */
export function getTowerData(assetId) {
  return request({
    url: `/tower/getData/${assetId}`,
    method: 'post',
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 查询塔吊今日工作状态
 * POST /tower/getTowerWorkInfo/{assetId}
 * 返回 TowerWorkInfoVO：totalWorkTime、totalTimes、totalAlarm、totalLoad、totalEffect
 */
export function getTowerWorkInfo(assetId) {
  return request({
    url: `/tower/getTowerWorkInfo/${assetId}`,
    method: 'post',
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 查询塔吊报警信息
 * POST /tower/getTowerAlarmInfo/{assetId}
 * 返回 TowerAlarmInfoVO：totalWarning、totalAlarm、todayWarning、todayAlarm、typeList[]、days30Warning[]、days30Alarm[]
 */
export function getTowerAlarmInfo(assetId) {
  return request({
    url: `/tower/getTowerAlarmInfo/${assetId}`,
    method: 'post',
    headers: { 'Content-Type': 'application/json' }
  })
}