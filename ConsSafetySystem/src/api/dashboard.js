import request from '@/utils/request'

/**
 * 获取首页看板聚合数据
 * 后端 `POST /dashboard/getDashboardData` 一次接口返回首页各模块数据：
 *   - buildAssetCount：危大工程设备统计（type_name/total_equipment/online_equipment/alert_count）
 *   - alertList / alertListPage：最近报警记录
 *   - alertTypeAndCount：报警类型聚合统计（content/numb）
 *   - spraySystemList：最近喷淋记录
 *   - realTimeList：环境实时数据（各监测点最新一条）
 */
export function getDashboardData() {
  return request({
    url: '/dashboard/getDashboardData',
    method: 'post'
  })
}