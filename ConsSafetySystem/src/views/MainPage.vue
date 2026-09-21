<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getDashboardData } from '@/api/dashboard'

const stats = ref({})
const envData = ref([])
const hazardChartData = ref([])
const sprinklerRecords = ref([])
const alarmList = ref([])
const deviceOverview = ref({})
const deviceStatistics = ref([])

const envChartRef = ref(null)
const hazardChartRef = ref(null)
const sprinklerChartRef = ref(null)
const deviceOverviewChartRef = ref(null)
const deviceStatsChartRef = ref(null)

let envChartInstance = null
let hazardChartInstance = null
let sprinklerChartInstance = null
let deviceOverviewChartInstance = null
let deviceStatsChartInstance = null
let resizeHandler = null

const chartColors = {
  blue: '#1a73e8',
  green: '#0d9e6c',
  orange: '#f5a623',
  red: '#e54545',
  cyan: '#0ea5e9',
  purple: '#8b5cf6',
  blueLight: '#5a8fd4',
  greenLight: '#2dd4a0',
  gridLine: 'rgba(122,106,85,0.18)',
  textDim: '#9a8f7f',
  textNormal: '#6b5f4f'
}

// 环境实时数据卡片配置：realTimeList 中按 monitorPointCode 取最新值
const envMeta = [
  { code: 'temp', label: '温度', unit: '°C', color: '#f5a623' },
  { code: 'humi', label: '湿度', unit: '%', color: '#1a73e8' },
  { code: 'windSpeed', label: '风速', unit: 'm/s', color: '#0ea5e9' },
  { code: 'noise', label: '噪音', unit: 'dB', color: '#e54545' },
  { code: 'pm25', label: 'PM2.5', unit: 'μg/m³', color: '#8b5cf6' },
  { code: 'tsp', label: '粉尘', unit: 'mg/m³', color: '#0d9e6c' }
]
// 把 realTimeList 转成 code -> value 的映射
function realTimeMap() {
  const map = {}
  ;(envData.value || []).forEach(it => {
    if (it.monitorPointCode) map[it.monitorPointCode] = it.value
  })
  return map
}
const envItems = computed(() => {
  const map = realTimeMap()
  return envMeta.map(m => ({
    code: m.code,
    label: m.label,
    unit: m.unit,
    color: m.color,
    display: map[m.code] !== undefined && map[m.code] !== null ? Number(map[m.code]) : null
  }))
})

// 报警类型展示（监测点/塔吊/升降机）
const alertTypeLabel = (t) =>
  ({ '1': '监测点', '2': '塔吊', '3': '升降机' }[String(t)] || '--')

// 喷淋运行状态（spraySystemList.operationType：0 关闭 1 开启 2 操作异常）
const sprayStatus = (op) =>
  ({ '0': '已关闭', '1': '开启', '2': '操作异常' }[String(op)] || '--')
const sprayTagClass = (op) => {
  const o = String(op)
  if (o === '2') return 'tag-orange'
  if (o === '0') return 'tag-red'
  return 'tag-green'
}

function initEnvChart() {
  if (!envChartRef.value) return
  if (envChartInstance) envChartInstance.dispose()
  envChartInstance = echarts.init(envChartRef.value)

  const map = realTimeMap()
  const tempVal = map.temp !== undefined ? Number(map.temp) : null
  const humiVal = map.humi !== undefined ? Number(map.humi) : null
  const hours = ['00:00','02:00','04:00','06:00','08:00','10:00','12:00','14:00','16:00','18:00','20:00','22:00']
  const tempData = hours.map((h, i) => (i === 11 ? tempVal : null))
  const humData = hours.map((h, i) => (i === 11 ? humiVal : null))

  envChartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: 'rgba(176,123,62,0.35)',
      textStyle: { color: '#332e26', fontSize: 12 }
    },
    legend: {
      bottom: 0,
      textStyle: { color: '#8a7f6f', fontSize: 11 },
      itemWidth: 10,
      itemHeight: 6,
      itemGap: 20
    },
    grid: { top: 10, right: 16, bottom: 30, left: 40 },
    xAxis: {
      type: 'category',
      data: hours,
      axisLine: { lineStyle: { color: chartColors.gridLine } },
      axisTick: { show: false },
      axisLabel: { color: chartColors.textDim, fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: chartColors.gridLine, type: 'dashed' } },
      axisLabel: { color: chartColors.textDim, fontSize: 10 }
    },
    series: [
      {
        name: '温度(°C)',
        type: 'line',
        data: tempData,
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        lineStyle: { color: chartColors.orange, width: 2 },
        itemStyle: { color: chartColors.orange },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(245,166,35,0.15)' },
            { offset: 1, color: 'rgba(245,166,35,0)' }
          ])
        }
      },
      {
        name: '湿度(%)',
        type: 'line',
        data: humData,
        smooth: true,
        symbol: 'circle',
        symbolSize: 4,
        lineStyle: { color: chartColors.blue, width: 2 },
        itemStyle: { color: chartColors.blue },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(26,115,232,0.15)' },
            { offset: 1, color: 'rgba(26,115,232,0)' }
          ])
        }
      }
    ]
  })
}

function initHazardChart() {
  if (!hazardChartRef.value) return
  if (hazardChartInstance) hazardChartInstance.dispose()
  hazardChartInstance = echarts.init(hazardChartRef.value)

  // 安全隐患分布数据源：后端报警类型聚合（content -> 数量）
  const hazardList = Array.isArray(hazardChartData.value) ? hazardChartData.value : []
  let categories = hazardList.map(i => i.content)
  let values = hazardList.map(i => Number(i.numb) || 0)
  if (!categories.length) {
    categories = ['高处坠落','物体打击','机械伤害','坍塌','触电','火灾']
    values = [23, 18, 15, 10, 8, 6]
  }

  hazardChartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: 'rgba(176,123,62,0.35)',
      textStyle: { color: '#332e26', fontSize: 12 }
    },
    grid: { top: 10, right: 20, bottom: 24, left: 36 },
    xAxis: {
      type: 'category',
      data: categories,
      axisLine: { lineStyle: { color: chartColors.gridLine } },
      axisTick: { show: false },
      axisLabel: { color: chartColors.textDim, fontSize: 10, rotate: 20 }
    },
    yAxis: {
      type: 'value',
      name: '隐患数',
      nameTextStyle: { color: chartColors.textDim, fontSize: 10 },
      splitLine: { lineStyle: { color: chartColors.gridLine, type: 'dashed' } },
      axisLabel: { color: chartColors.textDim, fontSize: 10 }
    },
    series: [{
      type: 'bar',
      data: values,
      barWidth: 22,
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#1a73e8' },
          { offset: 1, color: '#0d5aaa' }
        ])
      },
      emphasis: {
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#3b8af0' },
            { offset: 1, color: '#1a73e8' }
          ])
        }
      }
    }]
  })
}

function initSprinklerChart() {
  if (!sprinklerChartRef.value) return
  if (sprinklerChartInstance) sprinklerChartInstance.dispose()
  sprinklerChartInstance = echarts.init(sprinklerChartRef.value)

  // 喷淋操作统计：按日期聚合最近喷淋记录（spraySystemList）的操作次数
  const list = Array.isArray(sprinklerRecords.value) ? sprinklerRecords.value : []
  const countByDate = {}
  list.forEach(it => {
    const dt = (it.operationTime || '').replace('T', ' ').slice(0, 10)
    if (dt) countByDate[dt] = (countByDate[dt] || 0) + 1
  })
  const dates = Object.keys(countByDate).sort()
  const days = dates.length ? dates : ['近期']
  const sprayData = dates.length ? dates.map(d => countByDate[d]) : [0]

  sprinklerChartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: 'rgba(176,123,62,0.35)',
      textStyle: { color: '#332e26', fontSize: 12 }
    },
    grid: { top: 10, right: 16, bottom: 24, left: 42 },
    xAxis: {
      type: 'category',
      data: days,
      axisLine: { lineStyle: { color: chartColors.gridLine } },
      axisTick: { show: false },
      axisLabel: { color: chartColors.textDim, fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      name: '操作次数',
      nameTextStyle: { color: chartColors.textDim, fontSize: 10 },
      splitLine: { lineStyle: { color: chartColors.gridLine, type: 'dashed' } },
      axisLabel: { color: chartColors.textDim, fontSize: 10 }
    },
    series: [{
      type: 'bar',
      data: sprayData,
      barWidth: 16,
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#0d9e6c' },
          { offset: 1, color: '#0a6e4a' }
        ])
      },
      emphasis: {
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#2dd4a0' },
            { offset: 1, color: '#0d9e6c' }
          ])
        }
      }
    }]
  })
}

function initDeviceOverviewChart() {
  if (!deviceOverviewChartRef.value) return
  if (deviceOverviewChartInstance) deviceOverviewChartInstance.dispose()
  deviceOverviewChartInstance = echarts.init(deviceOverviewChartRef.value)

  // 设备状态总览：后端危大工程设备统计（type_name -> 设备总数）
  const countList = Array.isArray(deviceOverview.value) ? deviceOverview.value : []
  const piColors = ['#1a73e8', '#0d9e6c', '#f5a623', '#0ea5e9', '#8b5cf6']
  let data = countList.map((x, idx) => ({
    name: x.type_name || x.typeName || '未分类',
    value: Number(x.total_equipment) || 0,
    itemStyle: { color: piColors[idx % piColors.length] }
  }))
  if (!data.length) {
    data = [
      { value: 20, name: '塔吊', itemStyle: { color: '#1a73e8' } },
      { value: 12, name: '升降机', itemStyle: { color: '#0d9e6c' } },
      { value: 18, name: '传感器', itemStyle: { color: '#f5a623' } },
      { value: 8, name: '喷淋设备', itemStyle: { color: '#0ea5e9' } },
      { value: 6, name: '摄像头', itemStyle: { color: '#8b5cf6' } }
    ]
  }

  deviceOverviewChartInstance.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: 'rgba(176,123,62,0.35)',
      textStyle: { color: '#332e26', fontSize: 12 },
      formatter: '{b}: {c} 台 ({d}%)'
    },
    legend: {
      bottom: 0,
      textStyle: { color: '#8a7f6f', fontSize: 10 },
      itemWidth: 8,
      itemHeight: 8,
      itemGap: 12
    },
    series: [{
      type: 'pie',
      radius: ['50%', '72%'],
      center: ['50%', '48%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 4,
        borderColor: '#ffffff',
        borderWidth: 3
      },
      label: {
        show: false
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 14,
          fontWeight: 'bold',
          color: '#332e26'
        },
        scaleSize: 8
      },
      data: data
    }]
  })
}

function initDeviceStatsChart() {
  if (!deviceStatsChartRef.value) return
  if (deviceStatsChartInstance) deviceStatsChartInstance.dispose()
  deviceStatsChartInstance = echarts.init(deviceStatsChartRef.value)

  // 设备运行统计：后端危大工程设备统计（在线 / 总-在线）
  const statsList = Array.isArray(deviceStatistics.value) ? deviceStatistics.value : []
  let types = statsList.map(x => x.type_name || x.typeName || '未分类')
  let onlineData = statsList.map(x => Number(x.online_equipment) || 0)
  let offlineData = statsList.map(x => (Number(x.total_equipment) || 0) - (Number(x.online_equipment) || 0))
  if (!types.length) {
    types = ['塔吊', '升降机', '传感器', '喷淋', '摄像头']
    onlineData = [18, 10, 16, 7, 5]
    offlineData = [2, 2, 2, 1, 1]
  }

  deviceStatsChartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.98)',
      borderColor: 'rgba(176,123,62,0.35)',
      textStyle: { color: '#332e26', fontSize: 12 }
    },
    legend: {
      bottom: 0,
      textStyle: { color: '#8a7f6f', fontSize: 11 },
      itemWidth: 10,
      itemHeight: 6,
      itemGap: 20
    },
    grid: { top: 10, right: 16, bottom: 30, left: 36 },
    xAxis: {
      type: 'category',
      data: types,
      axisLine: { lineStyle: { color: chartColors.gridLine } },
      axisTick: { show: false },
      axisLabel: { color: chartColors.textDim, fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      name: '台',
      nameTextStyle: { color: chartColors.textDim, fontSize: 10 },
      splitLine: { lineStyle: { color: chartColors.gridLine, type: 'dashed' } },
      axisLabel: { color: chartColors.textDim, fontSize: 10 }
    },
    series: [
      {
        name: '在线',
        type: 'bar',
        data: onlineData,
        barWidth: 14,
        barGap: '30%',
        itemStyle: {
          borderRadius: [3, 3, 0, 0],
          color: '#0d9e6c'
        }
      },
      {
        name: '离线',
        type: 'bar',
        data: offlineData,
        barWidth: 14,
        itemStyle: {
          borderRadius: [3, 3, 0, 0],
          color: '#e54545'
        }
      }
    ]
  })
}

function handleResize() {
  envChartInstance?.resize()
  hazardChartInstance?.resize()
  sprinklerChartInstance?.resize()
  deviceOverviewChartInstance?.resize()
  deviceStatsChartInstance?.resize()
}

function renderCharts() {
  initEnvChart()
  initHazardChart()
  initSprinklerChart()
  initDeviceOverviewChart()
  initDeviceStatsChart()
}

async function fetchData() {
  try {
    const res = await getDashboardData()
    if (!res || res.code !== 0) return
    const d = res.data || {}

    // 顶部统计卡片：从危大工程设备统计聚合
    const buildList = Array.isArray(d.buildAssetCount) ? d.buildAssetCount : []
    const deviceTotal = buildList.reduce((s, x) => s + (Number(x.total_equipment) || 0), 0)
    const onlineCount = buildList.reduce((s, x) => s + (Number(x.online_equipment) || 0), 0)
    const alarmCount = buildList.reduce((s, x) => s + (Number(x.alert_count) || 0), 0)
    stats.value = {
      deviceTotal,
      onlineCount,
      alarmCount,
      // 后端接口暂无喷淋运行时长字段，保留占位默认值
      sprinklerHours: 38
    }

    // 预警事件列表
    alarmList.value = ((d.alertList || d.alertListPage) || []).slice(0, 5)
    // 喷淋记录
    sprinklerRecords.value = d.spraySystemList || []
    // 环境实时数据（各监测点最新一条）
    envData.value = d.realTimeList || []

    // 三个图表的数据源
    deviceOverview.value = buildList
    deviceStatistics.value = buildList
    hazardChartData.value = d.alertTypeAndCount || []

    // 数据就绪后刷新图表
    renderCharts()
  } catch (e) {
    // 接口异常时保留默认占位展示，不做额外提示
  }
}

onMounted(() => {
  fetchData()
  setTimeout(() => {
    renderCharts()
  }, 200)
  resizeHandler = handleResize
  window.addEventListener('resize', resizeHandler)
})

onUnmounted(() => {
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  envChartInstance?.dispose()
  hazardChartInstance?.dispose()
  sprinklerChartInstance?.dispose()
  deviceOverviewChartInstance?.dispose()
  deviceStatsChartInstance?.dispose()
})

const goToDailyReport = () => {
  const base = `${window.location.origin}${import.meta.env.BASE_URL || '/'}`
  const url = `${base.replace(/\/$/, '')}/daily-report`
  window.open(url, '_blank', 'noopener,noreferrer')
}
</script>

<template>
  <div class="dashboard">
    <div class="page-header">
      <h2>监控总览</h2>
      <div class="header-meta">
        <span class="meta-dot"></span>
        <span>实时数据更新中</span>
        <span class="meta-time">{{ new Date().toLocaleString('zh-CN') }}</span>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background: rgba(26,115,232,.12);">
          <svg viewBox="0 0 24 24" fill="none" width="22" height="22"><rect x="3" y="3" width="18" height="18" rx="2" stroke="#1a73e8" stroke-width="1.5"/><path d="M7 9h4M7 13h2M7 17h3" stroke="#1a73e8" stroke-width="1.4" stroke-linecap="round"/></svg>
        </div>
        <div class="stat-info">
          <div class="stat-val">{{ stats.deviceTotal || 56 }}</div>
          <div class="stat-lbl">设备总数</div>
        </div>
        <div class="stat-trend up">+3</div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: rgba(13,158,108,.12);">
          <svg viewBox="0 0 24 24" fill="none" width="22" height="22"><path d="M9 11.5L11.2 13.7L15.5 9" stroke="#0d9e6c" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/><circle cx="12" cy="12" r="9" stroke="#0d9e6c" stroke-width="1.5"/></svg>
        </div>
        <div class="stat-info">
          <div class="stat-val">{{ stats.onlineCount || 48 }}</div>
          <div class="stat-lbl">在线设备</div>
        </div>
        <div class="stat-trend up">85.7%</div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: rgba(245,166,35,.12);">
          <svg viewBox="0 0 24 24" fill="none" width="22" height="22"><path d="M12 2L2 7l10 5 10-5L12 2Z" stroke="#f5a623" stroke-width="1.5" stroke-linejoin="round"/><path d="M2 17l10 5 10-5" stroke="#f5a623" stroke-width="1.5" stroke-linejoin="round"/><path d="M2 12l10 5 10-5" stroke="#f5a623" stroke-width="1.5" stroke-linejoin="round"/></svg>
        </div>
        <div class="stat-info">
          <div class="stat-val">{{ stats.alarmCount || 12 }}</div>
          <div class="stat-lbl">今日预警</div>
        </div>
        <div class="stat-trend down">-5</div>
      </div>

      <div class="stat-card">
        <div class="stat-icon" style="background: rgba(14,165,233,.12);">
          <svg viewBox="0 0 24 24" fill="none" width="22" height="22"><circle cx="12" cy="12" r="9" stroke="#0ea5e9" stroke-width="1.5"/><path d="M12 7v5l3 3" stroke="#0ea5e9" stroke-width="1.5" stroke-linecap="round"/></svg>
        </div>
        <div class="stat-info">
          <div class="stat-val">{{ stats.sprinklerHours || 38 }}<span class="stat-unit">h</span></div>
          <div class="stat-lbl">喷淋运行时长</div>
        </div>
        <div class="stat-trend up">+12%</div>
      </div>
    </div>

    <div class="dashboard-grid">
      <div class="grid-col-left">
        <div class="panel">
          <div class="panel-header">
            <div class="panel-title">环境实时数据</div>
            <span class="panel-link">查看详情 →</span>
          </div>
          <div class="env-grid">
            <div class="env-item" v-for="item in envItems" :key="item.code">
              <div class="env-val" :style="{ color: item.color }">
                {{ item.display !== null ? item.display : '--' }}
                <span class="env-unit" v-if="item.display !== null">{{ item.unit }}</span>
              </div>
              <div class="env-label">{{ item.label }}</div>
              <div class="env-status normal">正常</div>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-header">
            <div class="panel-title">安全隐患分布</div>
            <span class="panel-link">查看详情 →</span>
          </div>
          <div ref="hazardChartRef" class="chart-container"></div>
        </div>

        <div class="panel">
          <div class="panel-header">
            <div class="panel-title">预警事件列表</div>
            <span class="panel-link">查看全部 →</span>
          </div>
          <div class="simple-table-wrap">
            <table class="simple-table">
              <thead>
                <tr>
                  <th>时间</th>
                  <th>类型</th>
                  <th>内容</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in alarmList.slice(0, 5)" :key="item.id">
                  <td>{{ (item.createTime || '').replace('T', ' ').slice(0, 19) || '--' }}</td>
                  <td>{{ alertTypeLabel(item.monitorAlertType) }}</td>
                  <td>{{ item.content || '--' }}</td>
                  <td>
                    <span class="tag" :class="item.handleState === '0' ? 'tag-red' : 'tag-green'">
                      {{ item.handleState === '0' ? '未处理' : '已处理' }}
                    </span>
                  </td>
                </tr>
                <tr v-if="alarmList.length === 0">
                  <td colspan="4" style="text-align:center;color:#5a6a7a;">暂无预警事件</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="grid-col-right">
        <div class="panel">
          <div class="panel-header">
            <div class="panel-title">环境趋势</div>
            <div class="panel-legend">
              <span class="legend-dot" style="background:#f5a623"></span>温度
              <span class="legend-dot" style="background:#1a73e8"></span>湿度
            </div>
          </div>
          <div ref="envChartRef" class="chart-container"></div>
        </div>

        <div class="panel">
          <div class="panel-header">
            <div class="panel-title">设备状态总览</div>
            <span class="panel-link">查看详情 →</span>
          </div>
          <div ref="deviceOverviewChartRef" class="chart-container chart-md"></div>
        </div>

        <div class="panel">
          <div class="panel-header">
            <div class="panel-title">喷淋记录</div>
            <span class="panel-link">查看详情 →</span>
          </div>
          <div class="simple-table-wrap">
            <table class="simple-table">
              <thead>
                <tr>
                  <th>区域</th>
                  <th>设备编号</th>
                  <th>运行状态</th>
                  <th>时间</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, idx) in sprinklerRecords" :key="item.id || idx">
                  <td>{{ item.assetLocation || item.pointName || '--' }}</td>
                  <td>{{ item.assetName || item.assetCode || '--' }}</td>
                  <td>{{ sprayStatus(item.operationType) }}</td>
                  <td>{{ (item.operationTime || '').replace('T', ' ').slice(0, 19) || '--' }}</td>
                  <td>
                    <span class="tag" :class="sprayTagClass(item.operationType)">
                      {{ sprayStatus(item.operationType) }}
                    </span>
                  </td>
                </tr>
                <tr v-if="!sprinklerRecords.length">
                  <td colspan="5" style="text-align:center;color:#5a6a7a;">暂无喷淋记录</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="panel">
          <div class="panel-header">
            <div class="panel-title">喷淋操作统计</div>
            <span class="panel-link">查看详情 →</span>
          </div>
          <div ref="sprinklerChartRef" class="chart-container chart-sm"></div>
        </div>

        <div class="panel">
          <div class="panel-header">
            <div class="panel-title">设备运行统计</div>
            <span class="panel-link">查看详情 →</span>
          </div>
          <div ref="deviceStatsChartRef" class="chart-container chart-sm"></div>
        </div>
      </div>
    </div>
    <div class="ai-assistant-entry">
      <button class="ai-assistant-fab" type="button" title="智能监测助手" @click="goToDailyReport">
        <span class="ai-fab-glow"></span>
        <span class="ai-fab-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" width="20" height="20">
            <rect x="4" y="6" width="16" height="12" rx="3" stroke="currentColor" stroke-width="1.7"/>
            <path d="M9 11h.01M12 11h.01M15 11h.01M9 15h6M12 3v3" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>
          </svg>
        </span>
        <span class="ai-fab-label">智能监测助手</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: var(--bg-page);
}

.dashboard::-webkit-scrollbar {
  width: 6px;
}

.dashboard::-webkit-scrollbar-thumb {
  background: rgba(176, 123, 62, 0.25);
  border-radius: 3px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  color: var(--text-strong);
  letter-spacing: 1px;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-muted);
}

.meta-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--success);
  box-shadow: 0 0 6px rgba(77, 133, 71, 0.5);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.meta-time {
  color: var(--text-faint);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  transition: all 0.25s;
  box-shadow: var(--shadow-sm);
}

.stat-card:hover {
  border-color: var(--border-strong);
  transform: translateY(-2px);
  box-shadow: var(--shadow);
}

.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-val {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-strong);
  line-height: 1.1;
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.stat-unit {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-muted);
}

.stat-lbl {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.stat-trend {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  flex-shrink: 0;
}

.stat-trend.up {
  background: var(--success-tint);
  color: var(--success);
}

.stat-trend.down {
  background: var(--danger-tint);
  color: var(--danger);
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.panel {
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 18px;
  margin-bottom: 16px;
  box-shadow: var(--shadow-sm);
  transition: border-color 0.25s;
}

.panel:hover {
  border-color: var(--border-strong);
}

.panel:last-child {
  margin-bottom: 0;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  position: relative;
  padding-left: 14px;
}

.panel-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  background: var(--primary-grad);
  border-radius: 2px;
}

.panel-legend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--text-muted);
}

.legend-dot {
  width: 8px;
  height: 3px;
  border-radius: 2px;
  margin-right: 2px;
}

.panel-link {
  font-size: 11px;
  color: var(--primary-text);
  cursor: pointer;
  transition: color 0.2s;
}

.panel-link:hover {
  color: var(--primary);
}

.env-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.env-item {
  background: var(--bg-soft);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 14px 10px;
  text-align: center;
  transition: all 0.25s;
}

.env-item:hover {
  border-color: var(--border-strong);
  background: var(--bg-hover);
}

.env-item.env-warn {
  border-color: rgba(194, 91, 91, 0.4);
  box-shadow: 0 0 0 1px rgba(194, 91, 91, 0.08) inset;
}

.env-val {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-strong);
  line-height: 1.2;
}

.env-unit {
  color: var(--text-faint);
  font-size: 11px;
  font-weight: 500;
}

.env-label {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 4px;
}

.env-status {
  font-size: 10px;
  margin-top: 4px;
  padding: 1px 8px;
  border-radius: 8px;
  display: inline-block;
}

.env-status.normal {
  background: var(--success-tint);
  color: var(--success);
}

.env-status.warn {
  background: var(--danger-tint);
  color: var(--danger);
}

.chart-container {
  width: 100%;
  height: 280px;
}

.chart-sm {
  height: 200px;
}

.chart-md {
  height: 240px;
}

.simple-table-wrap {
  max-height: 220px;
  overflow-y: auto;
}

.simple-table-wrap::-webkit-scrollbar {
  width: 4px;
}

.simple-table-wrap::-webkit-scrollbar-thumb {
  background: rgba(176, 123, 62, 0.2);
  border-radius: 2px;
}

.simple-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.simple-table th {
  background: var(--bg-muted);
  padding: 10px 12px;
  text-align: left;
  color: var(--text-secondary);
  font-weight: 600;
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
  position: sticky;
  top: 0;
}

.simple-table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--border);
  color: var(--text-main);
  white-space: nowrap;
}

.simple-table tbody tr {
  transition: background 0.15s;
}

.simple-table tbody tr:hover {
  background: var(--bg-hover);
}

.tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
}

.tag-green {
  background: var(--success-tint);
  color: var(--success);
}

.tag-red {
  background: var(--danger-tint);
  color: var(--danger);
}

.tag-orange {
  background: var(--warning-tint);
  color: var(--warning);
}

.tag-blue {
  background: var(--info-tint);
  color: var(--info);
}
.ai-assistant-entry {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 80;
}

.ai-assistant-fab {
  position: relative;
  display: flex;
  min-width: 52px;
  flex-direction: column;
  align-items: center;
  gap: 9px;
  padding: 14px 10px 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.32);
  border-radius: 17px;
  background: var(--primary-grad);
  box-shadow: 0 12px 28px rgba(120, 86, 43, 0.28);
  color: #fff;
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease;
}

.ai-assistant-fab:hover {
  box-shadow: 0 16px 32px rgba(120, 86, 43, 0.34);
  transform: translateY(-3px) scale(1.02);
}

.ai-fab-glow {
  position: absolute;
  top: -22px;
  right: -22px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  filter: blur(1px);
}

.ai-fab-icon {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.ai-fab-label {
  position: relative;
  color: #fff;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 2px;
  writing-mode: vertical-rl;
}

@media (max-width: 900px) {
  .ai-assistant-entry {
    right: 16px;
    bottom: 16px;
  }
}
</style>
