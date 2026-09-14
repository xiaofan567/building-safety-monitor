<template>
  <div class="env-monitor-page">
    <div class="breadcrumb">
      <span class="crumb-tag">施工环境</span>
      <span class="crumb-sep">/</span>
      <span class="crumb-current">环境实时监测</span>
    </div>

    <div class="page-header">
      <div>
        <h2>环境实时监测</h2>
        <div class="header-tip">施工现场环境参数实时监控与趋势分析</div>
      </div>
      <div class="refresh-tip" :class="{ error: !!loadError }">
        <span class="refresh-dot"></span>
        {{ loadError || '每10秒自动刷新' }}
      </div>
    </div>

    <div class="env-grid" v-loading="loading">
      <section class="env-card">
        <div class="card-header">
          <div class="card-title">
            <img :src="envTipsIcon" alt="" class="card-icon">
            <h3>24小时空气质量</h3>
          </div>
          <span class="card-unit">μg/m³</span>
        </div>
        <div ref="airQualityChartEl" class="chart chart-md"></div>
      </section>

      <section class="env-card">
        <div class="card-header">
          <div class="card-title">
            <img :src="envTipsIcon" alt="" class="card-icon">
            <h3>实时数据</h3>
          </div>
          <span class="card-unit">实时值</span>
        </div>
        <div class="realtime-grid">
          <div v-for="metric in realtimeMetrics" :key="metric.key" class="metric-card">
            <div class="metric-icon">
              <img :src="metric.icon" :alt="metric.label">
            </div>
            <div class="metric-content">
              <div class="metric-label">{{ metric.label }}</div>
              <div class="metric-value">
                {{ formatRealtimeValue(metric.key) }}
                <span v-if="hasRealtimeValue(metric.key) && metric.unit" class="metric-unit">{{ metric.unit }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="env-card">
        <div class="card-header">
          <div class="card-title">
            <img :src="envTipsIcon" alt="" class="card-icon">
            <h3>历史空气质量分析</h3>
          </div>
          <span class="card-unit">近7日</span>
        </div>
        <div ref="historyAirQualityChartEl" class="chart chart-md"></div>
      </section>

      <section class="env-card">
        <div class="card-header">
          <div class="card-title">
            <img :src="envTipsIcon" alt="" class="card-icon">
            <h3>噪声变化趋势</h3>
          </div>
          <span class="card-unit">dB</span>
        </div>
        <div ref="noiseChartEl" class="chart chart-sm"></div>
      </section>

      <section class="env-card">
        <div class="card-header">
          <div class="card-title">
            <img :src="envTipsIcon" alt="" class="card-icon">
            <h3>空气质量仪表盘</h3>
          </div>
          <span class="card-unit">μg/m³</span>
        </div>
        <div class="gauge-grid">
          <div ref="tspChartEl" class="gauge-chart"></div>
          <div ref="pm10ChartEl" class="gauge-chart"></div>
          <div ref="pm25ChartEl" class="gauge-chart"></div>
        </div>
      </section>

      <section class="env-card spray-card">
        <div class="card-header">
          <div class="card-title">
            <img :src="envTipsIcon" alt="" class="card-icon">
            <h3>喷淋启停记录</h3>
          </div>
          <span class="card-unit">最近7条</span>
        </div>
        <div v-if="sprayRecords.length" class="spray-list">
          <div v-for="record in sprayRecords" :key="record.id" class="spray-item">
            <div class="spray-main">
              <span class="spray-name">{{ record.assetName || record.pointName || '喷淋设备' }}</span>
              <span class="status-tag" :class="getOperationMeta(record.operationType).className">
                {{ getOperationMeta(record.operationType).text }}
              </span>
            </div>
            <div class="spray-meta">
              <span>{{ record.operationReason || '自动控制' }}</span>
              <time>{{ formatDateTime(record.operationTime) }}</time>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">暂无喷淋启停记录</div>
      </section>

      <section class="env-card">
        <div class="card-header">
          <div class="card-title">
            <img :src="envTipsIcon" alt="" class="card-icon">
            <h3>近一周温度</h3>
          </div>
          <span class="card-unit">℃</span>
        </div>
        <div ref="temperatureChartEl" class="chart chart-sm"></div>
      </section>

      <section class="env-card">
        <div class="card-header">
          <div class="card-title">
            <img :src="envTipsIcon" alt="" class="card-icon">
            <h3>近7日空气质量</h3>
          </div>
          <span class="card-unit">PM2.5 日均值</span>
        </div>
        <div ref="weeklyAirQualityChartEl" class="chart chart-sm"></div>
        <div class="quality-legend">
          <div v-for="level in airQualityLevels" :key="level.label" class="legend-item">
            <span class="legend-color" :style="{ background: level.color }"></span>
            <span>{{ level.label }}</span>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import {
  formatFixedAirQualityData,
  formatNoiseData,
  formatTemperatureData,
  formatWeeklyAirQualityData,
  selectEnvData
} from '@/api/environmentMnt'

import envTipsIcon from '@/assets/img/envtips.png'
import temperatureIcon from '@/assets/img/wendu.png'
import humidityIcon from '@/assets/img/shidu.png'
import windSpeedIcon from '@/assets/img/fengsu.png'
import noiseIcon from '@/assets/img/zaosheng.png'
import windDirectionIcon from '@/assets/img/fengxiang.png'
import airPressureIcon from '@/assets/img/qiya.png'

const REFRESH_INTERVAL = 10000

const airQualityLevels = [
  { label: '优', max: 35, color: '#30BF78' },
  { label: '良', max: 75, color: '#3397FE' },
  { label: '轻度污染', max: 115, color: '#FAAD14' },
  { label: '中度污染', max: 150, color: '#F4664A' },
  { label: '重度污染', max: 250, color: '#FB466B' },
  { label: '严重污染', max: Number.POSITIVE_INFINITY, color: '#8B0000' }
]

const realtimeMetrics = [
  { key: 'temp', label: '温度', icon: temperatureIcon, unit: '℃' },
  { key: 'humi', label: '湿度', icon: humidityIcon, unit: '%' },
  { key: 'windSpeed', label: '风速', icon: windSpeedIcon, unit: 'm/s' },
  { key: 'noise', label: '噪声', icon: noiseIcon, unit: 'dB' },
  { key: 'windDir', label: '风向', icon: windDirectionIcon, unit: '' },
  { key: 'airPres', label: '气压', icon: airPressureIcon, unit: 'hPa' }
]

const realTimeData = reactive({
  temp: '',
  humi: '',
  pm25: '',
  pm10: '',
  noise: '',
  airPres: '',
  tsp: '',
  windDir: '',
  windSpeed: ''
})

const airQualityChartEl = ref(null)
const noiseChartEl = ref(null)
const temperatureChartEl = ref(null)
const historyAirQualityChartEl = ref(null)
const tspChartEl = ref(null)
const pm10ChartEl = ref(null)
const pm25ChartEl = ref(null)
const weeklyAirQualityChartEl = ref(null)

const sprayRecords = ref([])
const loading = ref(false)
const loadError = ref('')
const activeCharts = []
let refreshTimer = null

let airQualityData = {
  hours: ['12时', '15时', '18时', '21时', '00时', '03时', '06时', '09时'],
  pm25: [0, 0, 0, 0, 0, 0, 0, 0],
  pm10: [0, 0, 0, 0, 0, 0, 0, 0]
}

let noiseData = {
  hours: ['12时', '15时', '18时', '21时', '00时', '03时', '06时', '09时'],
  noise: [0, 0, 0, 0, 0, 0, 0, 0]
}

let temperatureData = {
  days: [],
  maxTemp: [],
  minTemp: []
}

let historyAirQualityData = []

let weeklyAirQualityData = {
  days: [],
  aqi: []
}

const setRealTimeData = (list) => {
  Object.keys(realTimeData).forEach(key => {
    realTimeData[key] = ''
  })

  if (!Array.isArray(list)) return
  list.forEach(item => {
    const pointCode = item.monitorPointCode
    if (Object.prototype.hasOwnProperty.call(realTimeData, pointCode)) {
      realTimeData[pointCode] = item.value ?? ''
    }
  })
}

const loadEnvData = async (silent = false) => {
  if (!silent) loading.value = true

  try {
    const data = await selectEnvData()
    const [airQuality, noise, temperature, weeklyAirQuality] = await Promise.all([
      formatFixedAirQualityData(data.pm25List, data.pm10List),
      formatNoiseData(data.noiseList),
      formatTemperatureData(data.temperatureList),
      formatWeeklyAirQualityData(data.pm25AvgList)
    ])

    airQualityData = airQuality
    noiseData = noise
    temperatureData = temperature
    weeklyAirQualityData = weeklyAirQuality
    historyAirQualityData = Array.isArray(data.airQuality) ? data.airQuality : []
    sprayRecords.value = Array.isArray(data.spraySystem) ? data.spraySystem : []
    setRealTimeData(data.realTimeList)
    loadError.value = ''
  } catch (error) {
    loadError.value = error?.msg || '环境数据加载失败'
    console.error('环境数据加载失败:', error)
  } finally {
    if (!silent) loading.value = false
  }
}

const formatRealtimeValue = (key) => {
  const value = realTimeData[key]
  return value === '' || value === null || value === undefined ? '--' : value
}

const hasRealtimeValue = (key) => {
  const value = realTimeData[key]
  return value !== '' && value !== null && value !== undefined
}

const getOperationMeta = (operationType) => {
  switch (String(operationType)) {
    case '1':
      return { text: '启动', className: 'status-running' }
    case '0':
      return { text: '关闭', className: 'status-stopped' }
    default:
      return { text: '异常', className: 'status-error' }
  }
}

const formatDateTime = (value) => {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 16)
}

const getAirQualityMeta = (value) => {
  const numericValue = Number(value) || 0
  return airQualityLevels.find(level => numericValue <= level.max) || airQualityLevels.at(-1)
}

const axisStyle = {
  axisLine: { lineStyle: { color: '#ece3d3' } },
  axisLabel: { color: '#a29686', fontSize: 10 },
  axisTick: { show: false }
}

const valueAxisStyle = {
  ...axisStyle,
  splitLine: { lineStyle: { color: '#f1eadb', type: 'dashed' } }
}

const baseTooltip = {
  trigger: 'axis',
  backgroundColor: 'rgba(51, 46, 38, 0.92)',
  borderWidth: 0,
  textStyle: { color: '#ffffff', fontSize: 12 }
}

const createChart = (element, option) => {
  if (!element.value) return
  const chart = echarts.init(element.value)
  chart.setOption(option)
  activeCharts.push(chart)
}

const disposeCharts = () => {
  activeCharts.splice(0).forEach(chart => chart.dispose())
}

const resizeCharts = () => {
  activeCharts.forEach(chart => chart.resize())
}

const getAirQualityOption = () => ({
  tooltip: baseTooltip,
  legend: {
    data: ['PM2.5', 'PM10'],
    bottom: 0,
    left: 'center',
    itemWidth: 12,
    itemHeight: 8,
    textStyle: { color: '#7a6c55', fontSize: 11 }
  },
  grid: { left: 4, right: 12, top: 28, bottom: 34, containLabel: true },
  xAxis: {
    type: 'category',
    data: airQualityData.hours,
    boundaryGap: false,
    ...axisStyle
  },
  yAxis: {
    type: 'value',
    name: 'μg/m³',
    nameTextStyle: { color: '#a29686', fontSize: 10, padding: [0, 0, 0, 6] },
    ...valueAxisStyle
  },
  series: [
    {
      name: 'PM2.5',
      type: 'line',
      data: airQualityData.pm25,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#4d8547', width: 2 },
      itemStyle: { color: '#4d8547', borderColor: '#ffffff', borderWidth: 2 }
    },
    {
      name: 'PM10',
      type: 'line',
      data: airQualityData.pm10,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#5a8fd4', width: 2 },
      itemStyle: { color: '#5a8fd4', borderColor: '#ffffff', borderWidth: 2 }
    }
  ]
})

const getNoiseOption = () => ({
  tooltip: baseTooltip,
  grid: { left: 4, right: 12, top: 24, bottom: 24, containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: noiseData.hours,
    ...axisStyle
  },
  yAxis: {
    type: 'value',
    name: 'dB',
    nameTextStyle: { color: '#a29686', fontSize: 10, padding: [0, 0, 0, 6] },
    ...valueAxisStyle
  },
  series: [{
    name: '噪声',
    type: 'line',
    data: noiseData.noise,
    smooth: true,
    symbol: 'circle',
    symbolSize: 6,
    lineStyle: { color: '#5a8fd4', width: 2 },
    itemStyle: { color: '#5a8fd4', borderColor: '#ffffff', borderWidth: 2 },
    areaStyle: {
      color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: 'rgba(90, 143, 212, 0.30)' },
        { offset: 1, color: 'rgba(90, 143, 212, 0.04)' }
      ])
    }
  }]
})

const getTemperatureOption = () => ({
  tooltip: baseTooltip,
  legend: {
    data: ['最高气温', '最低气温'],
    bottom: 0,
    left: 'center',
    itemWidth: 12,
    itemHeight: 8,
    textStyle: { color: '#7a6c55', fontSize: 11 }
  },
  grid: { left: 4, right: 12, top: 24, bottom: 34, containLabel: true },
  xAxis: {
    type: 'category',
    data: temperatureData.days,
    ...axisStyle
  },
  yAxis: {
    type: 'value',
    name: '℃',
    nameTextStyle: { color: '#a29686', fontSize: 10, padding: [0, 0, 0, 6] },
    ...valueAxisStyle
  },
  series: [
    {
      name: '最高气温',
      type: 'line',
      data: temperatureData.maxTemp,
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { color: '#c98d2e', width: 2 },
      itemStyle: { color: '#c98d2e', borderColor: '#ffffff', borderWidth: 2 }
    },
    {
      name: '最低气温',
      type: 'line',
      data: temperatureData.minTemp,
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { color: '#5a8fd4', width: 2 },
      itemStyle: { color: '#5a8fd4', borderColor: '#ffffff', borderWidth: 2 }
    }
  ]
})

const getHistoryAirQualityOption = () => {
  const data = historyAirQualityData.map(item => ({
    name: item.level || item.name || '未知',
    value: item.count ?? 0,
    itemStyle: { color: airQualityLevels.find(level => level.label === (item.level || item.name))?.color || '#b8ac9c' }
  }))

  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(51, 46, 38, 0.92)',
      borderWidth: 0,
      textStyle: { color: '#ffffff', fontSize: 12 },
      formatter: '{b}<br/>{c} 次（{d}%）'
    },
    legend: {
      icon: 'circle',
      bottom: 0,
      left: 'center',
      itemWidth: 9,
      itemHeight: 9,
      itemGap: 12,
      textStyle: { color: '#7a6c55', fontSize: 10 }
    },
    title: data.length ? undefined : {
      text: '暂无数据',
      left: 'center',
      top: '40%',
      textStyle: { color: '#a29686', fontSize: 12, fontWeight: 400 }
    },
    series: [{
      name: '空气质量等级',
      type: 'pie',
      radius: ['42%', '62%'],
      center: ['50%', '42%'],
      avoidLabelOverlap: true,
      itemStyle: { borderWidth: 3, borderColor: '#ffffff' },
      label: { show: false },
      data
    }]
  }
}

const getGaugeOption = (name, value, max, unit) => ({
  series: [{
    type: 'gauge',
    radius: '96%',
    center: ['50%', '56%'],
    min: 0,
    max,
    startAngle: 210,
    endAngle: -30,
    axisLine: {
      lineStyle: {
        width: 7,
        color: [[0.2, '#30BF78'], [0.6, '#FAAD14'], [1, '#FF5353']]
      }
    },
    pointer: {
      width: 2,
      length: '55%',
      itemStyle: { color: 'auto' }
    },
    axisTick: { show: false },
    splitLine: { show: false },
    axisLabel: { color: '#a29686', distance: 14, fontSize: 7 },
    title: {
      show: true,
      offsetCenter: [0, '82%'],
      color: '#7a6c55',
      fontSize: 11
    },
    detail: {
      valueAnimation: true,
      offsetCenter: [0, '24%'],
      formatter: (currentValue) => `${Number(currentValue).toFixed(0)}\n${unit}`,
      color: '#332e26',
      fontSize: 11,
      lineHeight: 14
    },
    data: [{ value: Number(value) || 0, name }]
  }]
})

const getWeeklyAirQualityOption = () => ({
  tooltip: {
    ...baseTooltip,
    formatter: (params) => {
      const point = Array.isArray(params) ? params[0] : params
      const level = getAirQualityMeta(point.value)
      return `${point.name}<br/>PM2.5：${point.value}<br/>等级：<span style="color:${level.color}">${level.label}</span>`
    }
  },
  grid: { left: 4, right: 12, top: 24, bottom: 12, containLabel: true },
  xAxis: {
    type: 'category',
    data: weeklyAirQualityData.days,
    ...axisStyle
  },
  yAxis: {
    type: 'value',
    name: 'μg/m³',
    nameTextStyle: { color: '#a29686', fontSize: 10, padding: [0, 0, 0, 6] },
    ...valueAxisStyle
  },
  series: [{
    name: 'PM2.5 日均值',
    type: 'bar',
    barWidth: '42%',
    data: weeklyAirQualityData.aqi.map(value => ({
      value,
      itemStyle: { color: getAirQualityMeta(value).color }
    })),
    itemStyle: { borderRadius: [3, 3, 0, 0] },
    emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(51, 46, 38, 0.18)' } }
  }]
})

const renderCharts = () => {
  disposeCharts()
  createChart(airQualityChartEl, getAirQualityOption())
  createChart(noiseChartEl, getNoiseOption())
  createChart(temperatureChartEl, getTemperatureOption())
  createChart(historyAirQualityChartEl, getHistoryAirQualityOption())
  createChart(tspChartEl, getGaugeOption('TSP', realTimeData.tsp, 500, 'μg/m³'))
  createChart(pm10ChartEl, getGaugeOption('PM10', realTimeData.pm10, 200, 'μg/m³'))
  createChart(pm25ChartEl, getGaugeOption('PM2.5', realTimeData.pm25, 200, 'μg/m³'))
  createChart(weeklyAirQualityChartEl, getWeeklyAirQualityOption())
}

const refreshPage = async (silent = false) => {
  await loadEnvData(silent)
  renderCharts()
}

onMounted(async () => {
  window.addEventListener('resize', resizeCharts, { passive: true })
  await refreshPage()
  refreshTimer = window.setInterval(() => refreshPage(true), REFRESH_INTERVAL)
})

onUnmounted(() => {
  if (refreshTimer) window.clearInterval(refreshTimer)
  refreshTimer = null
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
})
</script>

<style scoped>
.env-monitor-page {
  height: 100%;
  overflow-y: auto;
  padding: 20px;
  box-sizing: border-box;
  background: var(--bg-page);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  color: var(--text-muted);
  font-size: 12px;
}
.crumb-tag { color: var(--text-secondary); }
.crumb-sep { color: var(--text-faint); }
.crumb-current { color: var(--primary-text); font-weight: 500; }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0;
  color: var(--text-strong);
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 1px;
}
.header-tip {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 12px;
}
.refresh-tip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--bg-soft);
  color: var(--text-secondary);
  font-size: 12px;
  white-space: nowrap;
}
.refresh-tip.error { color: var(--danger); border-color: rgba(194, 91, 91, 0.28); }
.refresh-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--success);
  box-shadow: 0 0 0 3px var(--success-tint);
}
.refresh-tip.error .refresh-dot { background: var(--danger); box-shadow: 0 0 0 3px var(--danger-tint); }

.env-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  align-items: stretch;
}

.env-card {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-white);
  box-shadow: var(--shadow-sm);
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.card-title h3 {
  overflow: hidden;
  margin: 0;
  color: var(--text-strong);
  font-size: 14px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  object-fit: contain;
}
.card-unit {
  flex-shrink: 0;
  color: var(--text-muted);
  font-size: 11px;
}

.chart { width: 100%; }
.chart-md { height: 230px; }
.chart-sm { height: 190px; }

.realtime-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}
.metric-card {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-soft);
  transition: border-color 0.2s, transform 0.2s, box-shadow 0.2s;
}
.metric-card:hover {
  transform: translateY(-1px);
  border-color: var(--border-strong);
  box-shadow: 0 5px 14px rgba(120, 95, 60, 0.09);
}
.metric-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 10px;
  background: var(--bg-white);
  box-shadow: 0 2px 8px rgba(120, 95, 60, 0.08);
}
.metric-icon img { width: 100%; height: 100%; object-fit: contain; }
.metric-content { min-width: 0; }
.metric-label {
  color: var(--text-muted);
  font-size: 11px;
}
.metric-value {
  overflow: hidden;
  color: var(--text-strong);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.metric-unit {
  margin-left: 3px;
  color: var(--text-muted);
  font-size: 10px;
  font-weight: 500;
}

.gauge-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}
.gauge-chart {
  width: 100%;
  height: 178px;
}

.spray-card {
  grid-row: span 2;
  min-height: 0;
}
.spray-list {
  max-height: 500px;
  padding-right: 4px;
  overflow-y: auto;
}
.spray-item {
  padding: 12px 0;
  border-bottom: 1px solid var(--border);
}
.spray-item:last-child { border-bottom: 0; }
.spray-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.spray-name {
  overflow: hidden;
  color: var(--text-main);
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.status-tag {
  flex-shrink: 0;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
}
.status-running { background: var(--success-tint); color: var(--success); }
.status-stopped { background: var(--danger-tint); color: var(--danger); }
.status-error { background: var(--warning-tint); color: var(--warning); }
.spray-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 7px;
  color: var(--text-muted);
  font-size: 11px;
}
.spray-meta span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.spray-meta time { flex-shrink: 0; }
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  color: var(--text-muted);
  font-size: 13px;
}

.quality-legend {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 6px;
  margin-top: 8px;
}
.legend-item {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: var(--text-secondary);
  font-size: 10px;
  text-align: center;
}
.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 3px;
}

@media (max-width: 1180px) {
  .env-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .spray-card { grid-row: auto; }
}

@media (max-width: 760px) {
  .env-monitor-page { padding: 14px; }
  .page-header { align-items: flex-start; flex-direction: column; }
  .env-grid { grid-template-columns: 1fr; }
  .chart-md { height: 210px; }
  .gauge-chart { height: 150px; }
  .quality-legend { flex-wrap: wrap; }
  .legend-item { min-width: 56px; }
}
</style>