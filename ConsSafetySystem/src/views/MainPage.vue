<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getDashboardStats, getEnvironmentRealtime, getSafetyHazards, getSprayRecords, getAlarmList, getDeviceOverview, getDeviceStatistics } from '@/api/dashboard'

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
  gridLine: 'rgba(26,50,72,0.4)',
  textDim: '#5a6a7a',
  textNormal: '#8899aa'
}

function initEnvChart() {
  if (!envChartRef.value) return
  if (envChartInstance) envChartInstance.dispose()
  envChartInstance = echarts.init(envChartRef.value)

  const hours = ['00:00','02:00','04:00','06:00','08:00','10:00','12:00','14:00','16:00','18:00','20:00','22:00']
  const tempData = [18, 17, 16, 15, 16, 19, 22, 25, 26, 24, 21, 19]
  const humData = [65, 68, 72, 75, 72, 62, 55, 50, 48, 52, 58, 62]

  envChartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15,29,46,0.95)',
      borderColor: 'rgba(26,115,232,0.3)',
      textStyle: { color: '#e8edf2', fontSize: 12 }
    },
    legend: {
      bottom: 0,
      textStyle: { color: '#8899aa', fontSize: 11 },
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

  const categories = ['高处坠落','物体打击','机械伤害','坍塌','触电','火灾']
  const values = [23, 18, 15, 10, 8, 6]

  hazardChartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15,29,46,0.95)',
      borderColor: 'rgba(26,115,232,0.3)',
      textStyle: { color: '#e8edf2', fontSize: 12 }
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

  const days = ['周一','周二','周三','周四','周五','周六','周日']
  const sprayData = [120, 145, 98, 132, 155, 80, 60]

  sprinklerChartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15,29,46,0.95)',
      borderColor: 'rgba(26,115,232,0.3)',
      textStyle: { color: '#e8edf2', fontSize: 12 }
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
      name: '喷淋量(L)',
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

  const data = [
    { value: 20, name: '塔吊', itemStyle: { color: '#1a73e8' } },
    { value: 12, name: '升降机', itemStyle: { color: '#0d9e6c' } },
    { value: 18, name: '传感器', itemStyle: { color: '#f5a623' } },
    { value: 8, name: '喷淋设备', itemStyle: { color: '#0ea5e9' } },
    { value: 6, name: '摄像头', itemStyle: { color: '#8b5cf6' } }
  ]

  deviceOverviewChartInstance.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15,29,46,0.95)',
      borderColor: 'rgba(26,115,232,0.3)',
      textStyle: { color: '#e8edf2', fontSize: 12 },
      formatter: '{b}: {c} 台 ({d}%)'
    },
    legend: {
      bottom: 0,
      textStyle: { color: '#8899aa', fontSize: 10 },
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
        borderColor: '#0f1d2e',
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
          color: '#e8edf2'
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

  const types = ['塔吊', '升降机', '传感器', '喷淋', '摄像头']
  const onlineData = [18, 10, 16, 7, 5]
  const offlineData = [2, 2, 2, 1, 1]

  deviceStatsChartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15,29,46,0.95)',
      borderColor: 'rgba(26,115,232,0.3)',
      textStyle: { color: '#e8edf2', fontSize: 12 }
    },
    legend: {
      bottom: 0,
      textStyle: { color: '#8899aa', fontSize: 11 },
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

async function fetchData() {
  try { const res = await getDashboardStats(); if (res?.data) stats.value = res.data } catch(e) {}
  try { const res = await getEnvironmentRealtime(); if (res?.data) envData.value = res.data } catch(e) {}
  try { const res = await getSafetyHazards(); if (res?.data) hazardChartData.value = res.data } catch(e) {}
  try { const res = await getSprayRecords(); if (res?.data) sprinklerRecords.value = res.data } catch(e) {}
  try { const res = await getAlarmList(); if (res?.data) alarmList.value = res.data } catch(e) {}
  try { const res = await getDeviceOverview(); if (res?.data) deviceOverview.value = res.data } catch(e) {}
  try { const res = await getDeviceStatistics(); if (res?.data) deviceStatistics.value = res.data } catch(e) {}
}

onMounted(() => {
  fetchData()
  setTimeout(() => {
    initEnvChart()
    initHazardChart()
    initSprinklerChart()
    initDeviceOverviewChart()
    initDeviceStatsChart()
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
            <div class="env-item">
              <div class="env-val" style="color:#f5a623">26<span class="env-unit">°C</span></div>
              <div class="env-label">温度</div>
              <div class="env-status normal">正常</div>
            </div>
            <div class="env-item">
              <div class="env-val" style="color:#1a73e8">55<span class="env-unit">%</span></div>
              <div class="env-label">湿度</div>
              <div class="env-status normal">正常</div>
            </div>
            <div class="env-item">
              <div class="env-val" style="color:#0ea5e9">3.2<span class="env-unit">m/s</span></div>
              <div class="env-label">风速</div>
              <div class="env-status normal">正常</div>
            </div>
            <div class="env-item env-warn">
              <div class="env-val" style="color:#e54545">85<span class="env-unit">dB</span></div>
              <div class="env-label">噪音</div>
              <div class="env-status warn">超标</div>
            </div>
            <div class="env-item">
              <div class="env-val" style="color:#8b5cf6">42<span class="env-unit">μg/m³</span></div>
              <div class="env-label">PM2.5</div>
              <div class="env-status normal">正常</div>
            </div>
            <div class="env-item">
              <div class="env-val" style="color:#0d9e6c">0.8<span class="env-unit">mg/m³</span></div>
              <div class="env-label">粉尘</div>
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
                  <td>{{ item.alertTime || item.createTime || '--' }}</td>
                  <td>{{ item.alertType || '--' }}</td>
                  <td>{{ item.alertContent || item.content || '--' }}</td>
                  <td>
                    <span class="tag" :class="item.alertStatus === '1' || item.handleState === '0' ? 'tag-red' : 'tag-green'">
                      {{ item.alertStatus === '1' || item.handleState === '0' ? '未处理' : '已处理' }}
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
                  <th>喷淋量(L)</th>
                  <th>时间</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="i in 5" :key="i">
                  <td>A区-主体施工区</td>
                  <td>SP-00{{ i }}</td>
                  <td>{{ 150 + i * 20 }}</td>
                  <td>{{ new Date().toLocaleTimeString('zh-CN') }}</td>
                  <td><span class="tag tag-green">正常</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="panel">
          <div class="panel-header">
            <div class="panel-title">周喷淋量统计</div>
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
  </div>
</template>

<style scoped>
.dashboard {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.dashboard::-webkit-scrollbar {
  width: 4px;
}

.dashboard::-webkit-scrollbar-thumb {
  background: rgba(26, 115, 232, 0.2);
  border-radius: 2px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #e8edf2;
  letter-spacing: 1px;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #5a6a7a;
}

.meta-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #0d9e6c;
  box-shadow: 0 0 6px rgba(13, 158, 108, 0.5);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.meta-time {
  color: #3a5068;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: linear-gradient(135deg, #0f1d2e 0%, #0a1625 100%);
  border: 1px solid rgba(26, 50, 72, 0.5);
  border-radius: 10px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  transition: all 0.25s;
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(26, 115, 232, 0.3), transparent);
  opacity: 0;
  transition: opacity 0.3s;
}

.stat-card:hover {
  border-color: rgba(26, 115, 232, 0.3);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}

.stat-card:hover::before {
  opacity: 1;
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
  color: #e8edf2;
  line-height: 1.1;
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.stat-unit {
  font-size: 14px;
  font-weight: 500;
  color: #5a6a7a;
}

.stat-lbl {
  font-size: 12px;
  color: #5a6a7a;
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
  background: rgba(13, 158, 108, 0.12);
  color: #0d9e6c;
}

.stat-trend.down {
  background: rgba(229, 69, 69, 0.12);
  color: #e54545;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.panel {
  background: #0f1d2e;
  border: 1px solid rgba(26, 50, 72, 0.5);
  border-radius: 10px;
  padding: 18px;
  margin-bottom: 16px;
  transition: border-color 0.25s;
}

.panel:hover {
  border-color: rgba(26, 115, 232, 0.2);
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
  color: #e8edf2;
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
  background: linear-gradient(180deg, #1a73e8, #0d9e6c);
  border-radius: 2px;
}

.panel-legend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #5a6a7a;
}

.legend-dot {
  width: 8px;
  height: 3px;
  border-radius: 2px;
  margin-right: 2px;
}

.panel-link {
  font-size: 11px;
  color: #5a8fd4;
  cursor: pointer;
  transition: color 0.2s;
}

.panel-link:hover {
  color: #1a73e8;
}

.env-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.env-item {
  background: #0a1625;
  border: 1px solid rgba(26, 50, 72, 0.4);
  border-radius: 8px;
  padding: 14px 10px;
  text-align: center;
  transition: all 0.25s;
}

.env-item:hover {
  border-color: rgba(26, 115, 232, 0.3);
  background: #0d1b2a;
}

.env-item.env-warn {
  border-color: rgba(229, 69, 69, 0.3);
  box-shadow: 0 0 0 1px rgba(229, 69, 69, 0.08) inset;
}

.env-val {
  font-size: 24px;
  font-weight: 700;
  color: #e8edf2;
  line-height: 1.2;
}

.env-unit {
  color: #3a5068;
  font-size: 11px;
  font-weight: 500;
}

.env-label {
  font-size: 11px;
  color: #5a6a7a;
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
  background: rgba(13, 158, 108, 0.1);
  color: #0d9e6c;
}

.env-status.warn {
  background: rgba(229, 69, 69, 0.1);
  color: #e54545;
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
  width: 3px;
}

.simple-table-wrap::-webkit-scrollbar-thumb {
  background: rgba(26, 115, 232, 0.15);
  border-radius: 2px;
}

.simple-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.simple-table th {
  background: #0a1625;
  padding: 10px 12px;
  text-align: left;
  color: #5a6a7a;
  font-weight: 500;
  border-bottom: 1px solid rgba(26, 50, 72, 0.5);
  white-space: nowrap;
  position: sticky;
  top: 0;
}

.simple-table td {
  padding: 10px 12px;
  border-bottom: 1px solid rgba(26, 50, 72, 0.25);
  color: #bcc8d4;
  white-space: nowrap;
}

.simple-table tbody tr {
  transition: background 0.15s;
}

.simple-table tbody tr:hover {
  background: rgba(26, 115, 232, 0.04);
}

.tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
}

.tag-green {
  background: rgba(13, 158, 108, 0.12);
  color: #0d9e6c;
}

.tag-red {
  background: rgba(229, 69, 69, 0.12);
  color: #e54545;
}

.tag-orange {
  background: rgba(245, 166, 35, 0.12);
  color: #f5a623;
}

.tag-blue {
  background: rgba(26, 115, 232, 0.12);
  color: #1a73e8;
}
</style>