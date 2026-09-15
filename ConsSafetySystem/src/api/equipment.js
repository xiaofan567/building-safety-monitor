import request from '@/utils/request'

// 设备类型ID常量（与后端一致）
export const EQUIPMENT_TYPE = {
  ALL: 0,        // 总览（聚合）
  TOWER_CRANE: 145,     // 塔吊
  ELEVATOR: 115,        // 升降机
  DEEP_FOUNDATION: 148, // 深基坑
  HIGH_FORMWORK: 147    // 高支模
}

/**
 * 机械设备总览 - 根据设备类型ID查询
 * POST /buildView/overview?id={typeId}
 * 返回 EquipmentOverviewVO：
 *   equipmentAssetsCount / equipmentAssetsOnlineCount / equipmentAssetsList
 *   alarmCount / warnCount（各含 compare_day_count / yesterday_count / growth_rate）
 * @param {number} typeId 设备类型ID（塔吊145 升降机115 高支模147 深基坑148）
 */
export function selectEquipDataByType(typeId) {
  return request({
    url: '/buildView/overview',
    method: 'post',
    params: { id: typeId },
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 机械设备总览 - 全部设备（聚合）
 * 后端无单独"全部"接口，并发请求 4 类设备后合并统计与列表。
 * 注意：本项目后端统一返回 Result 包装，接口原样返回，需通过 .data 取业务数据。
 */
export async function selectAllEquipData() {
  const ids = [
    EQUIPMENT_TYPE.TOWER_CRANE,
    EQUIPMENT_TYPE.ELEVATOR,
    EQUIPMENT_TYPE.DEEP_FOUNDATION,
    EQUIPMENT_TYPE.HIGH_FORMWORK
  ]

  const emptyVo = () => ({
    equipmentAssetsCount: 0,
    equipmentAssetsOnlineCount: 0,
    equipmentAssetsList: [],
    alarmCount: { compare_day_count: 0, yesterday_count: 0, growth_rate: 0 },
    warnCount: { compare_day_count: 0, yesterday_count: 0, growth_rate: 0 }
  })

  let responses = []
  try {
    responses = await Promise.all(ids.map((id) => selectEquipDataByType(id)))
  } catch (error) {
    console.error('总览数据加载失败：', error)
    responses = [emptyVo(), emptyVo(), emptyVo(), emptyVo()]
  }

  // 解包 Result.data 得到业务 VO
  const vos = responses.map((r) => r && r.data ? r.data : emptyVo())

  return {
    equipmentAssetsCount: vos.reduce((sum, r) => sum + (r.equipmentAssetsCount || 0), 0),
    equipmentAssetsOnlineCount: vos.reduce((sum, r) => sum + (r.equipmentAssetsOnlineCount || 0), 0),
    equipmentAssetsList: vos.flatMap((r) => r.equipmentAssetsList || []),
    alarmCount: mergeTrend(vos, 'alarmCount'),
    warnCount: mergeTrend(vos, 'warnCount')
  }
}

/**
 * 设备上下线记录 - 分页查询
 * POST /equipmentOffline/selectPage
 * 入参：{ page, limit, equipmentId? }
 * 返回 Result<BPage<EquipmentOffline>>：
 *   records: [{ id, equipmentId, status(1上线/2离线), monitorTime, equipmentName, assetCode, typeName }]
 *   total / pages
 * @param {{page:number, limit:number, equipmentId?:number}} data
 */
export function selectOnOffLineData(data) {
  return request({
    url: '/equipmentOffline/selectPage',
    method: 'post',
    data,
    headers: { 'Content-Type': 'application/json' }
  })
}

/**
 * 合并多类设备的报警/预警趋势：数量求和，增长率按合并后总量重新计算。
 */
function mergeTrend(responses, key) {
  const yesterdayCount = responses.reduce((sum, r) => sum + (r[key]?.yesterday_count || 0), 0)
  const compareDayCount = responses.reduce((sum, r) => sum + (r[key]?.compare_day_count || 0), 0)
  const growthRate = (() => {
    if (compareDayCount === 0) return yesterdayCount > 0 ? 100 : 0
    return Math.round((yesterdayCount - compareDayCount) * 100.0 / compareDayCount)
  })()
  return { yesterday_count: yesterdayCount, compare_day_count: compareDayCount, growth_rate: growthRate }
}